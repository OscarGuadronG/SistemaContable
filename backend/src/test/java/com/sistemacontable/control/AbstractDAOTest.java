package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.entity.Departamento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbstractDAOTest {

    /** DAO concreto minimo para ejercitar la clase abstracta. */
    static class DepartamentoDaoPrueba extends AbstractDAO<Departamento, Long> {
        EntityManager em;

        DepartamentoDaoPrueba(EntityManager em) {
            super(Departamento.class);
            this.em = em;
        }

        @Override
        protected EntityManager getEntityManager() {
            return em;
        }
    }

    @Mock
    EntityManager em;
    @Mock
    EntityManagerFactory emf;
    @Mock
    PersistenceUnitUtil unitUtil;
    @Mock
    Metamodel metamodel;
    @Mock
    EntityType<Departamento> entityType;
    @Mock
    SingularAttribute<Departamento, Long> atributoId;
    @Mock
    SingularAttribute<Departamento, String> atributoNombre;
    @Mock(answer = Answers.RETURNS_SELF)
    TypedQuery<Departamento> consulta;
    @Mock(answer = Answers.RETURNS_SELF)
    TypedQuery<Long> consultaConteo;

    DepartamentoDaoPrueba dao;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void preparar() {
        dao = new DepartamentoDaoPrueba(em);
        when(em.getEntityManagerFactory()).thenReturn(emf);
        when(emf.getPersistenceUnitUtil()).thenReturn(unitUtil);
        when(em.getMetamodel()).thenReturn(metamodel);
        when(metamodel.entity(Departamento.class)).thenReturn(entityType);
        when(entityType.getName()).thenReturn("Departamento");
        // El atributo @Id se llama "codigo" a proposito: el DAO no debe asumir el literal "id".
        when(atributoId.isId()).thenReturn(true);
        when(atributoId.getName()).thenReturn("codigo");
        when(atributoNombre.isId()).thenReturn(false);
        when(atributoNombre.getName()).thenReturn("nombre");
        when(entityType.getSingularAttributes()).thenReturn((Set) Set.of(atributoNombre, atributoId));
    }

    // ---------------------------------------------------------------- constructor y em

    @Test
    void constructorRechazaClaseNula() {
        assertThrows(IllegalArgumentException.class, () -> new AbstractDAO<Departamento, Long>(null) {
            @Override
            protected EntityManager getEntityManager() {
                return em;
            }
        });
    }

    @Test
    void sinEntityManagerLanzaIllegalState() {
        DepartamentoDaoPrueba sinEm = new DepartamentoDaoPrueba(null);

        assertThrows(IllegalStateException.class, () -> sinEm.crear(new Departamento()));
        assertThrows(IllegalStateException.class, () -> sinEm.modificar(new Departamento()));
        assertThrows(IllegalStateException.class, () -> sinEm.buscarPorId(1L));
        assertThrows(IllegalStateException.class, () -> sinEm.eliminar(1L));
        assertThrows(IllegalStateException.class, () -> sinEm.findRange(0, 10));
        assertThrows(IllegalStateException.class, sinEm::count);
        assertThrows(IllegalStateException.class, sinEm::flush);
    }

    // ---------------------------------------------------------------- crear

    @Test
    void crearConNuloLanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> dao.crear(null));
        verify(em, never()).persist(any());
    }

    @Test
    void crearPersisteYDevuelveLaMismaInstancia() {
        Departamento d = new Departamento();

        assertSame(d, dao.crear(d));
        verify(em).persist(d);
    }

    @Test
    void crearPropagaLaExcepcionDelProveedor() {
        Departamento d = new Departamento();
        PersistenceException causa = new PersistenceException("fallo");
        org.mockito.Mockito.doThrow(causa).when(em).persist(d);

        assertSame(causa, assertThrows(PersistenceException.class, () -> dao.crear(d)));
    }

    // ---------------------------------------------------------------- modificar

    @Test
    void modificarConNuloLanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> dao.modificar(null));
    }

    @Test
    void modificarSinIdLanzaIllegalArgument() {
        Departamento d = new Departamento();
        when(unitUtil.getIdentifier(d)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> dao.modificar(d));
        verify(em, never()).merge(any());
    }

    @Test
    void modificarRegistroInexistenteLanzaNoEncontrado() {
        Departamento d = new Departamento();
        when(unitUtil.getIdentifier(d)).thenReturn(7L);
        when(em.find(Departamento.class, 7L)).thenReturn(null);

        assertThrows(NoEncontradoException.class, () -> dao.modificar(d));
        verify(em, never()).merge(any());
    }

    @Test
    void modificarFusionaYDevuelveLaInstanciaGestionada() {
        Departamento entrada = new Departamento();
        Departamento gestionada = new Departamento();
        when(unitUtil.getIdentifier(entrada)).thenReturn(7L);
        when(em.find(Departamento.class, 7L)).thenReturn(gestionada);
        when(em.merge(entrada)).thenReturn(gestionada);

        assertSame(gestionada, dao.modificar(entrada));
    }

    // ---------------------------------------------------------------- buscarPorId

    @Test
    void buscarPorIdNuloLanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorId(null));
    }

    @Test
    void buscarPorIdDevuelveOptionalSegunExista() {
        Departamento d = new Departamento();
        when(em.find(Departamento.class, 1L)).thenReturn(d);

        assertEquals(Optional.of(d), dao.buscarPorId(1L));
        assertEquals(Optional.empty(), dao.buscarPorId(2L));
    }

    // ---------------------------------------------------------------- eliminar

    @Test
    void eliminarConIdNuloLanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> dao.eliminar(null));
    }

    @Test
    void eliminarInexistenteLanzaNoEncontrado() {
        when(em.find(Departamento.class, 9L)).thenReturn(null);

        assertThrows(NoEncontradoException.class, () -> dao.eliminar(9L));
        verify(em, never()).remove(any());
    }

    @Test
    void eliminarRemueveLaEntidadEncontrada() {
        Departamento d = new Departamento();
        when(em.find(Departamento.class, 3L)).thenReturn(d);

        dao.eliminar(3L);

        verify(em).remove(d);
    }

    // ---------------------------------------------------------------- findRange

    @Test
    void findRangeRechazaPrimeroNegativo() {
        assertThrows(IllegalArgumentException.class, () -> dao.findRange(-1, 10));
    }

    @Test
    void findRangeRechazaMaxFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> dao.findRange(0, 0));
        assertThrows(IllegalArgumentException.class, () -> dao.findRange(0, -5));
        assertThrows(IllegalArgumentException.class, () -> dao.findRange(0, 101));
        verify(em, never()).createQuery(anyString(), any(Class.class));
    }

    @Test
    void findRangeAceptaPaginasConPrimeroIgualAlMax() {
        // Regresion: validar max > first rompia la paginacion (first=50, max=50).
        when(em.createQuery(anyString(), eq(Departamento.class))).thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(List.of());

        dao.findRange(50, 50);

        verify(consulta).setFirstResult(50);
        verify(consulta).setMaxResults(50);
    }

    @Test
    void findRangeAceptaElLimiteSuperiorDe100() {
        when(em.createQuery(anyString(), eq(Departamento.class))).thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(List.of());

        dao.findRange(0, 100);

        verify(consulta).setMaxResults(100);
    }

    @Test
    void findRangeOrdenaPorElAtributoIdDelMetamodeloYNoPorElLiteralId() {
        Departamento d = new Departamento();
        when(em.createQuery(anyString(), eq(Departamento.class))).thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(List.of(d));
        ArgumentCaptor<String> jpql = ArgumentCaptor.forClass(String.class);

        List<Departamento> resultado = dao.findRange(10, 20);

        verify(em).createQuery(jpql.capture(), eq(Departamento.class));
        assertEquals("SELECT e FROM Departamento e ORDER BY e.codigo ASC", jpql.getValue());
        verify(consulta).setFirstResult(10);
        verify(consulta).setMaxResults(20);
        assertEquals(List.of(d), resultado);
    }

    @Test
    void findRangeFallaSiLaEntidadNoTieneIdSimple() {
        when(entityType.getSingularAttributes()).thenReturn(Set.of());

        assertThrows(IllegalStateException.class, () -> dao.findRange(0, 10));
    }

    @Test
    void findRangePropagaLaExcepcionDelProveedor() {
        PersistenceException causa = new PersistenceException("fallo");
        when(em.createQuery(anyString(), eq(Departamento.class))).thenReturn(consulta);
        when(consulta.getResultList()).thenThrow(causa);

        assertSame(causa, assertThrows(PersistenceException.class, () -> dao.findRange(0, 10)));
    }

    // ---------------------------------------------------------------- count

    @Test
    void countDevuelveLongSinTruncarAInt() {
        long grande = Integer.MAX_VALUE + 5L;
        when(em.createQuery("SELECT COUNT(e) FROM Departamento e", Long.class)).thenReturn(consultaConteo);
        when(consultaConteo.getSingleResult()).thenReturn(grande);

        assertEquals(grande, dao.count());
    }

    @Test
    void countPropagaLaExcepcionEnLugarDeDevolverMenosUno() {
        PersistenceException causa = new PersistenceException("fallo");
        when(em.createQuery("SELECT COUNT(e) FROM Departamento e", Long.class)).thenReturn(consultaConteo);
        when(consultaConteo.getSingleResult()).thenThrow(causa);

        assertSame(causa, assertThrows(PersistenceException.class, dao::count));
    }

    // ---------------------------------------------------------------- existePorReferencia

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void existePorReferenciaCuentaFilasPorUnAtributoDelMetamodelo() {
        when(entityType.getAttribute("nombre")).thenReturn((Attribute) atributoNombre);
        when(em.createQuery(anyString(), eq(Long.class))).thenReturn(consultaConteo);
        when(consultaConteo.getSingleResult()).thenReturn(2L, 0L);

        assertTrue(dao.existePorReferencia("nombre", "x"));
        assertFalse(dao.existePorReferencia("nombre", "x"));

        org.mockito.ArgumentCaptor<String> jpql = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(em, org.mockito.Mockito.times(2)).createQuery(jpql.capture(), eq(Long.class));
        assertEquals("SELECT COUNT(e) FROM Departamento e WHERE e.nombre = :valor", jpql.getValue());
        verify(consultaConteo, org.mockito.Mockito.times(2)).setParameter("valor", "x");
    }

    @Test
    void existePorReferenciaRechazaUnAtributoQueNoExisteSinConsultar() {
        when(entityType.getAttribute("nada")).thenThrow(new IllegalArgumentException("sin atributo"));

        assertThrows(IllegalArgumentException.class, () -> dao.existePorReferencia("nada", "x"));
        verify(em, never()).createQuery(anyString(), eq(Long.class));
    }

    @Test
    void existePorReferenciaRechazaArgumentosNulosOVacios() {
        assertThrows(IllegalArgumentException.class, () -> dao.existePorReferencia(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> dao.existePorReferencia(" ", "x"));
        assertThrows(IllegalArgumentException.class, () -> dao.existePorReferencia("nombre", null));
    }

    // ---------------------------------------------------------------- flush

    @Test
    void flushDelegaEnElEntityManager() {
        dao.flush();

        verify(em).flush();
    }
}
