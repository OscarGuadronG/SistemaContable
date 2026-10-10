package com.sistemacontable.boundary;

import com.sistemacontable.boundary.ErroresHttp.Traduccion;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduce las violaciones de Bean Validation (cuerpos {@code @Valid} incompletos) al formato JSON de error comun.
 *
 * <p>Hace falta ademas de {@link ManejadorExcepciones} porque el motor JAX-RS de Liberty (RESTEasy) registra su
 * propio traductor para {@code ValidationException} que responde texto plano; JAX-RS elige el traductor cuyo tipo
 * esta mas cerca de la excepcion, y {@link ConstraintViolationException} esta mas cerca que {@code Throwable}.</p>
 */
@Provider
public class ManejadorValidacion implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException error) {
        Traduccion traduccion = ErroresHttp.traducir(error);
        return Response.status(traduccion.estado()).type(MediaType.APPLICATION_JSON)
                .entity(traduccion.cuerpo()).build();
    }
}
