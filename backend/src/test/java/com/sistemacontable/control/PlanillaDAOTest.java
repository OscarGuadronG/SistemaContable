package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.sistemacontable.entity.Planilla;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlanillaDAOTest {

    private SimulacionJpa jpa;
    private PlanillaDAO dao;

    @BeforeEach
    void preparar() {
        jpa = new SimulacionJpa();
        dao = jpa.inyectar(new PlanillaDAO());
    }

    @Test
    void buscarParaActualizarBloqueaLaFilaConPessimisticWrite() {
        Planilla p = new Planilla();
        when(jpa.em.find(Planilla.class, 3L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(p);

        assertEquals(Optional.of(p), dao.buscarParaActualizar(3L));
    }

    @Test
    void buscarParaActualizarInexistenteDevuelveVacio() {
        when(jpa.em.find(Planilla.class, 4L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(null);

        assertEquals(Optional.empty(), dao.buscarParaActualizar(4L));
    }

    @Test
    void buscarParaActualizarRechazaIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> dao.buscarParaActualizar(null));
    }
}
