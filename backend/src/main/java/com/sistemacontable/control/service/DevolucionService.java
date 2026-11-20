package com.sistemacontable.control.service;

import com.sistemacontable.control.FinanzasDAO;
import com.sistemacontable.boundary.dto.*;
import com.sistemacontable.control.service.cmd.*;
import com.sistemacontable.boundary.mapper.ContabilidadMapper;
import com.sistemacontable.boundary.mapper.InventarioMapper;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.*;
import com.sistemacontable.entity.enums.*;
import com.sistemacontable.security.UsuarioActual;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/** Reversión de inventario, documento y asiento bajo la misma transacción JTA. */
@Stateless
@LocalBean
public class DevolucionService {
    @Inject private FinanzasDAO dao;
    @Inject private ParametroService parametros;
    @Inject private PartidaService partidas;
    @Inject private ExistenciasService stock;
    @Inject private KardexService kardex;
    @Inject private UsuarioActual usuario;

    public DevolucionDto comprar(DevolucionCompraCmd cmd) {
        if (cmd == null) throw new ReglaNegocioException("La devolución es obligatoria");
        Compra compra = dao.buscar(Compra.class, cmd.compraId(), true);
        ReglasDevolucion.plazo(compra.getFecha(), cmd.fecha(), parametros.entero("devolucion_compra.dias_max"));
        BigDecimal porcentaje = cmd.porcentajeCompensacion();
        if (porcentaje == null || porcentaje.signum() < 0 || porcentaje.compareTo(new BigDecimal("100")) > 0)
            throw new ReglaNegocioException("El porcentaje de compensación debe estar entre 0 y 100");
        Empleado empleado = empleado(cmd.empleadoId());
        Map<ClaveStock, BigDecimal> solicitadas = agrupar(cmd.lineas());
        List<CompraDetalle> originales = dao.lineasCompra(compra.getId());
        List<KardexDetalle> entradas = dao.entradasCompra(compra.getId());
        List<Linea> lineas = new ArrayList<>();
        for (var solicitud : solicitadas.entrySet()) {
            ClaveStock clave = solicitud.getKey();
            BigDecimal cantidadOriginal = originales.stream().filter(d -> coincide(clave, d.getLote(), d.getAlmacen()))
                    .map(CompraDetalle::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
            ReglasDevolucion.cantidad(solicitud.getValue(), cantidadOriginal, dao.cantidadDevuelta(false, compra.getId(), clave.loteId(), clave.almacenId()));
            List<KardexDetalle> costos = entradas.stream().filter(d -> coincide(clave, d.getLote(), d.getKardex().getDestino())).toList();
            BigDecimal cantidad = costos.stream().map(KardexDetalle::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (cantidad.signum() == 0) throw new ReglaNegocioException("No existe la entrada de compra para determinar el costo");
            BigDecimal valor = costos.stream().map(d -> d.getCantidad().multiply(d.getPrecioCosto())).reduce(BigDecimal.ZERO, BigDecimal::add);
            lineas.add(new Linea(dao.buscar(Lote.class, clave.loteId(), false), almacen(clave.almacenId()), solicitud.getValue(), null,
                    valor.divide(cantidad, Dinero.ESCALA_COSTO, Dinero.MODO)));
        }
        stock.verificarDisponible(solicitadas);
        BigDecimal costo = Dinero.redondear(lineas.stream().map(l -> l.cantidad().multiply(l.costo())).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal compensacion = Dinero.redondear(costo.multiply(porcentaje).divide(new BigDecimal("100")));
        DevolucionCompra d = new DevolucionCompra(); d.setCompra(compra); d.setEmpleado(empleado); d.setFecha(cmd.fecha());
        d.setPorcentajeCompensacion(porcentaje); d.setMontoCosto(costo); d.setMontoCompensado(compensacion); d.setMotivo(cmd.motivo());
        dao.guardar(d); dao.flush();
        for (Linea l : lineas) {
            DevolucionCompraDetalle det = new DevolucionCompraDetalle(); det.setDevolucion(d); det.setLote(l.lote()); det.setAlmacen(l.almacen());
            det.setCantidad(l.cantidad()); det.setCostoUnitario(l.costo()); dao.guardar(det);
            stock.disminuir(l.lote(), l.almacen(), l.cantidad());
        }
        movimientos(false, d.getId(), cmd.fecha(), cmd.motivo(), lineas);
        List<LineaAsiento> asiento = new ArrayList<>();
        cargo(asiento, contrapartida(compra.getFormaPago(), false), compensacion);
        cargo(asiento, "cuenta.otros.gastos", costo.subtract(compensacion));
        abono(asiento, "cuenta.inventario", costo);
        if (!asiento.isEmpty()) {
            Partida p = partidas.registrar(TipoPartida.Diario, cmd.fecha(), "Devolución de compra #" + compra.getId(), null, null, null, asiento);
            p.setIdDevolucionCompra(d.getId());
        }
        dao.flush(); return obtenerCompra(d.getId());
    }
    public DevolucionDto vender(DevolucionVentaCmd cmd) {
        if (cmd == null) throw new ReglaNegocioException("La devolución es obligatoria");
        Venta venta = dao.buscar(Venta.class, cmd.ventaId(), true);
        ReglasDevolucion.plazo(venta.getFecha(), cmd.fecha(), parametros.entero("devolucion_venta.dias_max"));
        Empleado empleado = empleado(cmd.empleadoId());
        Map<ClaveStock, BigDecimal> solicitadas = agrupar(cmd.lineas());
        List<VentaDetalle> originales = dao.lineasVenta(venta.getId());
        List<Linea> lineas = new ArrayList<>();
        // El cliente puede devolver a otro almacén; el límite se controla por lote en toda la venta.
        Map<Long, BigDecimal> acumulado = new HashMap<>();
        for (var solicitud : solicitadas.entrySet()) {
            ClaveStock clave = solicitud.getKey();
            List<VentaDetalle> origen = originales.stream().filter(d -> d.getLote().getId().equals(clave.loteId())).toList();
            BigDecimal cantidad = origen.stream().map(VentaDetalle::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal pedida = acumulado.merge(clave.loteId(), solicitud.getValue(), BigDecimal::add);
            ReglasDevolucion.cantidad(pedida, cantidad, dao.cantidadDevuelta(true, venta.getId(), clave.loteId(), clave.almacenId()));
            BigDecimal precio = origen.stream().map(l -> l.getCantidad().multiply(l.getPrecio())).reduce(BigDecimal.ZERO, BigDecimal::add).divide(cantidad, Dinero.ESCALA_COSTO, Dinero.MODO);
            BigDecimal costo = origen.stream().map(l -> l.getCantidad().multiply(l.getCostoUnitario())).reduce(BigDecimal.ZERO, BigDecimal::add).divide(cantidad, Dinero.ESCALA_COSTO, Dinero.MODO);
            lineas.add(new Linea(origen.get(0).getLote(), almacen(clave.almacenId()), solicitud.getValue(), precio, costo));
        }
        BigDecimal bruto = lineas.stream().map(l -> l.cantidad().multiply(l.precio())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal proporcion = venta.getSubtotal().signum() == 0 ? BigDecimal.ONE : BigDecimal.ONE.subtract(venta.getDescuento().divide(venta.getSubtotal(), 12, Dinero.MODO));
        BigDecimal monto = Dinero.redondear(bruto.multiply(proporcion));
        BigDecimal iva = Dinero.multiplicar(monto, parametros.decimal("iva.tasa"));
        BigDecimal costo = Dinero.redondear(lineas.stream().map(l -> l.cantidad().multiply(l.costo())).reduce(BigDecimal.ZERO, BigDecimal::add));
        DevolucionVenta d = new DevolucionVenta(); d.setVenta(venta); d.setEmpleado(empleado); d.setFecha(cmd.fecha());
        d.setMonto(monto); d.setCosto(costo); d.setMotivo(cmd.motivo()); dao.guardar(d); dao.flush();
        for (Linea l : lineas) {
            DevolucionVentaDetalle det = new DevolucionVentaDetalle(); det.setDevolucion(d); det.setLote(l.lote()); det.setAlmacen(l.almacen());
            det.setCantidad(l.cantidad()); det.setPrecio(l.precio()); det.setCostoUnitario(l.costo()); dao.guardar(det);
            stock.aumentar(l.lote(), l.almacen(), l.cantidad());
        }
        movimientos(true, d.getId(), cmd.fecha(), cmd.motivo(), lineas);
        List<LineaAsiento> asiento = new ArrayList<>();
        cargo(asiento, "cuenta.devoluciones.ventas", monto); cargo(asiento, "cuenta.iva.debito", iva); cargo(asiento, "cuenta.inventario", costo);
        abono(asiento, contrapartida(venta.getFormaPago(), true), monto.add(iva)); abono(asiento, "cuenta.costo.ventas", costo);
        if (!asiento.isEmpty()) {
            Partida p = partidas.registrar(TipoPartida.Diario, cmd.fecha(), "Devolución de venta #" + venta.getId(), null, null, null, asiento);
            p.setIdDevolucionVenta(d.getId());
        }
        dao.flush(); return obtenerVenta(d.getId());
    }
    private void movimientos(boolean venta, Long id, LocalDate fecha, String motivo, List<Linea> lineas) {
        Map<Almacen, List<DetalleKardex>> grupos = new LinkedHashMap<>();
        for (Linea l : lineas) grupos.computeIfAbsent(l.almacen(), a -> new ArrayList<>()).add(new DetalleKardex(l.lote(), l.cantidad(), l.costo()));
        for (var grupo : grupos.entrySet()) {
            MovimientoKardex m = kardex.registrar(TipoMovimiento.Devolucion, venta ? null : grupo.getKey(), venta ? grupo.getKey() : null,
                    null, null, motivo == null || motivo.isBlank() ? "Devolución #" + id : motivo, grupo.getValue());
            if (venta) m.kardex().setIdDevolucionVenta(id); else m.kardex().setIdDevolucionCompra(id);
            m.kardex().setFecha(fecha.atStartOfDay().atOffset(ZoneOffset.UTC));
        }
    }
    public DevolucionDto obtenerCompra(Long id) {
        DevolucionCompra d = dao.buscar(DevolucionCompra.class, id, false);
        return new DevolucionDto(d.getId(), d.getCompra().getId(), null, d.getEmpleado().getId(), d.getFecha(), d.getPorcentajeCompensacion(),
                d.getMontoCosto(), d.getMontoCompensado(), null, null, d.getMotivo(),
                dao.detallesCompra(id).stream().map(l -> new DevolucionDto.Linea(l.getId(),l.getLote().getId(),l.getAlmacen().getId(),l.getCantidad(),null,l.getCostoUnitario())).toList(), movimientosDto(false,id), partidaDto(false,id));
    }
    public DevolucionDto obtenerVenta(Long id) {
        DevolucionVenta d = dao.buscar(DevolucionVenta.class, id, false);
        return new DevolucionDto(d.getId(), null, d.getVenta().getId(), d.getEmpleado().getId(), d.getFecha(), null, null, null,
                d.getMonto(), d.getCosto(), d.getMotivo(),
                dao.detallesVenta(id).stream().map(l -> new DevolucionDto.Linea(l.getId(),l.getLote().getId(),l.getAlmacen().getId(),l.getCantidad(),l.getPrecio(),l.getCostoUnitario())).toList(), movimientosDto(true,id), partidaDto(true,id));
    }
    public List<DevolucionDto> listarCompra(LocalDate desde, LocalDate hasta, Long id) {
        rango(desde,hasta); return dao.devolucionesCompra(desde,hasta,id).stream().map(d -> obtenerCompra(d.getId())).toList();
    }
    public List<DevolucionDto> listarVenta(LocalDate desde, LocalDate hasta, Long id) {
        rango(desde,hasta); return dao.devolucionesVenta(desde,hasta,id).stream().map(d -> obtenerVenta(d.getId())).toList();
    }
    private List<KardexDto> movimientosDto(boolean venta, Long id) {
        return dao.kardexDevolucion(venta,id).stream().map(k -> InventarioMapper.kardex(k,kardex.detalles(k.getId()))).toList();
    }
    private PartidaDto partidaDto(boolean venta, Long id) {
        Partida p = dao.partidaDevolucion(venta,id); return p == null ? null : ContabilidadMapper.partida(p,partidas.lineas(p.getId()));
    }
    private Empleado empleado(Long id) {
        Long empleado = id == null ? usuario.getIdEmpleado() : id;
        if (empleado == null) throw new ReglaNegocioException("Debe indicar un empleado o vincularlo al perfil");
        return dao.buscar(Empleado.class,empleado,false);
    }
    private Almacen almacen(Long id) {
        Almacen a = dao.buscar(Almacen.class,id,false);
        if (!Boolean.TRUE.equals(a.getEstado())) throw new ReglaNegocioException("El almacén está inhabilitado"); return a;
    }
    private static boolean coincide(ClaveStock c, Lote l, Almacen a) {
        return a != null && c.loteId().equals(l.getId()) && c.almacenId().equals(a.getId());
    }
    private static Map<ClaveStock,BigDecimal> agrupar(List<LineaDevolucionCmd> lineas) {
        if (lineas == null || lineas.isEmpty()) throw new ReglaNegocioException("Debe indicar al menos una línea");
        Map<ClaveStock,BigDecimal> mapa = new LinkedHashMap<>();
        for (LineaDevolucionCmd l : lineas) {
            if (l == null || l.loteId() == null || l.almacenId() == null || l.cantidad() == null || l.cantidad().signum() <= 0)
                throw new ReglaNegocioException("Cada línea debe indicar lote, almacén y cantidad mayor que cero");
            mapa.merge(new ClaveStock(l.loteId(),l.almacenId()),l.cantidad(),BigDecimal::add);
        } return mapa;
    }
    private void cargo(List<LineaAsiento> a,String clave,BigDecimal monto) { if (monto.signum()>0) a.add(LineaAsiento.cargo(partidas.cuentaConfigurada(clave),monto)); }
    private void abono(List<LineaAsiento> a,String clave,BigDecimal monto) { if (monto.signum()>0) a.add(LineaAsiento.abono(partidas.cuentaConfigurada(clave),monto)); }
    private static String contrapartida(FormaPago forma,boolean venta) {
        if (forma == FormaPago.Credito) return venta ? "cuenta.clientes" : "cuenta.proveedores";
        return forma == FormaPago.Cheque ? "cuenta.bancos" : "cuenta.caja";
    }
    private static void rango(LocalDate d,LocalDate h) { if (d!=null && h!=null && d.isAfter(h)) throw new IllegalArgumentException("Rango de fechas inválido"); }
    private record Linea(Lote lote,Almacen almacen,BigDecimal cantidad,BigDecimal precio,BigDecimal costo) { }
}
