package com.sistemacontable.boundary.mapper;

import com.sistemacontable.entity.EntidadBase;
import java.util.List;
import java.util.function.Function;

/**
 * Utilidades comunes de los mapeadores. Los mapeadores son funciones puras y deben ejecutarse DENTRO de la
 * transaccion (la de los recursos o la del servicio): las relaciones son perezosas y aqui solo se lee su id, nunca
 * otro atributo, para no provocar cargas adicionales.
 */
final class Mapeo {

    private Mapeo() {
    }

    /** Id de una relacion, o nulo si no hay relacion. */
    static Long id(EntidadBase entidad) {
        return entidad == null ? null : entidad.getId();
    }

    /** Mapea una lista; una lista nula (lineas no solicitadas) se conserva nula. */
    static <E, D> List<D> lista(List<E> origen, Function<E, D> funcion) {
        return origen == null ? null : origen.stream().map(funcion).toList();
    }
}
