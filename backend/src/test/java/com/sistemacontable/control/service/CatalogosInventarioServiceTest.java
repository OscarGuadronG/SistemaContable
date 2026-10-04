package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.ExistenciasDAO;
import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.KardexDetalleDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.TipoProductoDAO;
import com.sistemacontable.control.UnidadMedidaDAO;
import com.sistemacontable.control.VentaDetalleDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.AlmacenCmd;
import com.sistemacontable.control.service.cmd.LoteCmd;
import com.sistemacontable.control.service.cmd.ProductoCmd;
import com.sistemacontable.control.service.cmd.TipoProductoCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Producto;
import com.sistemacontable.entity.TipoProducto;
import com.sistemacontable.entity.UnidadMedida;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tipos de producto, productos, lotes y almacenes: servicios delgados con jerarquia sin ciclos y borrado seguro. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogosInventarioServiceTest {

    @Mock
    TipoProductoDAO tipoProductoDAO;
    @Mock
    ProductoDAO productoDAO;
    @Mock
    UnidadMedidaDAO unidadMedidaDAO;
    @Mock
    LoteDAO loteDAO;
    @Mock
    AlmacenDAO almacenDAO;
    @Mock
    ExistenciasDAO existenciasDAO;
    @Mock
    CompraDetalleDAO compraDetalleDAO;
    @Mock
    VentaDetalleDAO ventaDetalleDAO;
    @Mock
    KardexDetalleDAO kardexDetalleDAO;
    @Mock
    KardexDAO kardexDAO;
    @InjectMocks
    TipoProductoService tipos;
    @InjectMocks
    ProductoService productos;
    @InjectMocks
    LoteService lotes;
    @InjectMocks
    AlmacenService almacenes;

    private final TipoProducto raiz = tipo(1L, "Alimentos", null);
    private final TipoProducto hijo = tipo(2L, "Lacteos", raiz);
    private final TipoProducto nieto = tipo(3L, "Quesos", hijo);
    private final UnidadMedida unidad = unidad();
    private final Producto producto = producto();
    private final Lote lote = lote();
    private final Almacen almacen = almacen();

    private static TipoProducto tipo(long id, String nombre, TipoProducto padre) {
        TipoProducto t = new TipoProducto();
        t.setId(id);
        t.setNombre(nombre);
        t.setPadre(padre);
        return t;
    }

    private static UnidadMedida unidad() {
        UnidadMedida u = new UnidadMedida();
        u.setId(5L);
        return u;
    }

    private Producto producto() {
        Producto p = new Producto();
        p.setId(6L);
        p.setNombre("Leche");
        p.setTipo(hijo);
        p.setUnidadMedida(unidad);
        return p;
    }

    private Lote lote() {
        Lote l = new Lote();
        l.setId(7L);
        l.setNumLote("L-1");
        l.setProducto(producto);
        return l;
    }

    private static Almacen almacen() {
        Almacen a = new Almacen();
        a.setId(8L);
        a.setNombre("Central");
        a.setEstado(true);
        return a;
    }

    @BeforeEach
    void preparar() {
        for (TipoProducto t : List.of(raiz, hijo, nieto)) {
            when(tipoProductoDAO.buscarPorId(t.getId())).thenReturn(Optional.of(t));
            when(tipoProductoDAO.findHijos(t)).thenReturn(List.of());
        }
        when(tipoProductoDAO.findHijos(raiz)).thenReturn(List.of(hijo));
        when(tipoProductoDAO.findHijos(hijo)).thenReturn(List.of(nieto));
        when(unidadMedidaDAO.buscarPorId(5L)).thenReturn(Optional.of(unidad));
        when(productoDAO.buscarPorId(6L)).thenReturn(Optional.of(producto));
        when(loteDAO.buscarPorId(7L)).thenReturn(Optional.of(lote));
        when(almacenDAO.buscarPorId(8L)).thenReturn(Optional.of(almacen));
    }

    // ------------------------------------------------------------------------------ tipos de producto

    @Test
    void tipoRaizYTipoConPadre() {
        TipoProducto r = tipos.crear(new TipoProductoCmd(" Bebidas ", null));
        TipoProducto h = tipos.crear(new TipoProductoCmd("Jugos", 1L));

        assertEquals("Bebidas", r.getNombre());
        assertNull(r.getPadre());
        assertSame(raiz, h.getPadre());
        assertThrows(ReglaNegocioException.class, () -> tipos.crear(new TipoProductoCmd(" ", null)));
        when(tipoProductoDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> tipos.crear(new TipoProductoCmd("x", 99L)));
    }

    @Test
    void tipoNoPuedeSerPadreDeSiMismoNiDeUnAncestro() {
        ReglaNegocioException propio = assertThrows(ReglaNegocioException.class,
                () -> tipos.modificar(1L, new TipoProductoCmd("Alimentos", 1L)));
        assertThrows(ReglaNegocioException.class, () -> tipos.modificar(1L, new TipoProductoCmd("Alimentos", 2L)));
        assertThrows(ReglaNegocioException.class, () -> tipos.modificar(1L, new TipoProductoCmd("Alimentos", 3L)));

        assertTrue(propio.getMessage().contains("ciclo"), propio.getMessage());
        verify(tipoProductoDAO, never()).modificar(any());
    }

    @Test
    void tipoSePuedeMoverAOtraRamaOARaiz() {
        assertSame(raiz, tipos.modificar(3L, new TipoProductoCmd("Quesos", 1L)).getPadre());
        assertNull(tipos.modificar(3L, new TipoProductoCmd("Quesos", null)).getPadre());
    }

    @Test
    void tipoConHijosOConProductosNoSeElimina() {
        assertThrows(ReglaNegocioException.class, () -> tipos.eliminar(1L));

        when(productoDAO.existePorReferencia("tipo", nieto)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> tipos.eliminar(3L));
        verify(tipoProductoDAO, never()).eliminar(any());

        when(productoDAO.existePorReferencia("tipo", nieto)).thenReturn(false);
        tipos.eliminar(3L);
        verify(tipoProductoDAO).eliminar(3L);
    }

    @Test
    void tipoConsultas() {
        when(tipoProductoDAO.findRaices()).thenReturn(List.of(raiz));

        assertEquals(List.of(raiz), tipos.raices());
        assertEquals(List.of(hijo), tipos.hijos(1L));
        assertSame(raiz, tipos.buscarPorId(1L));
    }

    // ------------------------------------------------------------------------------------- productos

    @Test
    void productoRequiereNombreUnidadYTipoExistentes() {
        Producto p = productos.crear(new ProductoCmd(" Queso ", 5L, 3L, " "));

        verify(productoDAO).crear(p);
        assertEquals("Queso", p.getNombre());
        assertSame(unidad, p.getUnidadMedida());
        assertSame(nieto, p.getTipo());
        assertNull(p.getDescripcion());
        assertThrows(ReglaNegocioException.class, () -> productos.crear(new ProductoCmd(" ", 5L, 3L, null)));
        assertThrows(ReglaNegocioException.class, () -> productos.crear(new ProductoCmd("x", null, 3L, null)));
        assertThrows(ReglaNegocioException.class, () -> productos.crear(new ProductoCmd("x", 5L, null, null)));
        when(unidadMedidaDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> productos.crear(new ProductoCmd("x", 99L, 3L, null)));
    }

    @Test
    void productoSeModificaYConLotesNoSeElimina() {
        assertEquals("Leche entera", productos.modificar(6L, new ProductoCmd("Leche entera", 5L, 2L, null)).getNombre());
        verify(productoDAO).modificar(producto);

        when(loteDAO.existePorReferencia("producto", producto)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> productos.eliminar(6L));
        verify(productoDAO, never()).eliminar(any());
        when(loteDAO.existePorReferencia("producto", producto)).thenReturn(false);
        productos.eliminar(6L);
        verify(productoDAO).eliminar(6L);
    }

    @Test
    void productoConsultas() {
        when(productoDAO.findRange(0, 5)).thenReturn(List.of(producto));

        assertSame(producto, productos.buscarPorId(6L));
        assertEquals(List.of(producto), productos.listar(0, 5));
    }

    // ---------------------------------------------------------------------------------------- lotes

    @Test
    void loteRequiereProductoExistenteYNumeroDeLote() {
        Lote l = lotes.crear(new LoteCmd(6L, " L-9 ", LocalDate.of(2027, 1, 1)));

        verify(loteDAO).crear(l);
        assertEquals("L-9", l.getNumLote());
        assertSame(producto, l.getProducto());
        assertEquals(LocalDate.of(2027, 1, 1), l.getFechCaducidad());
        assertThrows(ReglaNegocioException.class, () -> lotes.crear(new LoteCmd(6L, " ", null)));
        assertThrows(ReglaNegocioException.class, () -> lotes.crear(new LoteCmd(null, "L", null)));
        when(productoDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> lotes.crear(new LoteCmd(99L, "L", null)));
    }

    @Test
    void loteNoCambiaDeProductoAlModificar() {
        Lote l = lotes.modificar(7L, new LoteCmd(null, "L-2", null));
        assertEquals("L-2", l.getNumLote());
        lotes.modificar(7L, new LoteCmd(6L, "L-3", null));
        verify(loteDAO, org.mockito.Mockito.times(2)).modificar(lote);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> lotes.modificar(7L, new LoteCmd(77L, "L-4", null)));
        assertTrue(ex.getMessage().contains("producto"), ex.getMessage());
    }

    @Test
    void loteConExistenciasComprasVentasOKardexNoSeElimina() {
        when(existenciasDAO.existePorReferencia("lote", lote)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> lotes.eliminar(7L));
        when(existenciasDAO.existePorReferencia("lote", lote)).thenReturn(false);
        when(compraDetalleDAO.existePorReferencia("lote", lote)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> lotes.eliminar(7L));
        when(compraDetalleDAO.existePorReferencia("lote", lote)).thenReturn(false);
        when(ventaDetalleDAO.existePorReferencia("lote", lote)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> lotes.eliminar(7L));
        when(ventaDetalleDAO.existePorReferencia("lote", lote)).thenReturn(false);
        when(kardexDetalleDAO.existePorReferencia("lote", lote)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> lotes.eliminar(7L));
        verify(loteDAO, never()).eliminar(any());

        when(kardexDetalleDAO.existePorReferencia("lote", lote)).thenReturn(false);
        lotes.eliminar(7L);
        verify(loteDAO).eliminar(7L);
    }

    @Test
    void loteConsultas() {
        when(loteDAO.findByProducto(6L)).thenReturn(List.of(lote));

        assertSame(lote, lotes.buscarPorId(7L));
        assertEquals(List.of(lote), lotes.porProducto(6L));
        when(loteDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> lotes.buscarPorId(99L));
    }

    // -------------------------------------------------------------------------------------- almacenes

    @Test
    void almacenSeCreaHabilitadoPorDefecto() {
        Almacen a = almacenes.crear(new AlmacenCmd(" Sucursal ", " ", null));

        verify(almacenDAO).crear(a);
        assertEquals("Sucursal", a.getNombre());
        assertTrue(a.getEstado());
        assertNull(a.getDescripcion());
        assertFalse(almacenes.crear(new AlmacenCmd("Cerrado", null, false)).getEstado());
        assertThrows(ReglaNegocioException.class, () -> almacenes.crear(new AlmacenCmd(" ", null, true)));
    }

    @Test
    void almacenSeModificaYSeHabilitaODeshabilita() {
        assertEquals("Matriz", almacenes.modificar(8L, new AlmacenCmd("Matriz", null, null)).getNombre());
        assertTrue(almacen.getEstado()); // estado nulo conserva el actual
        assertFalse(almacenes.cambiarEstado(8L, false).getEstado());
        assertTrue(almacenes.cambiarEstado(8L, true).getEstado());
        assertThrows(ReglaNegocioException.class, () -> almacenes.cambiarEstado(8L, null));
    }

    @Test
    void almacenConExistenciasMovimientosODetallesNoSeElimina() {
        when(existenciasDAO.existePorReferencia("almacen", almacen)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> almacenes.eliminar(8L));
        when(existenciasDAO.existePorReferencia("almacen", almacen)).thenReturn(false);
        when(compraDetalleDAO.existePorReferencia("almacen", almacen)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> almacenes.eliminar(8L));
        when(compraDetalleDAO.existePorReferencia("almacen", almacen)).thenReturn(false);
        when(ventaDetalleDAO.existePorReferencia("almacen", almacen)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> almacenes.eliminar(8L));
        when(ventaDetalleDAO.existePorReferencia("almacen", almacen)).thenReturn(false);
        when(kardexDAO.existePorReferencia("origen", almacen)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> almacenes.eliminar(8L));
        when(kardexDAO.existePorReferencia("origen", almacen)).thenReturn(false);
        when(kardexDAO.existePorReferencia("destino", almacen)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> almacenes.eliminar(8L));
        verify(almacenDAO, never()).eliminar(any());

        when(kardexDAO.existePorReferencia("destino", almacen)).thenReturn(false);
        almacenes.eliminar(8L);
        verify(almacenDAO).eliminar(8L);
    }

    @Test
    void almacenConsultas() {
        when(almacenDAO.findActivos()).thenReturn(List.of(almacen));
        when(almacenDAO.findRange(0, 5)).thenReturn(List.of(almacen));

        assertSame(almacen, almacenes.buscarPorId(8L));
        assertEquals(List.of(almacen), almacenes.activos());
        assertEquals(List.of(almacen), almacenes.listar(0, 5));
    }
}
