package com.sistemacontable.control;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.JpaTestSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;

/**
 * Ayuda para pruebas unitarias de DAO: un EntityManager simulado cuyas consultas tipadas devuelven resultados
 * predefinidos y permiten comprobar la JPQL y los parametros usados.
 */
final class SimulacionJpa {

    final EntityManager em = mock(EntityManager.class);

    /** Registra una consulta tipada que devuelve {@code resultados} con getResultList. */
    <R> TypedQuery<R> lista(Class<R> tipo, List<R> resultados) {
        TypedQuery<R> q = nueva(tipo);
        when(q.getResultList()).thenReturn(resultados);
        return q;
    }

    /** Registra una consulta tipada que devuelve {@code unico} con getSingleResult. */
    <R> TypedQuery<R> unico(Class<R> tipo, R unico) {
        TypedQuery<R> q = nueva(tipo);
        when(q.getSingleResult()).thenReturn(unico);
        return q;
    }

    @SuppressWarnings("unchecked")
    private <R> TypedQuery<R> nueva(Class<R> tipo) {
        TypedQuery<R> q = mock(TypedQuery.class, Answers.RETURNS_SELF);
        when(em.createQuery(anyString(), eq(tipo))).thenReturn(q);
        return q;
    }

    /** JPQL con la que se creo la consulta de ese tipo de resultado. */
    String jpql(Class<?> tipo) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(em).createQuery(captor.capture(), eq(tipo));
        return captor.getValue();
    }

    <D> D inyectar(D dao) {
        JpaTestSupport.inyectarEm(dao, em);
        return dao;
    }
}
