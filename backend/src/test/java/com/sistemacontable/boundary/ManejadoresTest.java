package com.sistemacontable.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.boundary.dto.ErrorDto;
import com.sistemacontable.control.exception.ReglaNegocioException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.junit.jupiter.api.Test;

/**
 * Los proveedores de excepciones. {@link ManejadorValidacion} existe porque RESTEasy (el motor JAX-RS de Liberty)
 * trae su propio traductor de {@code ValidationException} que responde texto plano; al declarar uno para
 * {@link ConstraintViolationException}, mas cercano en la jerarquia, las violaciones salen con el mismo formato JSON
 * que el resto de errores.
 */
class ManejadoresTest {

    record Cuerpo(@NotBlank String nombre) {
    }

    @Test
    void laValidacionSeResponde400ConLaListaDeViolacionesEnJson() {
        ConstraintViolationException e = new ConstraintViolationException(
                Validation.buildDefaultValidatorFactory().getValidator().validate(new Cuerpo(" ")));

        Response r = new ManejadorValidacion().toResponse(e);

        assertEquals(400, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());
        ErrorDto cuerpo = assertInstanceOf(ErrorDto.class, r.getEntity());
        assertEquals("VALIDACION", cuerpo.codigo());
        assertEquals("nombre", cuerpo.violaciones().get(0).campo());
    }

    @Test
    void cadaManejadorEstaRegistradoComoProveedorDeSuTipo() {
        assertTrue(ManejadorValidacion.class.isAnnotationPresent(Provider.class));
        assertTrue(ManejadorExcepciones.class.isAnnotationPresent(Provider.class));
        assertTrue(ExceptionMapper.class.isAssignableFrom(ManejadorValidacion.class));
    }

    @Test
    void unaExcepcionWebConservaSuCabeceraAllow() {
        Response r = new ManejadorExcepciones().toResponse(new NotAllowedException(
                Response.status(405).header("Allow", "GET, HEAD").build()));

        assertEquals(405, r.getStatus());
        assertNotNull(r.getHeaderString("Allow"));
        assertEquals("HTTP_405", ((ErrorDto) r.getEntity()).codigo());
    }

    @Test
    void unaReglaDeNegocioSeResponde422EnJson() {
        Response r = new ManejadorExcepciones().toResponse(new ReglaNegocioException("Stock insuficiente"));

        assertEquals(422, r.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, r.getMediaType());
    }
}
