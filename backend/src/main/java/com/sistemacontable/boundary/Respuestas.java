package com.sistemacontable.boundary;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.function.Function;

/**
 * Construccion uniforme de respuestas de los recursos REST: paginas con la cabecera {@code Total-Records}, 201 con
 * {@code Location}, 200 y 204. Los recursos solo delegan en servicios y usan estas utilidades; aqui no hay reglas de
 * negocio.
 */
public final class Respuestas {

    /** Cabecera con el total de registros (long) de la coleccion completa, no de la pagina. */
    public static final String TOTAL_REGISTROS = "Total-Records";

    private Respuestas() {
    }

    /** 200 con la pagina ya mapeada a DTO y {@code Total-Records} con el total sin paginar. */
    public static <E, D> Response pagina(List<E> filas, long total, Function<E, D> mapeo) {
        List<D> cuerpo = filas.stream().map(mapeo).toList();
        return Response.ok(cuerpo).header(TOTAL_REGISTROS, total).build();
    }

    public static Response ok(Object cuerpo) {
        return Response.ok(cuerpo).build();
    }

    /**
     * 201 con el cuerpo creado y {@code Location} = {@code <base>/<recurso>/<id>} (la base es la de la aplicacion,
     * p. ej. {@code /api}); el recurso puede ser otro distinto al que recibe el POST (un traslado se consulta en
     * {@code /kardex/{id}}).
     */
    public static Response creado(UriInfo uriInfo, String recurso, Long id, Object cuerpo) {
        URI ubicacion = uriInfo.getBaseUriBuilder().path(recurso).path(String.valueOf(id)).build();
        return Response.created(ubicacion).entity(cuerpo).build();
    }

    public static Response sinContenido() {
        return Response.noContent().build();
    }

    /**
     * Cuerpo obligatorio: un POST o PUT sin cuerpo llega como nulo.
     *
     * @throws IllegalArgumentException si falta (el traductor de excepciones lo convierte en 400)
     */
    public static <T> T requerir(T cuerpo) {
        if (cuerpo == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud es obligatorio");
        }
        return cuerpo;
    }
}
