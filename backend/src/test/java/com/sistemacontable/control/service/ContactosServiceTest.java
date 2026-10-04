package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.ClienteDAO;
import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.ProveedorDAO;
import com.sistemacontable.control.TerceroDAO;
import com.sistemacontable.control.UnidadMedidaDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.ClienteCmd;
import com.sistemacontable.control.service.cmd.ProveedorCmd;
import com.sistemacontable.control.service.cmd.TerceroCmd;
import com.sistemacontable.control.service.cmd.UnidadMedidaCmd;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.Tercero;
import com.sistemacontable.entity.UnidadMedida;
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

/** Clientes, proveedores, terceros y unidades de medida: servicios delgados con unicidad y borrado seguro. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContactosServiceTest {

    @Mock
    ClienteDAO clienteDAO;
    @Mock
    ProveedorDAO proveedorDAO;
    @Mock
    TerceroDAO terceroDAO;
    @Mock
    UnidadMedidaDAO unidadMedidaDAO;
    @Mock
    VentaDAO ventaDAO;
    @Mock
    CompraDAO compraDAO;
    @Mock
    PartidaDAO partidaDAO;
    @Mock
    ProductoDAO productoDAO;
    @InjectMocks
    ClienteService clientes;
    @InjectMocks
    ProveedorService proveedores;
    @InjectMocks
    TerceroService terceros;
    @InjectMocks
    UnidadMedidaService unidades;

    private final Cliente cliente = cliente();
    private final Proveedor proveedor = proveedor();
    private final Tercero tercero = tercero();
    private final UnidadMedida unidad = unidad();

    private static Cliente cliente() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNit("0614-1");
        return c;
    }

    private static Proveedor proveedor() {
        Proveedor p = new Proveedor();
        p.setId(2L);
        p.setNit("0614-2");
        return p;
    }

    private static Tercero tercero() {
        Tercero t = new Tercero();
        t.setId(3L);
        return t;
    }

    private static UnidadMedida unidad() {
        UnidadMedida u = new UnidadMedida();
        u.setId(4L);
        return u;
    }

    @BeforeEach
    void preparar() {
        when(clienteDAO.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(clienteDAO.findByNit("0614-1")).thenReturn(Optional.of(cliente));
        when(proveedorDAO.buscarPorId(2L)).thenReturn(Optional.of(proveedor));
        when(proveedorDAO.findByNit("0614-2")).thenReturn(Optional.of(proveedor));
        when(terceroDAO.buscarPorId(3L)).thenReturn(Optional.of(tercero));
        when(unidadMedidaDAO.buscarPorId(4L)).thenReturn(Optional.of(unidad));
    }

    // ----------------------------------------------------------------------------------- clientes

    @Test
    void clienteSeCreaConNitNormalizado() {
        Cliente c = clientes.crear(new ClienteCmd(" Tienda ", " ", " 0614-9 "));

        verify(clienteDAO).crear(c);
        assertEquals("Tienda", c.getNombre());
        assertEquals("0614-9", c.getNit());
        assertNull(c.getDescripcion());
    }

    @Test
    void clienteConNitRepetidoOCamposVaciosSeRechaza() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> clientes.crear(new ClienteCmd("Otra", null, "0614-1")));
        assertTrue(ex.getMessage().contains("NIT") && ex.getMessage().contains("0614-1"), ex.getMessage());
        assertThrows(ReglaNegocioException.class, () -> clientes.crear(new ClienteCmd(" ", null, "1")));
        assertThrows(ReglaNegocioException.class, () -> clientes.crear(new ClienteCmd("x", null, " ")));
        assertThrows(ReglaNegocioException.class, () -> clientes.crear(null));
        verify(clienteDAO, never()).crear(any());
    }

    @Test
    void clienteSeModificaConservandoSuNitPeroNoTomandoElDeOtro() {
        assertEquals("Nuevo", clientes.modificar(1L, new ClienteCmd("Nuevo", null, "0614-1")).getNombre());
        verify(clienteDAO).modificar(cliente);

        Cliente otro = new Cliente();
        otro.setId(9L);
        when(clienteDAO.findByNit("0614-7")).thenReturn(Optional.of(otro));
        assertThrows(ReglaNegocioException.class, () -> clientes.modificar(1L, new ClienteCmd("x", null, "0614-7")));
    }

    @Test
    void clienteConVentasNoSeElimina() {
        when(ventaDAO.existePorReferencia("cliente", cliente)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> clientes.eliminar(1L));
        verify(clienteDAO, never()).eliminar(any());

        when(ventaDAO.existePorReferencia("cliente", cliente)).thenReturn(false);
        clientes.eliminar(1L);
        verify(clienteDAO).eliminar(1L);
    }

    @Test
    void clienteConsultas() {
        when(clienteDAO.findRange(0, 5)).thenReturn(List.of(cliente));
        when(clienteDAO.findByNit("x")).thenReturn(Optional.empty());

        assertSame(cliente, clientes.buscarPorId(1L));
        assertSame(cliente, clientes.buscarPorNit("0614-1"));
        assertEquals(List.of(cliente), clientes.listar(0, 5));
        assertThrows(NoEncontradoException.class, () -> clientes.buscarPorNit("x"));
        when(clienteDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> clientes.buscarPorId(99L));
    }

    // -------------------------------------------------------------------------------- proveedores

    @Test
    void proveedorSeCreaYValidaNitUnico() {
        Proveedor p = proveedores.crear(new ProveedorCmd("Prov", "desc", "0614-8"));

        verify(proveedorDAO).crear(p);
        assertEquals("desc", p.getDescripcion());
        assertThrows(ReglaNegocioException.class, () -> proveedores.crear(new ProveedorCmd("Otro", null, "0614-2")));
        assertThrows(ReglaNegocioException.class, () -> proveedores.crear(new ProveedorCmd("", null, "1")));
        assertThrows(ReglaNegocioException.class, () -> proveedores.crear(new ProveedorCmd("x", null, null)));
    }

    @Test
    void proveedorSeModificaYConComprasNoSeElimina() {
        assertEquals("Nuevo", proveedores.modificar(2L, new ProveedorCmd("Nuevo", null, "0614-2")).getNombre());

        when(compraDAO.existePorReferencia("proveedor", proveedor)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> proveedores.eliminar(2L));
        verify(proveedorDAO, never()).eliminar(any());

        when(compraDAO.existePorReferencia("proveedor", proveedor)).thenReturn(false);
        proveedores.eliminar(2L);
        verify(proveedorDAO).eliminar(2L);
    }

    @Test
    void proveedorConsultas() {
        when(proveedorDAO.findRange(0, 5)).thenReturn(List.of(proveedor));

        assertSame(proveedor, proveedores.buscarPorId(2L));
        assertSame(proveedor, proveedores.buscarPorNit("0614-2"));
        assertEquals(List.of(proveedor), proveedores.listar(0, 5));
    }

    // ------------------------------------------------------------------------------------ terceros

    @Test
    void terceroSeCreaModificaYConPartidasNoSeElimina() {
        Tercero t = terceros.crear(new TerceroCmd(" Banco ", null));
        assertEquals("Banco", t.getNombre());
        assertThrows(ReglaNegocioException.class, () -> terceros.crear(new TerceroCmd(" ", null)));
        assertEquals("Nuevo", terceros.modificar(3L, new TerceroCmd("Nuevo", "d")).getNombre());

        when(partidaDAO.existePorReferencia("tercero", tercero)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> terceros.eliminar(3L));
        when(partidaDAO.existePorReferencia("tercero", tercero)).thenReturn(false);
        terceros.eliminar(3L);
        verify(terceroDAO).eliminar(3L);
    }

    @Test
    void terceroConsultas() {
        when(terceroDAO.findRange(0, 5)).thenReturn(List.of(tercero));

        assertSame(tercero, terceros.buscarPorId(3L));
        assertEquals(List.of(tercero), terceros.listar(0, 5));
    }

    // ----------------------------------------------------------------------- unidades de medida

    @Test
    void unidadDeMedidaRequiereNombreYAbreviatura() {
        UnidadMedida u = unidades.crear(new UnidadMedidaCmd(" Kilogramo ", " kg "));

        verify(unidadMedidaDAO).crear(u);
        assertEquals("Kilogramo", u.getNombre());
        assertEquals("kg", u.getAbrev());
        assertThrows(ReglaNegocioException.class, () -> unidades.crear(new UnidadMedidaCmd("x", " ")));
        assertThrows(ReglaNegocioException.class, () -> unidades.crear(new UnidadMedidaCmd(null, "x")));
        assertThrows(ReglaNegocioException.class, () -> unidades.crear(null));
    }

    @Test
    void unidadDeMedidaSeModificaYConProductosNoSeElimina() {
        assertEquals("Libra", unidades.modificar(4L, new UnidadMedidaCmd("Libra", "lb")).getNombre());

        when(productoDAO.existePorReferencia("unidadMedida", unidad)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> unidades.eliminar(4L));
        verify(unidadMedidaDAO, never()).eliminar(any());
        when(productoDAO.existePorReferencia("unidadMedida", unidad)).thenReturn(false);
        unidades.eliminar(4L);
        verify(unidadMedidaDAO).eliminar(4L);
    }

    @Test
    void unidadDeMedidaConsultas() {
        when(unidadMedidaDAO.findRange(0, 5)).thenReturn(List.of(unidad));

        assertSame(unidad, unidades.buscarPorId(4L));
        assertEquals(List.of(unidad), unidades.listar(0, 5));
    }
}
