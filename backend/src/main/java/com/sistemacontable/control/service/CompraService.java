package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.ProveedorDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.CompraCmd;
import com.sistemacontable.control.service.cmd.LineaCompraCmd;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de compras con contabilizacion automatica. Es una sola transaccion JTA (el contenedor revierte ante
 * cualquier {@code ReglaNegocioException}/{@code NoEncontradoException} o error de infraestructura).
 *
 * <p>Orden: primero se valida y se calcula todo SIN escribir (proveedor y empleado existen; al menos una linea;
 * cantidad &gt; 0 y precio &gt;= 0; lote existente; almacen existente y habilitado; cuentas configuradas existentes,
 * activas e imputables; asiento cuadrado). Despues se escribe: compra, y por cada linea su {@code compra_detalle},
 * el aumento de existencias, y un {@code kardex} {@code Entrada} (destino = almacen, id_compra) con su
 * {@code kardex_detalle} (precio_costo = precio de compra); al final la partida {@code Diario}: Inventario e IVA
 * credito fiscal al debe, Proveedores (total) al haber. El IVA es {@code iva.tasa} sobre el subtotal. Una compra de
 * importe total cero actualiza stock y kardex pero no genera partida (no hay nada que contabilizar).</p>
 */
@Stateless
@LocalBean
public class CompraService {

    @Inject
    private ProveedorDAO proveedorDAO;
    @Inject
    private EmpleadoDAO empleadoDAO;
    @Inject
    private LoteDAO loteDAO;
    @Inject
    private AlmacenDAO almacenDAO;
    @Inject
    private CompraDAO compraDAO;
    @Inject
    private CompraDetalleDAO compraDetalleDAO;
    @Inject
    private ExistenciasService existenciasService;
    @Inject
    private KardexService kardexService;
    @Inject
    private PartidaService partidaService;
    @Inject
    private ConfigContable config;

    public Compra buscarPorId(Long id) {
        return compraDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Compra", id));
    }

    /** Lineas de una compra existente. */
    public List<CompraDetalle> detalles(Long compraId) {
        buscarPorId(compraId);
        return compraDetalleDAO.buscarPorCompra(compraId);
    }

    public List<Compra> listar(int first, int max) {
        return compraDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return compraDAO.count();
    }

    public ResultadoCompra registrar(CompraCmd cmd) {
        // ---- Fase 1: validar y calcular sin escribir nada
        if (cmd == null) {
            throw new ReglaNegocioException("La compra es obligatoria");
        }
        if (cmd.proveedorId() == null) {
            throw new ReglaNegocioException("El proveedor de la compra es obligatorio");
        }
        if (cmd.empleadoId() == null) {
            throw new ReglaNegocioException("El empleado de la compra es obligatorio");
        }
        if (cmd.lineas().isEmpty()) {
            throw new ReglaNegocioException("La compra debe tener al menos una linea");
        }
        Proveedor proveedor = proveedorDAO.buscarPorId(cmd.proveedorId())
                .orElseThrow(() -> new NoEncontradoException("Proveedor", cmd.proveedorId()));
        Empleado empleado = empleadoDAO.buscarPorId(cmd.empleadoId())
                .orElseThrow(() -> new NoEncontradoException("Empleado", cmd.empleadoId()));

        List<LineaResuelta> lineas = new ArrayList<>();
        BigDecimal subtotalSinRedondear = BigDecimal.ZERO;
        int numero = 0;
        for (LineaCompraCmd l : cmd.lineas()) {
            numero++;
            if (l == null) {
                throw new ReglaNegocioException("La linea " + numero + " de la compra es nula");
            }
            LineaResuelta resuelta = ResolucionLinea.resolver(loteDAO, almacenDAO, l.loteId(), l.almacenId(),
                    l.cantidad(), l.precio(), numero);
            lineas.add(resuelta);
            subtotalSinRedondear = subtotalSinRedondear.add(resuelta.importe());
        }
        TotalesDocumento totales = TotalesDocumento.calcular(subtotalSinRedondear,
                config.getBigDecimal(ConfigContable.IVA_TASA));
        Cuenta inventario = partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO);
        Cuenta ivaCredito = partidaService.cuentaConfigurada(ConfigContable.CUENTA_IVA_CREDITO);
        Cuenta proveedores = partidaService.cuentaConfigurada(ConfigContable.CUENTA_PROVEEDORES);
        List<LineaAsiento> asiento = Asientos.compra(inventario, ivaCredito, proveedores, totales);
        if (!asiento.isEmpty()) {
            ReglasPartida.validar(asiento);
        }

        // ---- Fase 2: escribir (todo dentro de la misma transaccion)
        LocalDate fecha = cmd.fecha() != null ? cmd.fecha() : LocalDate.now();
        Compra compra = new Compra();
        compra.setFecha(fecha);
        compra.setProveedor(proveedor);
        compra.setEmpleado(empleado);
        compra.setDescripcion(cmd.descripcion());
        compraDAO.crear(compra);
        compraDAO.flush();
        String referencia = "Compra #" + compra.getId();

        List<CompraDetalle> detalles = new ArrayList<>();
        List<MovimientoKardex> movimientos = new ArrayList<>();
        for (LineaResuelta l : lineas) {
            CompraDetalle detalle = new CompraDetalle();
            detalle.setCompra(compra);
            detalle.setLote(l.lote());
            detalle.setAlmacen(l.almacen());
            detalle.setCantidad(l.cantidad());
            detalle.setPrecio(l.precio());
            compraDetalleDAO.crear(detalle);
            detalles.add(detalle);

            existenciasService.aumentar(l.lote(), l.almacen(), l.cantidad());
            movimientos.add(kardexService.registrar(TipoMovimiento.Entrada, null, l.almacen(), compra, null,
                    referencia, List.of(new DetalleKardex(l.lote(), l.cantidad(), l.precio()))));
        }

        Partida partida = asiento.isEmpty() ? null
                : partidaService.registrar(TipoPartida.Diario, fecha, referencia, compra, null, null, asiento);
        return new ResultadoCompra(compra, detalles, movimientos, partida, totales.subtotal(), totales.iva(),
                totales.total());
    }
}
