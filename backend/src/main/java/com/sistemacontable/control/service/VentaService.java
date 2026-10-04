package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.ClienteDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.VentaDetalleDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaVentaCmd;
import com.sistemacontable.control.service.cmd.VentaCmd;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro de ventas con contabilizacion automatica en una sola transaccion JTA.
 *
 * <p>Orden: primero se valida y se calcula todo SIN escribir (cliente y empleado existen; al menos una linea;
 * cantidad &gt; 0 y precio &gt;= 0; lote existente; almacen existente y habilitado; <b>stock suficiente para todas
 * las lineas acumuladas por lote y almacen</b> (se bloquean las filas con FOR UPDATE); costo promedio ponderado
 * conocido para cada lote; cuentas configuradas validas; asiento cuadrado). Si algo falla no se ha escrito nada.
 * Despues se escribe: venta, y por cada linea su {@code ventas_detalle}, la resta de existencias y un
 * {@code kardex} {@code Salida} (origen = almacen, id_venta) cuyo {@code kardex_detalle} lleva como
 * {@code precio_costo} el costo promedio ponderado del lote (4 decimales); al final la partida {@code Ingreso}:
 * la cuenta de cobro (Caja si {@code venta.contado}, Clientes si no) al debe por el total; Ventas (subtotal) e IVA
 * debito fiscal al haber; y Costo de ventas al debe contra Inventario al haber.</p>
 *
 * <p>Decisiones conservadoras: un lote sin compras registradas no tiene costo conocido y la venta se rechaza (en vez
 * de contabilizar costo cero); el almacen de la linea debe estar habilitado. El precio de la linea no incluye IVA.
 * El costo de ventas es {@code SUM(cantidad x costo promedio)} redondeado una sola vez a 2 decimales.</p>
 */
@Stateless
@LocalBean
public class VentaService {

    @Inject
    private ClienteDAO clienteDAO;
    @Inject
    private EmpleadoDAO empleadoDAO;
    @Inject
    private LoteDAO loteDAO;
    @Inject
    private AlmacenDAO almacenDAO;
    @Inject
    private VentaDAO ventaDAO;
    @Inject
    private VentaDetalleDAO ventaDetalleDAO;
    @Inject
    private ExistenciasService existenciasService;
    @Inject
    private KardexService kardexService;
    @Inject
    private PartidaService partidaService;
    @Inject
    private ConfigContable config;

    public Venta buscarPorId(Long id) {
        return ventaDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Venta", id));
    }

    /** Lineas de una venta existente. */
    public List<VentaDetalle> detalles(Long ventaId) {
        buscarPorId(ventaId);
        return ventaDetalleDAO.buscarPorVenta(ventaId);
    }

    public List<Venta> listar(int first, int max) {
        return ventaDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return ventaDAO.count();
    }

