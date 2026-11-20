package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.ProveedorDAO;
import com.sistemacontable.control.ProductoDAO;
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
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.Producto;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.enums.FormaPago;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import com.sistemacontable.security.UsuarioActual;

/**
 * Registro de compras con contabilizacion automatica. Es una sola transaccion JTA (el contenedor revierte ante
 * cualquier {@code ReglaNegocioException}/{@code NoEncontradoException} o error de infraestructura).
 *
 * <p>Orden: primero se valida y se calcula todo SIN escribir (proveedor y empleado existen; al menos una linea;
 * cantidad &gt; 0 y precio &gt;= 0; lote existente; almacen existente y habilitado; cuentas configuradas existentes,
 * activas e imputables; asiento cuadrado). Despues se escribe: compra, y por cada linea su {@code compra_detalle},
 * el aumento de existencias, y un {@code kardex} {@code Entrada} (destino = almacen, id_compra) con su
 * {@code kardex_detalle} (precio_costo = precio de compra); al final la partida {@code Diario}: Inventario e IVA
 * credito fiscal al debe; al haber, Caja ({@code cuenta.caja}) por la parte de contado y Proveedores por la parte a
 * credito. El IVA es {@code iva.tasa} sobre el subtotal. Una compra de importe total cero actualiza stock y kardex
 * pero no genera partida (no hay nada que contabilizar).</p>
 *
 * <p>La forma de pago queda persistida; los gastos de adquisición se distribuyen entre líneas y los documentos al
 * crédito guardan una instantánea de tasa, plazo y vencimiento.</p>
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
    private PartidaService partidaService;
    @Inject private ParametroService parametros;
    @Inject private ProductoDAO productoDAO;
    @Inject private UsuarioActual usuarioActual;
    @Inject private KardexDAO kardexDAO;
    @Inject private KardexService kardexService;
    @Inject private PartidaDAO partidaDAO;
    @Inject private PartidaDetalleDAO partidaDetalleDAO;

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
    public List<Compra> buscar(LocalDate desde, LocalDate hasta, Long proveedorId, FormaPago formaPago, int first, int max) { return compraDAO.buscar(desde,hasta,proveedorId,formaPago,first,max); }
    public long contar(LocalDate desde, LocalDate hasta, Long proveedorId, FormaPago formaPago) { return compraDAO.contarBuscar(desde,hasta,proveedorId,formaPago); }

    public List<Kardex> kardex(Long compraId) { return kardexDAO.findByCompra(compraId); }
    public List<com.sistemacontable.entity.KardexDetalle> kardexDetalles(Long id) { return kardexService.detalles(id); }
    public Partida partida(Long compraId) { return partidaDAO.findByCompra(compraId).orElse(null); }
    public List<PartidaDetalle> partidaLineas(Long id) { return partidaDetalleDAO.buscarPorPartida(id); }

    public ResultadoCompra registrar(CompraCmd cmd) {
        if (cmd == null) throw new ReglaNegocioException("La compra es obligatoria");
        if (cmd.proveedorId() == null) throw new ReglaNegocioException("El proveedor de la compra es obligatorio");
        Long empleadoId = cmd.empleadoId() != null ? cmd.empleadoId() : usuarioActual == null ? null : usuarioActual.getIdEmpleado();
        if (empleadoId == null) throw new ReglaNegocioException("Debe indicar empleadoId o contar con un empleado en el perfil autenticado");
        if (cmd.formaPago() == null) throw new ReglaNegocioException("La forma de pago es obligatoria");
        if (cmd.formaPago() == FormaPago.Cheque && (cmd.numeroCheque() == null || cmd.numeroCheque().isBlank())) throw new ReglaNegocioException("numeroCheque es obligatorio para pago con cheque");
        if (cmd.lineas().isEmpty()) throw new ReglaNegocioException("La compra debe tener al menos una linea");
        BigDecimal descuento = Dinero.redondear(cmd.descuento() == null ? BigDecimal.ZERO : cmd.descuento());
        BigDecimal gastos = Dinero.redondear(cmd.gastosAdicionales() == null ? BigDecimal.ZERO : cmd.gastosAdicionales());
        if (descuento.signum() < 0 || gastos.signum() < 0) throw new ReglaNegocioException("Descuento y gastos adicionales deben ser positivos o cero");
        Proveedor proveedor = proveedorDAO.buscarPorId(cmd.proveedorId()).orElseThrow(() -> new NoEncontradoException("Proveedor", cmd.proveedorId()));
        Empleado empleado = empleadoDAO.buscarPorId(empleadoId).orElseThrow(() -> new NoEncontradoException("Empleado", empleadoId));
        List<LineaResuelta> lineas = new ArrayList<>();
        List<com.sistemacontable.control.service.cmd.NuevoLoteCmd> nuevos = new ArrayList<>();
        List<Almacen> almacenes = new ArrayList<>();
        List<Producto> productosNuevos = new ArrayList<>();
        BigDecimal subSinRedondear = BigDecimal.ZERO;
        int n = 0;
        for (LineaCompraCmd l : cmd.lineas()) {
            n++;
            if (l == null || (l.loteId() == null) == (l.nuevoLote() == null)) throw new ReglaNegocioException("La linea " + n + " debe indicar loteId o nuevoLote, pero no ambos");
            if (l.cantidad() == null || l.cantidad().signum() <= 0 || l.precio() == null || l.precio().signum() < 0) throw new ReglaNegocioException("Cantidad y precio inválidos en linea " + n);
            Almacen almacen = almacenDAO.buscarPorId(l.almacenId()).orElseThrow(() -> new NoEncontradoException("Almacen", l.almacenId()));
            if (!Boolean.TRUE.equals(almacen.getEstado())) throw new ReglaNegocioException("El almacen " + l.almacenId() + " está deshabilitado");
            Lote lote;
            if (l.loteId() != null) lote = loteDAO.buscarPorId(l.loteId()).orElseThrow(() -> new NoEncontradoException("Lote", l.loteId()));
            else {
                var nuevo = l.nuevoLote();
                if (nuevo.productoId() == null || nuevo.numLote() == null || nuevo.numLote().isBlank()) throw new ReglaNegocioException("nuevoLote requiere productoId y numLote");
                Producto prod = productoDAO.buscarPorId(nuevo.productoId()).orElseThrow(() -> new NoEncontradoException("Producto", nuevo.productoId()));
                lote = new Lote(); lote.setProducto(prod); lote.setNumLote(nuevo.numLote().trim()); lote.setFechCaducidad(nuevo.fechCaducidad());
                nuevos.add(nuevo); productosNuevos.add(prod);
            }
            lineas.add(new LineaResuelta(lote, almacen, l.cantidad(), l.precio())); almacenes.add(almacen);
            subSinRedondear = subSinRedondear.add(l.cantidad().multiply(l.precio()));
        }
        BigDecimal subtotal = Dinero.redondear(subSinRedondear);
        if (descuento.compareTo(subtotal) > 0) throw new ReglaNegocioException("El descuento no puede superar el subtotal");
        BigDecimal iva = Dinero.multiplicar(subtotal.subtract(descuento), parametros.decimal("iva.tasa"));
        BigDecimal total = Dinero.redondear(subtotal.subtract(descuento).add(iva).add(gastos));
        Cuenta inventario = partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO);
        Cuenta ivaCredito = partidaService.cuentaConfigurada(ConfigContable.CUENTA_IVA_CREDITO);
        Cuenta descCuenta = descuento.signum() > 0 ? partidaService.cuentaConfigurada("cuenta.descuentos.compras") : null;
        Cuenta caja = cmd.formaPago() == FormaPago.Contado ? partidaService.cuentaConfigurada(ConfigContable.CUENTA_CAJA) : null;
        Cuenta bancos = cmd.formaPago() == FormaPago.Cheque ? partidaService.cuentaConfigurada(ConfigContable.CUENTA_BANCOS) : null;
        Cuenta provCuenta = cmd.formaPago() == FormaPago.Credito ? partidaService.cuentaConfigurada(ConfigContable.CUENTA_PROVEEDORES) : null;
        List<LineaAsiento> asiento = Asientos.compraCompleta(inventario, ivaCredito, descCuenta, caja, bancos, provCuenta, cmd.formaPago(), subtotal, descuento, gastos, iva, total);
        if (!asiento.isEmpty()) ReglasPartida.validar(asiento);

        LocalDate fecha = cmd.fecha() == null ? LocalDate.now() : cmd.fecha();
        for (LineaResuelta lr : lineas) if (lr.lote().getId() == null) { loteDAO.crear(lr.lote()); loteDAO.flush(); }
        Compra compra = new Compra(); compra.setFecha(fecha); compra.setProveedor(proveedor); compra.setEmpleado(empleado); compra.setDescripcion(cmd.descripcion());
        compra.setNumeroDocumento(cmd.numeroDocumento()); compra.setFormaPago(cmd.formaPago()); compra.setNumeroCheque(cmd.numeroCheque()); compra.setDescuento(descuento); compra.setGastosAdicionales(gastos); compra.setSubtotal(subtotal); compra.setIva(iva); compra.setTotal(total);
        if (cmd.formaPago() == FormaPago.Credito) { compra.setTasaInteres(parametros.decimal("credito.tasa_mensual")); compra.setPlazoMeses(parametros.entero("credito.plazo_meses")); compra.setFechaVencimiento(fecha.plusMonths(compra.getPlazoMeses())); }
        compraDAO.crear(compra); compraDAO.flush();
        String referencia = "Compra #" + compra.getId();
        Map<Long, List<DetalleKardex>> entradas = new LinkedHashMap<>();
        Map<Long, BigDecimal> gastosPorAlmacen = new LinkedHashMap<>();
        List<CompraDetalle> detalles = new ArrayList<>();
        BigDecimal gastosAsignados = BigDecimal.ZERO;
        for (int i = 0; i < lineas.size(); i++) {
            LineaResuelta l = lineas.get(i);
            BigDecimal peso = subSinRedondear.signum() == 0 ? l.cantidad() : l.importe();
            BigDecimal divisor = subSinRedondear.signum() == 0
                    ? lineas.stream().map(LineaResuelta::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add) : subSinRedondear;
            BigDecimal asignado = i == lineas.size() - 1 ? gastos.subtract(gastosAsignados)
                    : (divisor.signum() == 0 ? BigDecimal.ZERO : Dinero.redondear(gastos.multiply(peso).divide(divisor, 10, java.math.RoundingMode.HALF_UP)));
            gastosAsignados = gastosAsignados.add(asignado);
            BigDecimal costoUnit = Dinero.redondearCosto(l.precio().add(asignado.divide(l.cantidad(), 10, java.math.RoundingMode.HALF_UP)));
            CompraDetalle d = new CompraDetalle(); d.setCompra(compra); d.setLote(l.lote()); d.setAlmacen(l.almacen()); d.setCantidad(l.cantidad()); d.setPrecio(l.precio()); compraDetalleDAO.crear(d); detalles.add(d);
            existenciasService.aumentar(l.lote(), l.almacen(), l.cantidad());
            entradas.computeIfAbsent(l.almacen().getId(), k -> new ArrayList<>()).add(new DetalleKardex(l.lote(), l.cantidad(), costoUnit));
            gastosPorAlmacen.merge(l.almacen().getId(), asignado, BigDecimal::add);
        }
        List<MovimientoKardex> movimientos = new ArrayList<>();
        for (Long almacenId : entradas.keySet()) {
            MovimientoKardex movimiento = kardexService.registrar(TipoMovimiento.Entrada, null,
                    almacenes.stream().filter(a -> a.getId().equals(almacenId)).findFirst().orElseThrow(), compra, null,
                    referencia, fecha.atStartOfDay().atOffset(java.time.ZoneOffset.UTC), entradas.get(almacenId));
            movimiento.kardex().setGastosAdicionales(gastosPorAlmacen.getOrDefault(almacenId, BigDecimal.ZERO));
            movimientos.add(movimiento);
        }
        Partida partida = asiento.isEmpty() ? null : partidaService.registrar(cmd.formaPago() == FormaPago.Credito ? TipoPartida.Diario : TipoPartida.Egreso, fecha, referencia, compra, null, null, asiento);
        return new ResultadoCompra(compra, detalles, movimientos, partida, subtotal, iva, total);
    }
}
