package com.sistemacontable.boundary;

import com.sistemacontable.boundary.ErroresHttp.Traduccion;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Unico {@link ExceptionMapper} de la aplicacion: delega en {@link ErroresHttp} y responde siempre con un JSON
 * {@code {"codigo","mensaje"}} (mas {@code violaciones} o {@code referencia} segun el caso). Al mapear
 * {@link Throwable} tambien atiende las excepciones web del framework (404 de ruta, 405, 415...), conservando su
 * estado y sus cabeceras (p. ej. {@code Allow}).
 */
@Provider
public class ManejadorExcepciones implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable error) {
        if (error instanceof WebApplicationException web) {
            int estado = web.getResponse().getStatus();
            if (estado < 400) {
                return web.getResponse(); // redirecciones y similares: no son errores
            }
            Traduccion traduccion = ErroresHttp.traducir(error);
            if (traduccion.estado() == estado) {
                return Response.fromResponse(web.getResponse()).type(MediaType.APPLICATION_JSON)
                        .entity(traduccion.cuerpo()).build();
            }
            return responder(traduccion);
        }
        return responder(ErroresHttp.traducir(error));
    }

    private static Response responder(Traduccion traduccion) {
        return Response.status(traduccion.estado()).type(MediaType.APPLICATION_JSON)
                .entity(traduccion.cuerpo()).build();
    }
}
