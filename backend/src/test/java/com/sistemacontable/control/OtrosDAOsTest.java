package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.PlanillaDetalle;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.TipoProducto;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtrosDAOsTest {

    private SimulacionJpa jpa;

    @BeforeEach
    void preparar() {
        jpa = new SimulacionJpa();
    }

    // ---------------------------------------------------------------- EmpleadoDAO

    @Test
    void findActivosFiltraPorEstadoActivo() {
        Empleado e = new Empleado();
        TypedQuery<Empleado> q = jpa.lista(Empleado.class, List.of(e));

        assertEquals(List.of(e), jpa.inyectar(new EmpleadoDAO()).findActivos());

        assertTrue(jpa.jpql(Empleado.class).contains("e.estado = :estado"));
        verify(q).setParameter("estado", EstadoEmpleado.Activo);
    }

    @Test
    void findByDuiYFindByNumeroIsssBuscanPorSuColumna() {
        Empleado e = new Empleado();
        TypedQuery<Empleado> q = jpa.lista(Empleado.class, List.of(e));
        EmpleadoDAO dao = jpa.inyectar(new EmpleadoDAO());

        assertEquals(Optional.of(e), dao.findByDui("00000000-1"));
        verify(q).setParameter("dui", "00000000-1");
        assertEquals(Optional.of(e), dao.findByNumeroIsss("123456789"));
        verify(q).setParameter("numeroIsss", "123456789");
    }

    @Test
    void findByDuiYFindByNumeroIsssRechazanVaciosYNulos() {
        EmpleadoDAO dao = jpa.inyectar(new EmpleadoDAO());

        assertThrows(IllegalArgumentException.class, () -> dao.findByDui(null));
        assertThrows(IllegalArgumentException.class, () -> dao.findByDui("  "));
        assertThrows(IllegalArgumentException.class, () -> dao.findByNumeroIsss(null));
        assertThrows(IllegalArgumentException.class, () -> dao.findByNumeroIsss(""));
    }

    // ---------------------------------------------------------------- PlanillaDetalleDAO

    @Test
    void buscarPorPlanillaFiltraPorIdDePlanilla() {
        PlanillaDetalle d = new PlanillaDetalle();
        TypedQuery<PlanillaDetalle> q = jpa.lista(PlanillaDetalle.class, List.of(d));

        assertEquals(List.of(d), jpa.inyectar(new PlanillaDetalleDAO()).buscarPorPlanilla(2L));

        assertTrue(jpa.jpql(PlanillaDetalle.class).contains("d.planilla.id = :planillaId"));
        verify(q).setParameter("planillaId", 2L);
    }

    @Test
    void existePorPlanillaYEmpleadoCuentaFilasPorAmbosIds() {
        TypedQuery<Long> q = jpa.unico(Long.class, 1L);

        assertTrue(jpa.inyectar(new PlanillaDetalleDAO()).existePorPlanillaYEmpleado(2L, 5L));

        String jpql = jpa.jpql(Long.class);
        assertTrue(jpql.contains("d.planilla.id = :planillaId") && jpql.contains("d.empleado.id = :empleadoId"), jpql);
        verify(q).setParameter("planillaId", 2L);
        verify(q).setParameter("empleadoId", 5L);
    }

    @Test
    void existePorPlanillaYEmpleadoEsFalsoSinFilas() {
        jpa.unico(Long.class, 0L);

        assertFalse(jpa.inyectar(new PlanillaDetalleDAO()).existePorPlanillaYEmpleado(2L, 5L));
    }

    @Test
    void metodosDePlanillaDetalleRechazanNulos() {
        PlanillaDetalleDAO dao = jpa.inyectar(new PlanillaDetalleDAO());

        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorPlanilla(null));
        assertThrows(IllegalArgumentException.class, () -> dao.existePorPlanillaYEmpleado(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> dao.existePorPlanillaYEmpleado(1L, null));
    }

    // ---------------------------------------------------------------- TipoProductoDAO

    @Test
    void findRaicesBuscaLosQueNoTienenPadre() {
        TipoProducto t = new TipoProducto();
        jpa.lista(TipoProducto.class, List.of(t));

        assertEquals(List.of(t), jpa.inyectar(new TipoProductoDAO()).findRaices());

        assertTrue(jpa.jpql(TipoProducto.class).contains("t.padre IS NULL"));
    }

    @Test
    void findHijosFiltraPorPadre() {
        TipoProducto padre = new TipoProducto();
        TypedQuery<TipoProducto> q = jpa.lista(TipoProducto.class, List.of());
        TipoProductoDAO dao = jpa.inyectar(new TipoProductoDAO());

        dao.findHijos(padre);

        assertTrue(jpa.jpql(TipoProducto.class).contains("t.padre = :padre"));
        verify(q).setParameter("padre", padre);
        assertThrows(IllegalArgumentException.class, () -> dao.findHijos(null));
    }

    // ---------------------------------------------------------------- Kardex, Lote, Almacen, NIT

    @Test
    void findByAlmacenIncluyeMovimientosDeOrigenYDestino() {
        Kardex k = new Kardex();
        TypedQuery<Kardex> q = jpa.lista(Kardex.class, List.of(k));
        KardexDAO dao = jpa.inyectar(new KardexDAO());

        assertEquals(List.of(k), dao.findByAlmacen(6L));

        String jpql = jpa.jpql(Kardex.class);
        // LEFT JOIN: un inner join sobre origen descartaria las entradas (origen nulo) y viceversa
        assertTrue(jpql.contains("LEFT JOIN k.origen") && jpql.contains("LEFT JOIN k.destino"), jpql);
        assertTrue(jpql.contains(" OR "), jpql);
        verify(q).setParameter("almacenId", 6L);
        assertThrows(IllegalArgumentException.class, () -> dao.findByAlmacen(null));
    }

    @Test
    void buscarPorKardexFiltraPorIdDeKardex() {
        KardexDetalle d = new KardexDetalle();
        TypedQuery<KardexDetalle> q = jpa.lista(KardexDetalle.class, List.of(d));

        assertEquals(List.of(d), jpa.inyectar(new KardexDetalleDAO()).buscarPorKardex(1L));

        assertTrue(jpa.jpql(KardexDetalle.class).contains("d.kardex.id = :kardexId"));
        verify(q).setParameter("kardexId", 1L);
    }

    @Test
    void findByProductoFiltraLotesPorIdDeProducto() {
        Lote l = new Lote();
        TypedQuery<Lote> q = jpa.lista(Lote.class, List.of(l));

        assertEquals(List.of(l), jpa.inyectar(new LoteDAO()).findByProducto(4L));

        assertTrue(jpa.jpql(Lote.class).contains("l.producto.id = :productoId"));
        verify(q).setParameter("productoId", 4L);
    }

    @Test
    void almacenFindActivosFiltraPorEstadoVerdadero() {
        Almacen a = new Almacen();
        jpa.lista(Almacen.class, List.of(a));

        assertEquals(List.of(a), jpa.inyectar(new AlmacenDAO()).findActivos());

        assertTrue(jpa.jpql(Almacen.class).contains("a.estado = true"));
    }

    @Test
    void findByNitEnClienteYProveedor() {
        Cliente c = new Cliente();
        TypedQuery<Cliente> qc = jpa.lista(Cliente.class, List.of(c));
        assertEquals(Optional.of(c), jpa.inyectar(new ClienteDAO()).findByNit("0614-1"));
        verify(qc).setParameter("nit", "0614-1");

        Proveedor p = new Proveedor();
        TypedQuery<Proveedor> qp = jpa.lista(Proveedor.class, List.of(p));
        assertEquals(Optional.of(p), jpa.inyectar(new ProveedorDAO()).findByNit("0614-2"));
        verify(qp).setParameter("nit", "0614-2");

        assertThrows(IllegalArgumentException.class, () -> jpa.inyectar(new ClienteDAO()).findByNit(" "));
        assertThrows(IllegalArgumentException.class, () -> jpa.inyectar(new ProveedorDAO()).findByNit(null));
    }
}
