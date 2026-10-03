package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.sistemacontable.entity.Cuenta;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CuentaDAOTest {

    private SimulacionJpa jpa;
    private CuentaDAO dao;

    @BeforeEach
    void preparar() {
        jpa = new SimulacionJpa();
        dao = jpa.inyectar(new CuentaDAO());
    }

    @Test
    void findByCodigoUsaElParametroYDevuelveElPrimero() {
        Cuenta caja = new Cuenta();
        TypedQuery<Cuenta> q = jpa.lista(Cuenta.class, List.of(caja));

        Optional<Cuenta> resultado = dao.findByCodigo(1101);

        assertEquals(Optional.of(caja), resultado);
        assertTrue(jpa.jpql(Cuenta.class).contains("c.codigo = :codigo"));
        verify(q).setParameter("codigo", 1101);
    }

    @Test
    void findByCodigoInexistenteDevuelveVacio() {
        jpa.lista(Cuenta.class, List.of());

        assertEquals(Optional.empty(), dao.findByCodigo(9));
    }

    @Test
    void findByCodigoNuloLanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> dao.findByCodigo(null));
    }

    @Test
    void findHijosFiltraPorPadreYOrdenaPorCodigo() {
        Cuenta padre = new Cuenta();
        Cuenta hijo = new Cuenta();
        TypedQuery<Cuenta> q = jpa.lista(Cuenta.class, List.of(hijo));

        assertEquals(List.of(hijo), dao.findHijos(padre));

        String jpql = jpa.jpql(Cuenta.class);
        assertTrue(jpql.contains("c.padre = :padre"), jpql);
        assertTrue(jpql.contains("ORDER BY c.codigo"), jpql);
        verify(q).setParameter("padre", padre);
    }

    @Test
    void findHijosConPadreNuloLanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> dao.findHijos(null));
    }

    @Test
    void existenHijosActivosCuentaSoloLosActivos() {
        Cuenta padre = new Cuenta();
        TypedQuery<Long> q = jpa.unico(Long.class, 2L);

        assertTrue(dao.existenHijosActivos(padre));

        String jpql = jpa.jpql(Long.class);
        assertTrue(jpql.contains("c.padre = :padre") && jpql.contains("c.activo = true"), jpql);
        verify(q).setParameter("padre", padre);
    }

    @Test
    void existenHijosActivosEsFalsoSinHijos() {
        jpa.unico(Long.class, 0L);

        assertFalse(dao.existenHijosActivos(new Cuenta()));
    }

    @Test
    void tieneMovimientosConsultaLasLineasDePartida() {
        Cuenta cuenta = new Cuenta();
        TypedQuery<Long> q = jpa.unico(Long.class, 1L);

        assertTrue(dao.tieneMovimientos(cuenta));

        String jpql = jpa.jpql(Long.class);
        assertTrue(jpql.contains("FROM PartidaDetalle d WHERE d.cuenta = :cuenta"), jpql);
        verify(q).setParameter("cuenta", cuenta);
    }

    @Test
    void tieneMovimientosEsFalsoSinLineas() {
        jpa.unico(Long.class, 0L);

        assertFalse(dao.tieneMovimientos(new Cuenta()));
    }

    @Test
    void existenHijosActivosYTieneMovimientosRechazanNulos() {
        assertThrows(IllegalArgumentException.class, () -> dao.existenHijosActivos(null));
        assertThrows(IllegalArgumentException.class, () -> dao.tieneMovimientos(null));
    }
}
