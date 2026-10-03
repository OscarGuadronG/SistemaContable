package com.sistemacontable.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Prueba de mapeo real con EclipseLink: persiste un grafo completo y lo lee de vuelta (todo con rollback). */
class MapeoJpaTest {

    private EntityManager em;

    @BeforeEach
    void abrir() {
        em = JpaTestSupport.nuevoEmConTransaccion();
    }

    @AfterEach
    void cerrar() {
        JpaTestSupport.cerrarConRollback(em);
    }

    @Test
    void persisteYLeeElGrafoDeInventarioYContabilidad() {
        Departamento dep = new Departamento();
        dep.setNombre("Ventas");
        em.persist(dep);

        Empleado emp = new Empleado();
        emp.setNombre("Ana");
        emp.setApellido("Lopez");
        emp.setDepartamento(dep);
        emp.setNumeroIsss("123");
        emp.setDui("00000000-1");
        em.persist(emp);

        UnidadMedida um = new UnidadMedida();
        um.setNombre("Unidad");
        um.setAbrev("u");
        em.persist(um);
        TipoProducto raiz = new TipoProducto();
        raiz.setNombre("General");
        em.persist(raiz);
        TipoProducto hijo = new TipoProducto();
        hijo.setNombre("Bebidas");
        hijo.setPadre(raiz);
        em.persist(hijo);
        Producto prod = new Producto();
        prod.setNombre("Agua");
        prod.setUnidadMedida(um);
        prod.setTipo(hijo);
        em.persist(prod);
        Lote lote = new Lote();
        lote.setProducto(prod);
        lote.setNumLote("L-1");
        em.persist(lote);
        Almacen alm = new Almacen();
        alm.setNombre("Central");
        alm.setEstado(true);
        em.persist(alm);

        Proveedor prov = new Proveedor();
        prov.setNombre("Prov");
        prov.setNit("0614-1");
        em.persist(prov);
        Compra compra = new Compra();
        compra.setProveedor(prov);
        compra.setEmpleado(emp);
        em.persist(compra);
        CompraDetalle cd = new CompraDetalle();
        cd.setCompra(compra);
        cd.setLote(lote);
        cd.setAlmacen(alm);
        cd.setCantidad(new BigDecimal("10.5"));
        cd.setPrecio(new BigDecimal("1234567.891"));
        em.persist(cd);

        Existencias ex = new Existencias();
        ex.setLote(lote);
        ex.setAlmacen(alm);
        ex.setCantidad(new BigDecimal("10.5"));
        em.persist(ex);

        Kardex k = new Kardex();
        k.setTipoMovimiento(TipoMovimiento.Entrada);
        k.setDestino(alm);
        k.setCompra(compra);
        em.persist(k);
        KardexDetalle kd = new KardexDetalle();
        kd.setKardex(k);
        kd.setLote(lote);
        kd.setCantidad(new BigDecimal("10.5"));
        kd.setPrecioCosto(new BigDecimal("1234567.891"));
        em.persist(kd);

        Cuenta activo = new Cuenta();
        activo.setCodigo(1);
        activo.setNombre("Activo");
        activo.setNaturaleza(NaturalezaCuenta.Deudora);
        em.persist(activo);
        Cuenta caja = new Cuenta();
        caja.setCodigo(11);
        caja.setNombre("Caja");
        caja.setNaturaleza(NaturalezaCuenta.Deudora);
        caja.setImputable(true);
        caja.setPadre(activo);
        em.persist(caja);
        Partida partida = new Partida();
        partida.setConcepto("Compra");
        partida.setTipo(TipoPartida.Diario);
        partida.setCompra(compra);
        em.persist(partida);
        PartidaDetalle pd = new PartidaDetalle();
        pd.setPartida(partida);
        pd.setCuenta(caja);
        pd.setCargo(new BigDecimal("100"));
        em.persist(pd);

        Planilla pl = new Planilla();
        OffsetDateTime ahora = OffsetDateTime.now();
        pl.setFechaInicio(ahora);
        pl.setFechaFin(ahora);
        pl.setFechaPago(ahora);
        em.persist(pl);
        PlanillaDetalle pld = new PlanillaDetalle();
        pld.setPlanilla(pl);
        pld.setEmpleado(emp);
        pld.setSueldoBase(new BigDecimal("500"));
        em.persist(pld);

        em.flush();
        em.clear();

        // Los ids los asigna la base de datos y las relaciones se leen de vuelta.
        assertNotNull(dep.getId());
        Empleado leido = em.find(Empleado.class, emp.getId());
        assertEquals(EstadoEmpleado.Activo, leido.getEstado());
        assertEquals("Ventas", leido.getDepartamento().getNombre());
        assertEquals(0, BigDecimal.ZERO.compareTo(leido.getSalario()));

        CompraDetalle cdLeido = em.find(CompraDetalle.class, cd.getId());
        assertEquals(0, new BigDecimal("1234567.891").compareTo(cdLeido.getPrecio()));
        assertEquals(LocalDate.now(), em.find(Compra.class, compra.getId()).getFecha());

        Kardex kLeido = em.find(Kardex.class, k.getId());
        assertEquals(TipoMovimiento.Entrada, kLeido.getTipoMovimiento());
        assertNull(kLeido.getOrigen());
        assertEquals("Central", kLeido.getDestino().getNombre());

        Cuenta cajaLeida = em.find(Cuenta.class, caja.getId());
        assertEquals("Caja", cajaLeida.getNombre());
        assertEquals(activo.getId(), cajaLeida.getPadre().getId());
        assertEquals(TipoPartida.Diario, em.find(Partida.class, partida.getId()).getTipo());
        assertEquals(raiz.getId(), em.find(TipoProducto.class, hijo.getId()).getPadre().getId());
        assertEquals((short) 30, em.find(PlanillaDetalle.class, pld.getId()).getDias());
    }
}
