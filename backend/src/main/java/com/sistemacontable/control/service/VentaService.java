package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.ClienteDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.VentaDetalleDAO;
import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaVentaCmd;
import com.sistemacontable.control.service.cmd.VentaCmd;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Producto;
import com.sistemacontable.entity.Existencias;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.sistemacontable.security.UsuarioActual;

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
 * al debe, Caja ({@code cuenta.caja}) por la parte de contado y Clientes ({@code cuenta.clientes}) por la parte a
 * credito; Ventas (subtotal) e IVA debito fiscal al haber; y Costo de ventas al debe contra Inventario al haber.</p>
 *
 * <p>Las ventas fijan el precio con el margen de configuración, consumen lotes FIFO y persisten el coste real por
 * detalle y el tipo de pago.</p>
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
    @Inject private ParametroService parametros;
    @Inject private ProductoDAO productoDAO;
    @Inject private UsuarioActual usuarioActual;
    @Inject private KardexDAO kardexDAO;
    @Inject private PartidaDAO partidaDAO;
    @Inject private PartidaDetalleDAO partidaDetalleDAO;

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
    public List<Venta> buscar(LocalDate desde, LocalDate hasta, Long clienteId, FormaPago formaPago, int first, int max) { return ventaDAO.buscar(desde,hasta,clienteId,formaPago,first,max); }
    public long contar(LocalDate desde, LocalDate hasta, Long clienteId, FormaPago formaPago) { return ventaDAO.contarBuscar(desde,hasta,clienteId,formaPago); }
    public List<Kardex> kardex(Long ventaId) { return kardexDAO.findByVenta(ventaId); }
    public List<com.sistemacontable.entity.KardexDetalle> kardexDetalles(Long id) { return kardexService.detalles(id); }
    public Partida partida(Long ventaId) { return partidaDAO.findByVenta(ventaId).orElse(null); }
    public List<PartidaDetalle> partidaLineas(Long id) { return partidaDetalleDAO.buscarPorPartida(id); }

    public ResultadoVenta registrar(VentaCmd cmd) {
        if (cmd == null || cmd.clienteId() == null) throw new ReglaNegocioException("La venta debe indicar cliente");
        Long empleadoId = cmd.empleadoId() != null ? cmd.empleadoId() : usuarioActual == null ? null : usuarioActual.getIdEmpleado();
        if (empleadoId == null) throw new ReglaNegocioException("Debe indicar empleadoId o contar con un empleado en el perfil autenticado");
        if (cmd.almacenId() == null || cmd.formaPago() == null || cmd.lineas().isEmpty()) throw new ReglaNegocioException("Almacén, forma de pago y al menos una línea son obligatorios");
        if (cmd.formaPago() == FormaPago.Cheque && (cmd.numeroCheque() == null || cmd.numeroCheque().isBlank())) throw new ReglaNegocioException("numeroCheque es obligatorio para pago con cheque");
        Cliente cliente = clienteDAO.buscarPorId(cmd.clienteId()).orElseThrow(() -> new NoEncontradoException("Cliente", cmd.clienteId()));
        Empleado empleado = empleadoDAO.buscarPorId(empleadoId).orElseThrow(() -> new NoEncontradoException("Empleado", empleadoId));
        Almacen almacen = almacenDAO.buscarPorId(cmd.almacenId()).orElseThrow(() -> new NoEncontradoException("Almacen", cmd.almacenId()));
        if (!Boolean.TRUE.equals(almacen.getEstado())) throw new ReglaNegocioException("El almacén está deshabilitado");
        BigDecimal margen = parametros.decimal("venta.margen"), tasaIva = parametros.decimal("iva.tasa");
        int indiceLinea = 0;
        for (LineaVentaCmd linea : cmd.lineas()) {
            indiceLinea++;
            if (linea == null || linea.productoId() == null || linea.cantidad() == null || linea.cantidad().signum() <= 0)
                throw new ReglaNegocioException("Línea " + indiceLinea + " inválida");
        }
        List<VentaItem> items = new ArrayList<>();
        Map<ClaveStock, BigDecimal> requeridos = new LinkedHashMap<>();
        Map<Long, BigDecimal> consumidoPorLote = new HashMap<>();
        BigDecimal subtotalSinRedondear = BigDecimal.ZERO, costoSinRedondear = BigDecimal.ZERO;
        int n = 0;
        List<LineaVentaCmd> lineasOrdenadas = cmd.lineas().stream().sorted(java.util.Comparator.comparing(LineaVentaCmd::productoId)).toList();
        for (LineaVentaCmd req : lineasOrdenadas) {
            n++;
            if (req == null || req.productoId() == null || req.cantidad() == null || req.cantidad().signum() <= 0) throw new ReglaNegocioException("Línea " + n + " inválida");
            Producto producto = productoDAO.buscarPorId(req.productoId()).orElseThrow(() -> new NoEncontradoException("Producto", req.productoId()));
            existenciasService.bloquearProductoAlmacen(producto.getId(), almacen.getId());
            List<Existencias> stocks = existenciasService.porProductoAlmacen(producto.getId(), almacen.getId());
            BigDecimal disponible = stocks.stream().map(Existencias::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (disponible.compareTo(req.cantidad()) < 0) throw new ReglaNegocioException("Existencia insuficiente del producto " + producto.getId() + " en almacén " + almacen.getId());
            Map<Long, BigDecimal> costos = new HashMap<>(); BigDecimal valorExistencia = BigDecimal.ZERO;
            for (Existencias stock : stocks) {
                BigDecimal costo = existenciasService.costoEntradas(stock.getLote().getId(), almacen.getId()).orElseThrow(() -> new ReglaNegocioException("No existe costo de entrada del lote " + stock.getLote().getId() + " en almacén " + almacen.getId()));
                costos.put(stock.getLote().getId(), costo); valorExistencia = valorExistencia.add(costo.multiply(stock.getCantidad()));
            }
            BigDecimal costoPromedio = Dinero.redondearCosto(valorExistencia.divide(disponible, 10, java.math.RoundingMode.HALF_UP));
            BigDecimal precio = Dinero.redondear(costoPromedio.multiply(BigDecimal.ONE.add(margen)));
            BigDecimal faltante = req.cantidad();
            stocks.sort((a,b) -> {
                var ta = existenciasService.primeraEntrada(a.getLote().getId(), almacen.getId()).orElse(java.time.OffsetDateTime.MAX);
                var tb = existenciasService.primeraEntrada(b.getLote().getId(), almacen.getId()).orElse(java.time.OffsetDateTime.MAX);
                int c = ta.compareTo(tb); return c != 0 ? c : a.getLote().getId().compareTo(b.getLote().getId());
            });
            for (Existencias stock : stocks) {
                if (faltante.signum() <= 0) break;
                BigDecimal disponibleLote = stock.getCantidad().subtract(consumidoPorLote.getOrDefault(stock.getLote().getId(), BigDecimal.ZERO));
                if (disponibleLote.signum() <= 0) continue;
                BigDecimal toma = faltante.min(disponibleLote), costo = costos.get(stock.getLote().getId());
                items.add(new VentaItem(stock.getLote(), toma, precio, costo));
                consumidoPorLote.merge(stock.getLote().getId(), toma, BigDecimal::add);
                requeridos.merge(new ClaveStock(stock.getLote().getId(), almacen.getId()), toma, BigDecimal::add);
                subtotalSinRedondear = subtotalSinRedondear.add(toma.multiply(precio));
                costoSinRedondear = costoSinRedondear.add(toma.multiply(costo));
                faltante = faltante.subtract(toma);
            }
            if (faltante.signum() > 0) throw new ReglaNegocioException("Existencia insuficiente para la cantidad acumulada del producto " + producto.getId());
        }
        existenciasService.verificarDisponible(requeridos);
        BigDecimal subtotal = Dinero.redondear(subtotalSinRedondear), descuento = Dinero.redondear(cmd.descuento() == null ? BigDecimal.ZERO : cmd.descuento());
        if (descuento.signum() < 0 || descuento.compareTo(subtotal) > 0) throw new ReglaNegocioException("El descuento debe estar entre cero y el subtotal");
        BigDecimal iva = Dinero.multiplicar(subtotal.subtract(descuento), tasaIva), total = Dinero.redondear(subtotal.subtract(descuento).add(iva));
        BigDecimal costoTotal = Dinero.redondear(costoSinRedondear);
        Cuenta caja = cmd.formaPago() == FormaPago.Contado ? partidaService.cuentaConfigurada(ConfigContable.CUENTA_CAJA) : null;
        Cuenta bancos = cmd.formaPago() == FormaPago.Cheque ? partidaService.cuentaConfigurada(ConfigContable.CUENTA_BANCOS) : null;
        Cuenta clientes = cmd.formaPago() == FormaPago.Credito ? partidaService.cuentaConfigurada(ConfigContable.CUENTA_CLIENTES) : null;
        Cuenta rebajas = descuento.signum() > 0 ? partidaService.cuentaConfigurada("cuenta.rebajas.ventas") : null;
        Cuenta ventas = partidaService.cuentaConfigurada(ConfigContable.CUENTA_VENTAS), ivaDebito = partidaService.cuentaConfigurada(ConfigContable.CUENTA_IVA_DEBITO), costoVentasCuenta = partidaService.cuentaConfigurada(ConfigContable.CUENTA_COSTO_VENTAS), inventario = partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO);
        List<LineaAsiento> asiento = Asientos.ventaCompleta(caja, bancos, clientes, rebajas, ventas, ivaDebito, costoVentasCuenta, inventario, cmd.formaPago(), subtotal, descuento, iva, total, costoTotal);
        if (!asiento.isEmpty()) ReglasPartida.validar(asiento);
        LocalDate fecha = cmd.fecha() == null ? LocalDate.now() : cmd.fecha();
        Venta venta = new Venta(); venta.setFecha(fecha); venta.setCliente(cliente); venta.setEmpleado(empleado); venta.setAlmacen(almacen); venta.setDescripcion(cmd.descripcion()); venta.setNumeroDocumento(cmd.numeroDocumento()); venta.setFormaPago(cmd.formaPago()); venta.setNumeroCheque(cmd.numeroCheque()); venta.setDescuento(descuento); venta.setSubtotal(subtotal); venta.setIva(iva); venta.setTotal(total); venta.setCostoTotal(costoTotal);
        if (cmd.formaPago() == FormaPago.Credito) { venta.setTasaInteres(parametros.decimal("credito.tasa_mensual")); venta.setPlazoMeses(parametros.entero("credito.plazo_meses")); venta.setFechaVencimiento(fecha.plusMonths(venta.getPlazoMeses())); }
        ventaDAO.crear(venta); ventaDAO.flush(); String referencia = "Venta #" + venta.getId();
        List<VentaDetalle> detalles = new ArrayList<>(); List<DetalleKardex> kardexLineas = new ArrayList<>();
        for (VentaItem item : items) {
            existenciasService.disminuir(item.lote(), almacen, item.cantidad());
            VentaDetalle d = new VentaDetalle(); d.setVenta(venta); d.setLote(item.lote()); d.setAlmacen(almacen); d.setCantidad(item.cantidad()); d.setPrecio(item.precio()); d.setCostoUnitario(item.costo()); ventaDetalleDAO.crear(d); detalles.add(d);
            kardexLineas.add(new DetalleKardex(item.lote(), item.cantidad(), item.costo()));
        }
        MovimientoKardex movimiento = kardexService.registrar(TipoMovimiento.Salida, almacen, null, null, venta, referencia, fecha.atStartOfDay().atOffset(java.time.ZoneOffset.UTC), kardexLineas);
        Partida partida = asiento.isEmpty() ? null : partidaService.registrar(TipoPartida.Ingreso, fecha, referencia, null, venta, null, asiento);
        return new ResultadoVenta(venta, detalles, List.of(movimiento), partida, subtotal, iva, total, costoTotal);
    }

    private record VentaItem(Lote lote, BigDecimal cantidad, BigDecimal precio, BigDecimal costo) {}
}
