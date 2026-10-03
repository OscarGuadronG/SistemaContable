package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.sistemacontable.entity.Existencias;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExistenciasDAOTest {

    private SimulacionJpa jpa;
    private ExistenciasDAO dao;

    @BeforeEach
    void preparar() {
        jpa = new SimulacionJpa();
        dao = jpa.inyectar(new ExistenciasDAO());
    }

    @Test
    void findForUpdateBloqueaConPessimisticWriteYFiltraPorLoteYAlmacen() {
        Existencias fila = new Existencias();
        TypedQuery<Existencias> q = jpa.lista(Existencias.class, List.of(fila));

        Optional<Existencias> resultado = dao.findForUpdate(5L, 7L);

        assertEquals(Optional.of(fila), resultado);
        String jpql = jpa.jpql(Existencias.class);
        assertTrue(jpql.contains("x.lote.id = :loteId") && jpql.contains("x.almacen.id = :almacenId"), jpql);
        verify(q).setParameter("loteId", 5L);
        verify(q).setParameter("almacenId", 7L);
        verify(q).setLockMode(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void findForUpdateSinFilaDevuelveVacio() {
        jpa.lista(Existencias.class, List.of());

        assertEquals(Optional.empty(), dao.findForUpdate(1L, 1L));
    }

    @Test
    void findForUpdateConFilasDuplicadasFallaPorqueNoHayUniqueEnLaBd() {
        jpa.lista(Existencias.class, List.of(new Existencias(), new Existencias()));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> dao.findForUpdate(1L, 1L));
        assertTrue(ex.getMessage().contains("duplicad"), ex.getMessage());
    }

    @Test
    void findForUpdateRechazaArgumentosNulos() {
        assertThrows(IllegalArgumentException.class, () -> dao.findForUpdate(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> dao.findForUpdate(1L, null));
    }

    @Test
    void stockTotalPorLoteSumaLasCantidades() {
        TypedQuery<BigDecimal> q = jpa.unico(BigDecimal.class, new BigDecimal("12.5"));

        assertEquals(0, new BigDecimal("12.5").compareTo(dao.stockTotalPorLote(3L)));
        assertTrue(jpa.jpql(BigDecimal.class).contains("SUM(x.cantidad)"));
        verify(q).setParameter("loteId", 3L);
    }

    @Test
    void stockTotalPorLoteSinFilasEsCeroYNoNulo() {
        jpa.unico(BigDecimal.class, null);

        assertEquals(BigDecimal.ZERO, dao.stockTotalPorLote(3L));
    }

    @Test
    void stockTotalPorLoteRechazaLoteNulo() {
        assertThrows(IllegalArgumentException.class, () -> dao.stockTotalPorLote(null));
    }
}
