package com.sistemacontable.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.boundary.ErroresHttp.Traduccion;
import com.sistemacontable.boundary.dto.ErrorDto;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import jakarta.ejb.EJBException;
import jakarta.ejb.EJBTransactionRolledbackException;
import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParsingException;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.TransactionalException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.ProcessingException;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Traduccion de excepciones a respuestas HTTP: negocio 422, no encontrado 404, argumento invalido 400, integridad 409,
 * resto 500 generico. Los envoltorios del contenedor (EJBException y similares) se desenvuelven: un error de negocio
 * nunca debe llegar al cliente como 500.
 */
class ErroresHttpTest {

    // ------------------------------------------------------------------------------------ directas

    @Test
    void reglaDeNegocioEs422ConSuCodigoYMensaje() {
        Traduccion t = ErroresHttp.traducir(new ReglaNegocioException("Stock insuficiente"));

        assertEquals(422, t.estado());
        assertEquals("REGLA_NEGOCIO", t.cuerpo().codigo());
        assertEquals("Stock insuficiente", t.cuerpo().mensaje());
        assertNull(t.cuerpo().referencia());
    }

    @Test
    void noEncontradoEs404() {
        Traduccion t = ErroresHttp.traducir(new NoEncontradoException("Cuenta", 9L));

        assertEquals(404, t.estado());
        assertEquals("NO_ENCONTRADO", t.cuerpo().codigo());
        assertEquals("Cuenta con id 9 no existe", t.cuerpo().mensaje());
    }

    @Test
    void argumentoInvalidoEs400() {
        Traduccion t = ErroresHttp.traducir(new IllegalArgumentException("El parametro 'max' debe estar entre 1 y 100"));

        assertEquals(400, t.estado());
        assertEquals("SOLICITUD_INVALIDA", t.cuerpo().codigo());
        assertEquals("El parametro 'max' debe estar entre 1 y 100", t.cuerpo().mensaje());
    }

    @Test
    void jsonMalFormadoEs400SinRevelarElAnalizador() {
        Traduccion t = ErroresHttp.traducir(new JsonbException("Error interno de Yasson en org.eclipse.yasson..."));

        assertEquals(400, t.estado());
        assertEquals("JSON_INVALIDO", t.cuerpo().codigo());
        assertFalse(t.cuerpo().mensaje().toLowerCase().contains("yasson"));
    }

    @Test
    void errorDeAnalisisJsonEs400() {
        Traduccion t = ErroresHttp.traducir(new JsonParsingException("Unexpected char", null));

        assertEquals(400, t.estado());
        assertEquals("JSON_INVALIDO", t.cuerpo().codigo());
    }

    // ------------------------------------------------------------------------------- Bean Validation

    record Cuerpo(@NotBlank String nombre, @NotNull Long id) {
    }

    @Test
    void violacionesDeValidacionSon400ConLaListaOrdenadaPorCampo() {
        Validator validador = Validation.buildDefaultValidatorFactory().getValidator();
        ConstraintViolationException e = new ConstraintViolationException(validador.validate(new Cuerpo(" ", null)));

        Traduccion t = ErroresHttp.traducir(e);

        assertEquals(400, t.estado());
        assertEquals("VALIDACION", t.cuerpo().codigo());
        List<ErrorDto.Violacion> v = t.cuerpo().violaciones();
        assertEquals(2, v.size());
        assertEquals("id", v.get(0).campo());
        assertEquals("nombre", v.get(1).campo());
        assertFalse(v.get(0).mensaje().isBlank());
    }

    @Test
    void laRutaDeUnaViolacionConserva_elIndiceDeLaLineaYOmiteMetodoYParametro() {
        Validator validador = Validation.buildDefaultValidatorFactory().getValidator();
        com.sistemacontable.boundary.dto.CompraRequest compra = new com.sistemacontable.boundary.dto.CompraRequest(1L, 2L,
                null, null, java.math.BigDecimal.ZERO, java.util.Arrays.asList(
                        new com.sistemacontable.boundary.dto.CompraRequest.Linea(1L, 1L, java.math.BigDecimal.ONE,
                                java.math.BigDecimal.ONE),
                        new com.sistemacontable.boundary.dto.CompraRequest.Linea(null, 1L, java.math.BigDecimal.ONE,
                                null)));

        Traduccion t = ErroresHttp.traducir(new ConstraintViolationException(validador.validate(compra)));

        List<String> campos = t.cuerpo().violaciones().stream().map(ErrorDto.Violacion::campo).toList();
        assertEquals(List.of("lineas[1].loteId", "lineas[1].precio"), campos);
    }

