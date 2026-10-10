package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.ClienteDAO;
import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.CuentaDAO;
import com.sistemacontable.control.DepartamentoDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.ExistenciasDAO;
import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.KardexDetalleDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PlanillaDAO;
import com.sistemacontable.control.PlanillaDetalleDAO;
import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.ProveedorDAO;
import com.sistemacontable.control.TerceroDAO;
import com.sistemacontable.control.TipoProductoDAO;
import com.sistemacontable.control.UnidadMedidaDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.VentaDetalleDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.Existencias;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.PlanillaDetalle;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Lecturas que la capa REST necesita de los servicios: {@code contar()} (cabecera Total-Records) y las consultas de
 * compras, ventas, kardex, existencias, partidas y lotes. Los recursos solo hablan con servicios, nunca con DAOs.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultasServiciosTest {

    @Mock
    DepartamentoDAO departamentoDAO;
    @Mock
    EmpleadoDAO empleadoDAO;
    @Mock
    CuentaDAO cuentaDAO;
    @Mock
    TerceroDAO terceroDAO;
    @Mock
    ClienteDAO clienteDAO;
    @Mock
    ProveedorDAO proveedorDAO;
    @Mock
    UnidadMedidaDAO unidadMedidaDAO;
    @Mock
    TipoProductoDAO tipoProductoDAO;
    @Mock
    ProductoDAO productoDAO;
    @Mock
    AlmacenDAO almacenDAO;
    @Mock
    PlanillaDAO planillaDAO;
    @Mock
    PlanillaDetalleDAO planillaDetalleDAO;
    @Mock
    PartidaDAO partidaDAO;
    @Mock
    KardexDAO kardexDAO;
    @Mock
    KardexDetalleDAO kardexDetalleDAO;
    @Mock
    ExistenciasDAO existenciasDAO;
    @Mock
    LoteDAO loteDAO;
    @Mock
    CompraDAO compraDAO;
    @Mock
    CompraDetalleDAO compraDetalleDAO;
    @Mock
    VentaDAO ventaDAO;
    @Mock
    VentaDetalleDAO ventaDetalleDAO;

    @InjectMocks
    DepartamentoService departamentos;
    @InjectMocks
    EmpleadoService empleados;
    @InjectMocks
    CuentaService cuentas;
    @InjectMocks
    TerceroService terceros;
    @InjectMocks
    ClienteService clientes;
    @InjectMocks
    ProveedorService proveedores;
    @InjectMocks
    UnidadMedidaService unidades;
    @InjectMocks
    TipoProductoService tipos;
    @InjectMocks
    ProductoService productos;
    @InjectMocks
    AlmacenService almacenes;
    @InjectMocks
    PlanillaService planillas;
    @InjectMocks
    PartidaService partidas;
    @InjectMocks
    KardexService kardex;
    @InjectMocks
    ExistenciasService existencias;
    @InjectMocks
    LoteService lotes;
    @InjectMocks
    CompraService compras;
    @InjectMocks
    VentaService ventas;

    // ------------------------------------------------------------------------------------- contar()

    @Test
    void contarDevuelveElTotalDelDaoComoLongEnTodosLosServicios() {
        // el DAO (no el servicio) es quien cuenta; con mas de Integer.MAX_VALUE filas no debe truncarse
        long total = 5_000_000_000L;
        when(departamentoDAO.count()).thenReturn(total);
        when(empleadoDAO.count()).thenReturn(total);
        when(cuentaDAO.count()).thenReturn(total);
        when(terceroDAO.count()).thenReturn(total);
        when(clienteDAO.count()).thenReturn(total);
        when(proveedorDAO.count()).thenReturn(total);
        when(unidadMedidaDAO.count()).thenReturn(total);
        when(tipoProductoDAO.count()).thenReturn(total);
        when(productoDAO.count()).thenReturn(total);
        when(almacenDAO.count()).thenReturn(total);
        when(planillaDAO.count()).thenReturn(total);
        when(partidaDAO.count()).thenReturn(total);
        when(kardexDAO.count()).thenReturn(total);
        when(existenciasDAO.count()).thenReturn(total);
        when(loteDAO.count()).thenReturn(total);
        when(compraDAO.count()).thenReturn(total);
        when(ventaDAO.count()).thenReturn(total);

        Map<String, Supplier<Long>> servicios = new LinkedHashMap<>();
        servicios.put("departamentos", departamentos::contar);
        servicios.put("empleados", empleados::contar);
        servicios.put("cuentas", cuentas::contar);
        servicios.put("terceros", terceros::contar);
        servicios.put("clientes", clientes::contar);
        servicios.put("proveedores", proveedores::contar);
        servicios.put("unidades", unidades::contar);
        servicios.put("tipos de producto", tipos::contar);
        servicios.put("productos", productos::contar);
        servicios.put("almacenes", almacenes::contar);
        servicios.put("planillas", planillas::contar);
        servicios.put("partidas", partidas::contar);
        servicios.put("kardex", kardex::contar);
        servicios.put("existencias", existencias::contar);
        servicios.put("lotes", lotes::contar);
        servicios.put("compras", compras::contar);
        servicios.put("ventas", ventas::contar);

        servicios.forEach((nombre, servicio) -> assertEquals(total, servicio.get(), nombre));
    }

    // ------------------------------------------------------------------------------------- listar()

    @Test
    void listarDelegaLaPaginacionAlDaoEnPartidasKardexExistenciasLotesComprasYVentas() {
        List<Lote> filas = List.of(new Lote());
        when(loteDAO.findRange(50, 50)).thenReturn(filas);
        assertSame(filas, lotes.listar(50, 50));

        List<Compra> c = List.of(new Compra());
        when(compraDAO.findRange(0, 10)).thenReturn(c);
        assertSame(c, compras.listar(0, 10));

        List<Venta> v = List.of(new Venta());
        when(ventaDAO.findRange(10, 10)).thenReturn(v);
        assertSame(v, ventas.listar(10, 10));

        List<Kardex> k = List.of(new Kardex());
        when(kardexDAO.findRange(0, 100)).thenReturn(k);
        assertSame(k, kardex.listar(0, 100));

        List<Existencias> e = List.of(new Existencias());
        when(existenciasDAO.findRange(5, 5)).thenReturn(e);
        assertSame(e, existencias.listar(5, 5));
    }

    // -------------------------------------------------------------------------------- buscarPorId()

    @Test
    void buscarPorIdDevuelveLaEntidadOLanzaNoEncontrado() {
        Compra compra = new Compra();
        when(compraDAO.buscarPorId(1L)).thenReturn(Optional.of(compra));
        when(compraDAO.buscarPorId(2L)).thenReturn(Optional.empty());
        Venta venta = new Venta();
        when(ventaDAO.buscarPorId(1L)).thenReturn(Optional.of(venta));
        when(ventaDAO.buscarPorId(2L)).thenReturn(Optional.empty());
        Kardex mov = new Kardex();
        when(kardexDAO.buscarPorId(1L)).thenReturn(Optional.of(mov));
        when(kardexDAO.buscarPorId(2L)).thenReturn(Optional.empty());
        Existencias stock = new Existencias();
        when(existenciasDAO.buscarPorId(1L)).thenReturn(Optional.of(stock));
        when(existenciasDAO.buscarPorId(2L)).thenReturn(Optional.empty());

        assertSame(compra, compras.buscarPorId(1L));
        assertSame(venta, ventas.buscarPorId(1L));
        assertSame(mov, kardex.buscarPorId(1L));
        assertSame(stock, existencias.buscarPorId(1L));
        assertThrows(NoEncontradoException.class, () -> compras.buscarPorId(2L));
        assertThrows(NoEncontradoException.class, () -> ventas.buscarPorId(2L));
        assertThrows(NoEncontradoException.class, () -> kardex.buscarPorId(2L));
        assertThrows(NoEncontradoException.class, () -> existencias.buscarPorId(2L));
    }

    // ----------------------------------------------------------------------------------- detalles()

    @Test
    void losDetallesDeUnDocumentoSolosSeConsultanSiExiste() {
        when(compraDAO.buscarPorId(1L)).thenReturn(Optional.of(new Compra()));
        when(compraDAO.buscarPorId(9L)).thenReturn(Optional.empty());
        List<CompraDetalle> lineasCompra = List.of(new CompraDetalle());
        when(compraDetalleDAO.buscarPorCompra(1L)).thenReturn(lineasCompra);
        when(ventaDAO.buscarPorId(1L)).thenReturn(Optional.of(new Venta()));
        when(ventaDAO.buscarPorId(9L)).thenReturn(Optional.empty());
        List<VentaDetalle> lineasVenta = List.of(new VentaDetalle());
        when(ventaDetalleDAO.buscarPorVenta(1L)).thenReturn(lineasVenta);
        when(kardexDAO.buscarPorId(1L)).thenReturn(Optional.of(new Kardex()));
        when(kardexDAO.buscarPorId(9L)).thenReturn(Optional.empty());
        List<KardexDetalle> lineasKardex = List.of(new KardexDetalle());
        when(kardexDetalleDAO.buscarPorKardex(1L)).thenReturn(lineasKardex);

        assertSame(lineasCompra, compras.detalles(1L));
        assertSame(lineasVenta, ventas.detalles(1L));
        assertSame(lineasKardex, kardex.detalles(1L));
        assertThrows(NoEncontradoException.class, () -> compras.detalles(9L));
        assertThrows(NoEncontradoException.class, () -> ventas.detalles(9L));
        assertThrows(NoEncontradoException.class, () -> kardex.detalles(9L));
        verify(compraDetalleDAO, never()).buscarPorCompra(9L);
        verify(ventaDetalleDAO, never()).buscarPorVenta(9L);
        verify(kardexDetalleDAO, never()).buscarPorKardex(9L);
    }

    @Test
    void partidasListaPaginadoPorElDao() {
        List<com.sistemacontable.entity.Partida> filas = List.of(new com.sistemacontable.entity.Partida());
        when(partidaDAO.findRange(0, 50)).thenReturn(filas);

        assertSame(filas, partidas.listar(0, 50));
    }

    @Test
    void buscarDetalleDevuelveElDetalleOLanzaNoEncontrado() {
        PlanillaDetalle detalle = new PlanillaDetalle();
        when(planillaDetalleDAO.buscarPorId(1L)).thenReturn(Optional.of(detalle));
        when(planillaDetalleDAO.buscarPorId(2L)).thenReturn(Optional.empty());

        assertSame(detalle, planillas.buscarDetalle(1L));
        assertThrows(NoEncontradoException.class, () -> planillas.buscarDetalle(2L));
    }
}
