package com.sistemacontable.boundary.mapper;

import com.sistemacontable.boundary.dto.CompraDto;
import com.sistemacontable.boundary.dto.ExistenciasDto;
import com.sistemacontable.boundary.dto.KardexDto;
import com.sistemacontable.boundary.dto.ResultadoAjusteDto;
import com.sistemacontable.boundary.dto.ResultadoCompraDto;
import com.sistemacontable.boundary.dto.ResultadoVentaDto;
import com.sistemacontable.boundary.dto.VentaDto;
import com.sistemacontable.boundary.dto.PartidaDto;
import com.sistemacontable.control.service.MovimientoKardex;
import com.sistemacontable.control.service.ResultadoAjuste;
import com.sistemacontable.control.service.ResultadoCompra;
import com.sistemacontable.control.service.ResultadoVenta;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.Existencias;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import java.util.List;

/**
 * Entidad a DTO de compras, ventas, kardex y existencias. Las {@code lineas} nulas se conservan nulas (los listados
 * no las incluyen); vacias significan documento sin lineas.
 */
public final class InventarioMapper {

    private InventarioMapper() {
    }

    public static ExistenciasDto existencias(Existencias e) {
        return e == null ? null : new ExistenciasDto(e.getId(), Mapeo.id(e.getLote()), Mapeo.id(e.getAlmacen()),
                e.getCantidad());
    }

    public static CompraDto compra(Compra c, List<CompraDetalle> lineas) {
        return compra(c, lineas, null, null);
    }
    public static CompraDto compra(Compra c, List<CompraDetalle> lineas, List<KardexDto> kardex, PartidaDto partida) {
        return c == null ? null : new CompraDto(c.getId(), c.getFecha(), Mapeo.id(c.getProveedor()),
                Mapeo.id(c.getEmpleado()), c.getNumeroDocumento(), c.getDescripcion(), c.getFormaPago(), c.getNumeroCheque(),
                c.getDescuento(), c.getGastosAdicionales(), c.getSubtotal(), c.getIva(), c.getTotal(), c.getTasaInteres(), c.getPlazoMeses(), c.getFechaVencimiento(), kardex, partida, Mapeo.lista(lineas, InventarioMapper::linea));
    }

    public static VentaDto venta(Venta v, List<VentaDetalle> lineas) {
        return venta(v, lineas, null, null);
    }
    public static VentaDto venta(Venta v, List<VentaDetalle> lineas, List<KardexDto> kardex, PartidaDto partida) {
        return v == null ? null : new VentaDto(v.getId(), v.getFecha(), Mapeo.id(v.getCliente()),
                Mapeo.id(v.getEmpleado()), Mapeo.id(v.getAlmacen()), v.getNumeroDocumento(), v.getDescripcion(), v.getFormaPago(), v.getNumeroCheque(),
                v.getDescuento(), v.getSubtotal(), v.getIva(), v.getTotal(), v.getCostoTotal(), v.getTasaInteres(), v.getPlazoMeses(), v.getFechaVencimiento(), kardex, partida, Mapeo.lista(lineas, InventarioMapper::linea));
    }

    public static KardexDto kardex(Kardex k, List<KardexDetalle> lineas) {
        return k == null ? null : new KardexDto(k.getId(), k.getTipoMovimiento(), k.getFecha(), Mapeo.id(k.getOrigen()),
                Mapeo.id(k.getDestino()), Mapeo.id(k.getCompra()), Mapeo.id(k.getVenta()), k.getDescripcion(), k.getGastosAdicionales(), k.getIdDevolucionCompra(), k.getIdDevolucionVenta(),
                Mapeo.lista(lineas, InventarioMapper::linea));
    }

    public static KardexDto movimiento(MovimientoKardex m) {
        return m == null ? null : kardex(m.kardex(), m.detalles());
    }

    public static ResultadoCompraDto resultado(ResultadoCompra r) {
        return new ResultadoCompraDto(compra(r.compra(), r.detalles()),
                Mapeo.lista(r.movimientos(), InventarioMapper::movimiento), Mapeo.id(r.partida()), r.subtotal(),
                r.iva(), r.total());
    }

    public static ResultadoVentaDto resultado(ResultadoVenta r) {
        return new ResultadoVentaDto(venta(r.venta(), r.detalles()),
                Mapeo.lista(r.movimientos(), InventarioMapper::movimiento), Mapeo.id(r.partida()), r.subtotal(),
                r.iva(), r.total(), r.costoVentas());
    }

    public static ResultadoAjusteDto resultado(ResultadoAjuste r) {
        return new ResultadoAjusteDto(movimiento(r.movimiento()), Mapeo.id(r.partida()));
    }

    private static CompraDto.Linea linea(CompraDetalle d) {
        return new CompraDto.Linea(d.getId(), Mapeo.id(d.getLote()), Mapeo.id(d.getAlmacen()), d.getCantidad(),
                d.getPrecio());
    }

    private static VentaDto.Linea linea(VentaDetalle d) {
        return new VentaDto.Linea(d.getId(), Mapeo.id(d.getLote()), Mapeo.id(d.getAlmacen()), d.getCantidad(),
                d.getPrecio(), d.getCostoUnitario());
    }

    private static KardexDto.Linea linea(KardexDetalle d) {
        return new KardexDto.Linea(d.getId(), Mapeo.id(d.getLote()), d.getCantidad(), d.getPrecioCosto());
    }
}