    // --------------------------------------------------------------------------- envoltorios EJB/JTA

    @Test
    void desenvuelveEjbExceptionConReglaDeNegocio() {
        Traduccion t = ErroresHttp.traducir(new EJBException(new ReglaNegocioException("Partida descuadrada")));

        assertEquals(422, t.estado());
        assertEquals("Partida descuadrada", t.cuerpo().mensaje());
    }

    @Test
    void desenvuelveEjbExceptionConNoEncontrado() {
        assertEquals(404, ErroresHttp.traducir(new EJBException(new NoEncontradoException("Lote", 3L))).estado());
    }

    @Test
    void desenvuelveEjbExceptionConArgumentoInvalido() {
        assertEquals(400, ErroresHttp.traducir(new EJBException(new IllegalArgumentException("id nulo"))).estado());
    }

    @Test
    void desenvuelveEjbExceptionConMensajePropioYCausa() {
        EJBException conMensaje = new EJBException("transaccion revertida",
                new ReglaNegocioException("Stock insuficiente"));

        assertEquals(422, ErroresHttp.traducir(conMensaje).estado());
        assertEquals("Stock insuficiente", ErroresHttp.traducir(conMensaje).cuerpo().mensaje());
    }

    @Test
    void desenvuelveEnvoltoriosAnidados() {
        Throwable anidada = new EJBException(new EJBTransactionRolledbackException("revertida",
                new TransactionalException("tx", new ReglaNegocioException("Cuenta inactiva"))));

        Traduccion t = ErroresHttp.traducir(anidada);

        assertEquals(422, t.estado());
        assertEquals("Cuenta inactiva", t.cuerpo().mensaje());
    }

    @Test
    void desenvuelveRollbackDeJtaYProcessingException() {
        Throwable rollback = new jakarta.transaction.RollbackException("rollback");
        rollback.initCause(new NoEncontradoException("Venta", 1L));

        assertEquals(404, ErroresHttp.traducir(rollback).estado());
        assertEquals(400, ErroresHttp.traducir(new ProcessingException(new JsonbException("x"))).estado());
    }

    @Test
    void ejbExceptionSinCausaEsUn500Generico() {
        Traduccion t = ErroresHttp.traducir(new EJBException("fallo cualquiera"));

        assertEquals(500, t.estado());
        assertEquals("ERROR_INTERNO", t.cuerpo().codigo());
    }

    // ------------------------------------------------------------------------------------------ 500

    @Test
    void errorInesperadoEs500GenericoConReferenciaYSinFiltrarDetalles() {
        Traduccion t = ErroresHttp.traducir(new RuntimeException("ERROR: relation \"public.cuentas\" does not exist; "
                + "SELECT * FROM cuentas WHERE password = 'x'"));

        assertEquals(500, t.estado());
        assertEquals("ERROR_INTERNO", t.cuerpo().codigo());
        assertNotNull(t.cuerpo().referencia());
        assertFalse(t.cuerpo().referencia().isBlank());
        assertTrue(t.cuerpo().mensaje().contains(t.cuerpo().referencia()));
        String texto = t.cuerpo().toString();
        assertFalse(texto.contains("SELECT"));
        assertFalse(texto.contains("cuentas"));
        assertFalse(texto.contains("password"));
    }

    @Test
    void cadaErrorInesperadoRecibeUnaReferenciaDistinta() {
        String a = ErroresHttp.traducir(new RuntimeException("a")).cuerpo().referencia();
        String b = ErroresHttp.traducir(new RuntimeException("b")).cuerpo().referencia();

        assertFalse(a.equals(b));
    }

    @Test
    void unEjbConUnErrorDeInfraestructuraTambienEs500() {
        Traduccion t = ErroresHttp.traducir(new EJBException(new IllegalStateException("No hay EntityManager")));

        assertEquals(500, t.estado());
        assertFalse(t.cuerpo().mensaje().contains("EntityManager"));
    }

