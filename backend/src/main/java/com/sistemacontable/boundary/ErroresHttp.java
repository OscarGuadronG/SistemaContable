package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.ErrorDto;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import jakarta.ejb.EJBException;
import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParsingException;
import jakarta.transaction.TransactionalException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Traduccion pura de excepciones a (estado HTTP, cuerpo de error). Reglas:
 * <ul>
 *   <li>{@link ReglaNegocioException} 422; {@link NoEncontradoException} 404;</li>
 *   <li>{@link IllegalArgumentException}, {@link ConstraintViolationException} (con la lista de violaciones) y errores
 *       de JSON-B 400;</li>
 *   <li>una violacion de integridad SQL (SQLState clase 23, p. ej. clave duplicada) 409, sin mostrar el SQL;</li>
 *   <li>las excepciones web del propio JAX-RS conservan su estado (404, 405, 415...);</li>
 *   <li>cualquier otra cosa 500 con un mensaje generico y una referencia (UUID) que tambien se registra en el log del
 *       servidor junto con la traza; nunca se devuelve SQL, trazas ni mensajes internos.</li>
 * </ul>
 * El contenedor envuelve las excepciones inesperadas de un EJB en {@link EJBException} (y las de la transaccion en
 * {@link TransactionalException} o {@code RollbackException}): esos envoltorios se desenvuelven y la causa se traduce
 * con las mismas reglas, de modo que un error de negocio o un 404 nunca se disfraza de 500. Solo se atraviesan
 * envoltorios conocidos; un error sin clasificar se busca (solo) por violaciones de validacion o de integridad.
 */
public final class ErroresHttp {

    public static final String REGLA_NEGOCIO = "REGLA_NEGOCIO";
    public static final String NO_ENCONTRADO = "NO_ENCONTRADO";
    public static final String SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";
    public static final String VALIDACION = "VALIDACION";
    public static final String JSON_INVALIDO = "JSON_INVALIDO";
    public static final String CONFLICTO_INTEGRIDAD = "CONFLICTO_INTEGRIDAD";
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    private static final Logger LOG = Logger.getLogger(ErroresHttp.class.getName());
    private static final int PROFUNDIDAD_MAXIMA = 25;

    /** Resultado de traducir una excepcion. */
    public record Traduccion(int estado, ErrorDto cuerpo) {
    }

    private ErroresHttp() {
    }

    public static Traduccion traducir(Throwable error) {
        Set<Throwable> vistos = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable actual = error;
        for (int i = 0; actual != null && i < PROFUNDIDAD_MAXIMA && vistos.add(actual); i++) {
            Optional<Traduccion> traduccion = clasificar(actual);
            if (traduccion.isPresent()) {
                return traduccion.get();
            }
            if (!esEnvoltorio(actual)) {
                return buscarEnLaCadena(actual).orElseGet(() -> interno(error));
            }
            actual = causaDe(actual);
        }
        return interno(error);
    }

    // ------------------------------------------------------------------------------- clasificacion

    private static Optional<Traduccion> clasificar(Throwable e) {
        if (e instanceof ReglaNegocioException) {
            return Optional.of(new Traduccion(422, ErrorDto.de(REGLA_NEGOCIO, e.getMessage())));
        }
        if (e instanceof NoEncontradoException) {
            return Optional.of(new Traduccion(404, ErrorDto.de(NO_ENCONTRADO, e.getMessage())));
        }
        if (e instanceof ConstraintViolationException cve) {
            return Optional.of(new Traduccion(400, new ErrorDto(VALIDACION,
                    "La solicitud contiene datos invalidos", violaciones(cve), null)));
        }
        if (e instanceof IllegalArgumentException) {
            return Optional.of(new Traduccion(400, ErrorDto.de(SOLICITUD_INVALIDA, e.getMessage())));
        }
        if (e instanceof JsonbException || e instanceof JsonParsingException) {
            // el mensaje del analizador menciona clases internas: se devuelve uno generico
            return Optional.of(new Traduccion(400, ErrorDto.de(JSON_INVALIDO,
                    "El cuerpo de la solicitud no es un JSON valido para este recurso")));
        }
        if (e instanceof WebApplicationException web) {
            int estado = web.getResponse().getStatus();
            if (estado >= 500) {
                return Optional.of(interno(web));
            }
            // el mensaje del framework (p. ej. RESTEASY003210 con la URL) no se repite en la respuesta
            return Optional.of(new Traduccion(estado, ErrorDto.de("HTTP_" + estado, mensajeHttp(estado))));
        }
        return Optional.empty();
    }

