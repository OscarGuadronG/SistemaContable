package com.sistemacontable.control.service;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.config.ConfigContable;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Hace en las pruebas de integracion lo que haria el contenedor: crea servicios y DAOs reales y les inyecta, por
 * reflexion, los campos {@code @Inject} (otros servicios, DAOs, configuracion) y {@code @PersistenceContext} (el
 * EntityManager de la prueba, RESOURCE_LOCAL con transaccion explicita).
 */
final class Cableado {

    private final EntityManager em;
    private final Map<Class<?>, Object> instancias = new HashMap<>();

    Cableado(EntityManager em, ConfigContable config) {
        this.em = em;
        instancias.put(ConfigContable.class, config);
    }

    /** Reemplaza una dependencia (p. ej. por un DAO que falla a proposito) antes de pedir los servicios. */
    <T> void reemplazar(Class<T> tipo, T instancia) {
        instancias.put(tipo, instancia);
    }

    @SuppressWarnings("unchecked")
    <T> T obtener(Class<T> tipo) {
        Object existente = instancias.get(tipo);
        if (existente != null) {
            return (T) existente;
        }
        try {
            T nueva = tipo.getDeclaredConstructor().newInstance();
            instancias.put(tipo, nueva);
            inyectar(nueva);
            return nueva;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo crear " + tipo, e);
        }
    }

    private void inyectar(Object objetivo) throws ReflectiveOperationException {
        for (Field campo : objetivo.getClass().getDeclaredFields()) {
            if (campo.isAnnotationPresent(PersistenceContext.class)) {
                JpaTestSupport.inyectarEm(objetivo, em);
            } else if (campo.isAnnotationPresent(Inject.class)) {
                campo.setAccessible(true);
                campo.set(objetivo, obtener(campo.getType()));
            }
        }
    }
}
