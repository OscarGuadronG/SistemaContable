package com.sistemacontable.control.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.List;

/**
 * Utilidades para las pruebas de integracion que confirman datos (commit) en la H2 en memoria: conteos, lecturas
 * y limpieza completa de tablas. Solo para pruebas.
 */
final class Bd {

    /** Tablas en orden seguro para borrar respetando las claves foraneas. */
    private static final List<String> TABLAS = List.of("kardex_detalle", "kardex", "partida_detalle", "partida",
            "compra_detalle", "ventas_detalle", "existencias", "compra", "ventas", "planilla_detalle", "planilla",
            "lote", "producto", "tipo_producto", "unidad_medida", "almacen", "empleado", "departamento", "cliente",
            "proveedor", "tercero", "cuentas");

    private Bd() {
    }

    static long contar(EntityManager em, Class<?> entidad) {
        return ((Number) em.createQuery("SELECT COUNT(e) FROM " + entidad.getSimpleName() + " e")
                .getSingleResult()).longValue();
    }

    /** Stock de un lote en un almacen (cero si no hay fila). */
    static BigDecimal stock(EntityManager em, Long loteId, Long almacenId) {
        List<BigDecimal> filas = em.createQuery(
                "SELECT x.cantidad FROM Existencias x WHERE x.lote.id = :l AND x.almacen.id = :a", BigDecimal.class)
                .setParameter("l", loteId)
                .setParameter("a", almacenId)
                .getResultList();
        return filas.isEmpty() ? BigDecimal.ZERO : filas.get(0);
    }

    /** Vacia todas las tablas (sentencias nativas) y confirma. */
    static void vaciar(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("UPDATE public.cuentas SET id_padre = NULL").executeUpdate();
            em.createNativeQuery("UPDATE public.tipo_producto SET id_padre = NULL").executeUpdate();
            for (String tabla : TABLAS) {
                em.createNativeQuery("DELETE FROM public." + tabla).executeUpdate();
            }
            em.getTransaction().commit();
        } finally {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            em.close();
        }
    }
}
