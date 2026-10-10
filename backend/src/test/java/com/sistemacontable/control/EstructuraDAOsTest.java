package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.entity.EntidadBase;
import com.sistemacontable.entity.MapeoEntidadesTest;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Cada entidad tiene su DAO @Stateless con su propio EntityManager y sin dependencias muertas. */
class EstructuraDAOsTest {

    static List<Class<?>> daos() throws ClassNotFoundException {
        List<Class<?>> resultado = new ArrayList<>();
        for (Class<?> entidad : MapeoEntidadesTest.ENTIDADES) {
            resultado.add(Class.forName("com.sistemacontable.control." + entidad.getSimpleName() + "DAO"));
        }
        return resultado;
    }

    @Test
    void hayUnDaoPorEntidad() throws Exception {
        Set<String> esperados = new TreeSet<>();
        for (Class<?> e : MapeoEntidadesTest.ENTIDADES) {
            esperados.add(e.getSimpleName() + "DAO");
        }
        Set<String> reales = new TreeSet<>();
        for (Class<?> d : daos()) {
            reales.add(d.getSimpleName());
        }
        assertEquals(esperados, reales);
        assertEquals(22, reales.size());
    }

    @ParameterizedTest
    @MethodSource("daos")
    void esStatelessLocalBeanQueExtiendeAbstractDaoConIdLong(Class<?> dao) {
        assertTrue(dao.isAnnotationPresent(Stateless.class), dao + " sin @Stateless");
        assertTrue(dao.isAnnotationPresent(LocalBean.class), dao + " sin @LocalBean");
        assertEquals(AbstractDAO.class, dao.getSuperclass());
        ParameterizedType tipo = (ParameterizedType) dao.getGenericSuperclass();
        Type[] args = tipo.getActualTypeArguments();
        assertTrue(EntidadBase.class.isAssignableFrom((Class<?>) args[0]));
        assertEquals(Long.class, args[1]);
        // El tipo de entidad coincide con el nombre del DAO
        assertEquals(((Class<?>) args[0]).getSimpleName() + "DAO", dao.getSimpleName());
    }

    @ParameterizedTest
    @MethodSource("daos")
    void tieneUnEntityManagerPrivadoDeLaUnidadContablePuYNingunInject(Class<?> dao) throws Exception {
        Field em = dao.getDeclaredField("em");
        assertEquals(EntityManager.class, em.getType());
        assertTrue(Modifier.isPrivate(em.getModifiers()), "em debe ser privado en " + dao);
        PersistenceContext pc = em.getAnnotation(PersistenceContext.class);
        assertNotNull(pc);
        assertEquals("contablePU", pc.unitName());
        for (Field f : dao.getDeclaredFields()) {
            assertFalse(f.isAnnotationPresent(Inject.class), "Dependencia @Inject sin uso en " + dao + "." + f.getName());
        }
    }
}