    private static String mensajeHttp(int estado) {
        return switch (estado) {
            case 401 -> "Se requiere autenticacion";
            case 403 -> "Acceso denegado";
            case 404 -> "Recurso no encontrado";
            case 405 -> "Metodo HTTP no permitido para este recurso";
            case 406 -> "No se puede responder en el formato solicitado";
            case 415 -> "Tipo de contenido no soportado";
            default -> "La solicitud no se pudo atender (HTTP " + estado + ")";
        };
    }

    private static boolean esEnvoltorio(Throwable e) {
        return e instanceof EJBException
                || e instanceof TransactionalException
                || e instanceof jakarta.transaction.RollbackException
                || e instanceof jakarta.persistence.RollbackException
                || e instanceof ProcessingException;
    }

    private static Throwable causaDe(Throwable e) {
        if (e instanceof EJBException ejb && ejb.getCausedByException() != null) {
            return ejb.getCausedByException();
        }
        return e.getCause();
    }

    /** Busca, a lo largo de la cadena de causas, una violacion de validacion o de integridad SQL. */
    private static Optional<Traduccion> buscarEnLaCadena(Throwable raiz) {
        Set<Throwable> vistos = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable actual = raiz;
        for (int i = 0; actual != null && i < PROFUNDIDAD_MAXIMA && vistos.add(actual); i++) {
            if (actual instanceof ConstraintViolationException) {
                return clasificar(actual);
            }
            if (actual instanceof SQLException sql && sql.getSQLState() != null
                    && sql.getSQLState().startsWith("23")) {
                return Optional.of(new Traduccion(409, ErrorDto.de(CONFLICTO_INTEGRIDAD,
                        "La operacion viola una restriccion de integridad de los datos "
                                + "(registro duplicado o con referencias)")));
            }
            actual = causaDe(actual);
        }
        return Optional.empty();
    }

    private static Traduccion interno(Throwable error) {
        String referencia = UUID.randomUUID().toString();
        LOG.log(Level.SEVERE, "Error interno [referencia " + referencia + "]", error);
        return new Traduccion(500, new ErrorDto(ERROR_INTERNO,
                "Ocurrio un error interno. Referencia: " + referencia, null, referencia));
    }

    // ------------------------------------------------------------------------------------ violaciones

    private static List<ErrorDto.Violacion> violaciones(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> conjunto = e.getConstraintViolations();
        if (conjunto == null) {
            return List.of();
        }
        List<ErrorDto.Violacion> lista = new ArrayList<>();
        for (ConstraintViolation<?> v : conjunto) {
            lista.add(new ErrorDto.Violacion(campo(v.getPropertyPath()), v.getMessage()));
        }
        lista.sort(Comparator.comparing(ErrorDto.Violacion::campo).thenComparing(ErrorDto.Violacion::mensaje));
        return lista;
    }

    /**
     * Ruta de la propiedad sin los nodos de metodo y parametro, conservando el indice de las colecciones (por
     * ejemplo {@code registrar.arg0.lineas[1].precio} pasa a {@code lineas[1].precio}).
     */
    private static String campo(Path ruta) {
        if (ruta == null) {
            return "";
        }
        StringBuilder campo = new StringBuilder();
        for (Path.Node nodo : ruta) {
            ElementKind tipo = nodo.getKind();
            if (tipo == ElementKind.METHOD || tipo == ElementKind.CONSTRUCTOR || tipo == ElementKind.PARAMETER
                    || tipo == ElementKind.RETURN_VALUE || tipo == ElementKind.CROSS_PARAMETER) {
                continue;
            }
            if (nodo.isInIterable() && campo.length() > 0) {
                campo.append('[').append(nodo.getIndex() != null ? nodo.getIndex() : nodo.getKey()).append(']');
            }
            if (nodo.getName() != null) {
                if (campo.length() > 0) {
                    campo.append('.');
                }
                campo.append(nodo.getName());
            }
        }
        return campo.toString();
    }
}