    // ------------------------------------------------------------------------------------ integridad

    @Test
    void violacionDeIntegridadSqlEs409SinMostrarElSql() {
        PersistenceException e = new PersistenceException("Exception [EclipseLink-4002]",
                new RuntimeException("Internal Exception", new SQLException(
                        "ERROR: duplicate key value violates unique constraint \"cuentas_codigo_key\"", "23505")));

        Traduccion t = ErroresHttp.traducir(new EJBException(e));

        assertEquals(409, t.estado());
        assertEquals("CONFLICTO_INTEGRIDAD", t.cuerpo().codigo());
        assertFalse(t.cuerpo().mensaje().contains("cuentas_codigo_key"));
        assertNull(t.cuerpo().referencia());
    }

    @Test
    void unErrorSqlQueNoEsDeIntegridadSigueSiendo500() {
        PersistenceException e = new PersistenceException(new SQLException("connection refused", "08001"));

        assertEquals(500, ErroresHttp.traducir(e).estado());
    }

    @Test
    void unaViolacionDeValidacionDentroDeUnaExcepcionDePersistenciaEs400() {
        Validator validador = Validation.buildDefaultValidatorFactory().getValidator();
        PersistenceException e = new PersistenceException(
                new ConstraintViolationException(validador.validate(new Cuerpo(null, 1L))));

        Traduccion t = ErroresHttp.traducir(new EJBException(e));

        assertEquals(400, t.estado());
        assertEquals("nombre", t.cuerpo().violaciones().get(0).campo());
    }

    // ------------------------------------------------------------------------- errores del propio JAX-RS

    @Test
    void conservaElEstadoDeLasExcepcionesWebDelFramework() {
        assertEquals(404, ErroresHttp.traducir(new NotFoundException()).estado());
        assertEquals(405, ErroresHttp.traducir(new NotAllowedException("GET")).estado());
        assertEquals(415, ErroresHttp.traducir(new NotSupportedException()).estado());
        assertEquals(403, ErroresHttp.traducir(new ForbiddenException()).estado());
    }

    @Test
    void lasExcepcionesWebLlevanUnCodigoHttp() {
        Traduccion t = ErroresHttp.traducir(new NotAllowedException("GET"));

        assertEquals("HTTP_405", t.cuerpo().codigo());
        assertFalse(t.cuerpo().mensaje().isBlank());
    }

    @Test
    void elMensajeDeUnaExcepcionWebDelFrameworkNoSeRepiteEnLaRespuesta() {
        Traduccion noExiste = ErroresHttp.traducir(new NotFoundException(
                "RESTEASY003210: Could not find resource for full path: http://host/api/nope"));
        Traduccion noPermitido = ErroresHttp.traducir(new jakarta.ws.rs.WebApplicationException(
                "RESTEASY003650: No resource method found for PUT, return 405 with Allow header", 405));

        assertEquals("Recurso no encontrado", noExiste.cuerpo().mensaje());
        assertEquals("Metodo HTTP no permitido para este recurso", noPermitido.cuerpo().mensaje());
        assertFalse(noExiste.cuerpo().mensaje().toUpperCase().contains("RESTEASY"));
    }

    @Test
    void unaExcepcionWebConEstadoSinTextoPropioTieneUnMensajeGenerico() {
        Traduccion t = ErroresHttp.traducir(new jakarta.ws.rs.WebApplicationException(
                jakarta.ws.rs.core.Response.status(409).build()));

        assertEquals(409, t.estado());
        assertEquals("HTTP_409", t.cuerpo().codigo());
        assertFalse(t.cuerpo().mensaje().isBlank());
    }

    @Test
    void unaExcepcionWebDeServidorNoFiltraSuMensaje() {
        Traduccion t = ErroresHttp.traducir(new InternalServerErrorException("detalle interno secreto"));

        assertEquals(500, t.estado());
        assertEquals("ERROR_INTERNO", t.cuerpo().codigo());
        assertFalse(t.cuerpo().mensaje().contains("secreto"));
        assertNotNull(t.cuerpo().referencia());
    }

    @Test
    void unaCadenaCiclicaDeCausasNoCuelga() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);

        assertEquals(500, ErroresHttp.traducir(a).estado());
    }
}
