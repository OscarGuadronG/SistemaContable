package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.TipoProductoDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Existencias;
import com.sistemacontable.entity.Producto;
import com.sistemacontable.entity.TipoProducto;
import com.sistemacontable.boundary.dto.InventarioDto;
import com.sistemacontable.boundary.dto.PrecioVentaDto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateless @LocalBean
public class InventarioService {
    @Inject private ProductoDAO productos;
    @Inject private TipoProductoDAO tipos;
    @Inject private AlmacenDAO almacenes;
    @Inject private ExistenciasService existencias;
    @Inject private ParametroService parametros;

    public InventarioDto buscar(Long productoId, Long almacenId) {
        Producto p = productos.buscarPorId(productoId).orElseThrow(() -> new NoEncontradoException("Producto", productoId));
        return map(p, almacenId, true);
    }

    public List<InventarioDto> listar(String buscar, Long tipoId, Long almacenId, String disponibilidad, int first, int max) {
        if (first < 0 || max < 1 || max > 100) throw new IllegalArgumentException("Paginación fuera de rango");
        String disp = disponibilidad == null ? "todos" : disponibilidad.toLowerCase();
        if (!Set.of("con", "sin", "todos").contains(disp)) throw new IllegalArgumentException("disponibilidad debe ser con, sin o todos");
        Set<Long> ids = tipoId == null ? null : descendientes(tipoId);
        List<InventarioDto> filas = new ArrayList<>();
        // Se pagina después de aplicar disponibilidad y almacén para que el total visible sea consistente.
        for (Producto p : todos(buscar, ids)) {
            InventarioDto dto = map(p, almacenId, false);
            boolean tiene = dto.existenciaTotal().signum() > 0;
            if (disp.equals("todos") || (disp.equals("con") == tiene)) filas.add(dto);
        }
        return filas.stream().skip(first).limit(max).toList();
    }

    public long contar(String buscar, Long tipoId, Long almacenId, String disponibilidad) {
        String disp = disponibilidad == null ? "todos" : disponibilidad.toLowerCase();
        if (!Set.of("con", "sin", "todos").contains(disp)) throw new IllegalArgumentException("disponibilidad debe ser con, sin o todos");
        Set<Long> ids = tipoId == null ? null : descendientes(tipoId);
        if (almacenId == null && disp.equals("todos")) return productos.contarInventario(buscar, ids);
        long count = 0;
        for (Producto p : todos(buscar, ids)) {
            boolean tiene = map(p, almacenId, false).existenciaTotal().signum() > 0;
            if (disp.equals("todos") || (disp.equals("con") == tiene)) count++;
        }
        return count;
    }

    public List<PrecioVentaDto> precios(Long almacenId) {
        Almacen almacen = almacenes.buscarPorId(almacenId).orElseThrow(() -> new NoEncontradoException("Almacen", almacenId));
        if (!Boolean.TRUE.equals(almacen.getEstado())) throw new ReglaNegocioException("El almacén está deshabilitado");
        BigDecimal margen = parametros.decimal("venta.margen");
        List<PrecioVentaDto> salida = new ArrayList<>();
        for (Existencias x : existencias.porAlmacen(almacenId)) {
            Producto p = x.getLote().getProducto();
            if (salida.stream().anyMatch(q -> q.productoId().equals(p.getId()))) continue;
            InventarioDto d = map(p, almacenId, false);
            if (d.existenciaTotal().signum() > 0) salida.add(new PrecioVentaDto(p.getId(), p.getNombre(), d.unidad(), d.tipo(), d.existenciaTotal(), d.costoPromedio(), Dinero.redondear(d.costoPromedio().multiply(BigDecimal.ONE.add(margen)))));
        }
        return salida;
    }

    private Set<Long> descendientes(Long tipoId) {
        TipoProducto root = tipos.buscarPorId(tipoId).orElseThrow(() -> new NoEncontradoException("Tipo de producto", tipoId));
        Set<Long> ids = new HashSet<>(); ids.add(root.getId());
        List<TipoProducto> pendientes = new ArrayList<>(); pendientes.add(root);
        for (int i = 0; i < pendientes.size(); i++) for (TipoProducto h : tipos.findHijos(pendientes.get(i))) if (ids.add(h.getId())) pendientes.add(h);
        return ids;
    }

    private List<Producto> todos(String buscar, Set<Long> ids) {
        long total = productos.contarInventario(buscar, ids);
        List<Producto> resultado = new ArrayList<>();
        for (int offset = 0; offset < total; offset += 100) resultado.addAll(productos.buscarInventario(buscar, ids, offset, (int)Math.min(100, total - offset)));
        return resultado;
    }

    private InventarioDto map(Producto p, Long filtroAlmacen, boolean detalle) {
        List<Existencias> filas = existencias.porProducto(p.getId());
        BigDecimal total = BigDecimal.ZERO, valor = BigDecimal.ZERO;
        List<InventarioDto.PorAlmacen> porAlmacen = new ArrayList<>();
        List<InventarioDto.Lote> lotes = new ArrayList<>();
        for (Existencias e : filas) {
            var row = porAlmacen.stream().filter(a -> a.almacenId().equals(e.getAlmacen().getId())).findFirst().orElse(null);
            if (row == null) porAlmacen.add(new InventarioDto.PorAlmacen(e.getAlmacen().getId(), e.getAlmacen().getNombre(), e.getCantidad()));
            else { porAlmacen.remove(row); porAlmacen.add(new InventarioDto.PorAlmacen(row.almacenId(), row.almacen(), row.cantidad().add(e.getCantidad()))); }
            if (filtroAlmacen != null && !filtroAlmacen.equals(e.getAlmacen().getId())) continue;
            total = total.add(e.getCantidad());
            BigDecimal costo = existencias.costoEntradas(e.getLote().getId(), e.getAlmacen().getId()).orElseThrow(() -> new ReglaNegocioException("No existe costo de entrada del lote " + e.getLote().getId() + " en el almacén " + e.getAlmacen().getId()));
            valor = valor.add(costo.multiply(e.getCantidad()));
            if (detalle) lotes.add(new InventarioDto.Lote(e.getLote().getId(), e.getLote().getNumLote(), e.getLote().getFechCaducidad(), e.getAlmacen().getId(), e.getAlmacen().getNombre(), e.getCantidad(), costo));
        }
        BigDecimal promedio = total.signum() == 0 ? BigDecimal.ZERO : Dinero.redondearCosto(valor.divide(total, 10, java.math.RoundingMode.HALF_UP));
        return new InventarioDto(p.getId(), p.getNombre(), p.getDescripcion(), new InventarioDto.Tipo(p.getTipo().getId(), p.getTipo().getNombre()), new InventarioDto.Unidad(p.getUnidadMedida().getId(), p.getUnidadMedida().getNombre(), p.getUnidadMedida().getAbrev()), total, promedio, porAlmacen, detalle ? lotes : null);
    }
}