    public ResultadoVenta registrar(VentaCmd cmd) {
        // ---- Fase 1: validar y calcular sin escribir nada
        if (cmd == null) {
            throw new ReglaNegocioException("La venta es obligatoria");
        }
        if (cmd.clienteId() == null) {
            throw new ReglaNegocioException("El cliente de la venta es obligatorio");
        }
        if (cmd.empleadoId() == null) {
            throw new ReglaNegocioException("El empleado de la venta es obligatorio");
        }
        if (cmd.lineas().isEmpty()) {
            throw new ReglaNegocioException("La venta debe tener al menos una linea");
        }
        Cliente cliente = clienteDAO.buscarPorId(cmd.clienteId())
                .orElseThrow(() -> new NoEncontradoException("Cliente", cmd.clienteId()));
        Empleado empleado = empleadoDAO.buscarPorId(cmd.empleadoId())
                .orElseThrow(() -> new NoEncontradoException("Empleado", cmd.empleadoId()));

        List<LineaResuelta> lineas = new ArrayList<>();
        Map<ClaveStock, BigDecimal> requeridos = new LinkedHashMap<>();
        BigDecimal subtotalSinRedondear = BigDecimal.ZERO;
        int numero = 0;
        for (LineaVentaCmd l : cmd.lineas()) {
            numero++;
            if (l == null) {
                throw new ReglaNegocioException("La linea " + numero + " de la venta es nula");
            }
            LineaResuelta resuelta = ResolucionLinea.resolver(loteDAO, almacenDAO, l.loteId(), l.almacenId(),
                    l.cantidad(), l.precio(), numero);
            lineas.add(resuelta);
            subtotalSinRedondear = subtotalSinRedondear.add(resuelta.importe());
            requeridos.merge(new ClaveStock(resuelta.lote().getId(), resuelta.almacen().getId()),
                    resuelta.cantidad(), BigDecimal::add);
        }
        existenciasService.verificarDisponible(requeridos);

        Map<Long, BigDecimal> costoPorLote = new HashMap<>();
        BigDecimal costoSinRedondear = BigDecimal.ZERO;
        for (LineaResuelta l : lineas) {
            Long loteId = l.lote().getId();
            BigDecimal costo = costoPorLote.get(loteId);
            if (costo == null) {
                costo = existenciasService.costoPromedio(loteId).orElseThrow(() -> new ReglaNegocioException(
                        "No se puede determinar el costo del lote " + loteId + ": no tiene compras registradas"));
                costoPorLote.put(loteId, costo);
            }
            costoSinRedondear = costoSinRedondear.add(l.cantidad().multiply(costo));
        }
        BigDecimal costoVentas = Dinero.redondear(costoSinRedondear);

        TotalesDocumento totales = TotalesDocumento.calcular(subtotalSinRedondear,
                config.getBigDecimal(ConfigContable.IVA_TASA));
        Cuenta cobro = partidaService.cuentaConfigurada(config.getBoolean(ConfigContable.VENTA_CONTADO)
                ? ConfigContable.CUENTA_CAJA : ConfigContable.CUENTA_CLIENTES);
        Cuenta ventas = partidaService.cuentaConfigurada(ConfigContable.CUENTA_VENTAS);
        Cuenta ivaDebito = partidaService.cuentaConfigurada(ConfigContable.CUENTA_IVA_DEBITO);
        Cuenta costoVentasCuenta = partidaService.cuentaConfigurada(ConfigContable.CUENTA_COSTO_VENTAS);
        Cuenta inventario = partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO);
        List<LineaAsiento> asiento = Asientos.venta(cobro, ventas, ivaDebito, costoVentasCuenta, inventario, totales,
                costoVentas);
        if (!asiento.isEmpty()) {
            ReglasPartida.validar(asiento);
        }

        // ---- Fase 2: escribir (todo dentro de la misma transaccion)
        LocalDate fecha = cmd.fecha() != null ? cmd.fecha() : LocalDate.now();
        Venta venta = new Venta();
        venta.setFecha(fecha);
        venta.setCliente(cliente);
        venta.setEmpleado(empleado);
        venta.setDescripcion(cmd.descripcion());
        ventaDAO.crear(venta);
        ventaDAO.flush();
        String referencia = "Venta #" + venta.getId();

        List<VentaDetalle> detalles = new ArrayList<>();
        List<MovimientoKardex> movimientos = new ArrayList<>();
        for (LineaResuelta l : lineas) {
            VentaDetalle detalle = new VentaDetalle();
            detalle.setVenta(venta);
            detalle.setLote(l.lote());
            detalle.setAlmacen(l.almacen());
            detalle.setCantidad(l.cantidad());
            detalle.setPrecio(l.precio());
            ventaDetalleDAO.crear(detalle);
            detalles.add(detalle);

            existenciasService.disminuir(l.lote(), l.almacen(), l.cantidad());
            movimientos.add(kardexService.registrar(TipoMovimiento.Salida, l.almacen(), null, null, venta,
                    referencia, List.of(new DetalleKardex(l.lote(), l.cantidad(), costoPorLote.get(l.lote().getId())))));
        }

        Partida partida = asiento.isEmpty() ? null
                : partidaService.registrar(TipoPartida.Ingreso, fecha, referencia, null, venta, null, asiento);
        return new ResultadoVenta(venta, detalles, movimientos, partida, totales.subtotal(), totales.iva(),
                totales.total(), costoVentas);
    }
}
