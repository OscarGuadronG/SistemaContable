package com.sistemacontable.boundary;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Lectura de parametros de consulta que JAX-RS no convierte por si solo (p. ej. fechas ISO). Los errores son
 * {@link IllegalArgumentException}, que el traductor de excepciones convierte en 400.
 */
public final class Parametros {

    private Parametros() {
    }

    /** Fecha obligatoria en formato ISO {@code AAAA-MM-DD}. */
    public static LocalDate fecha(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El parametro '" + nombre + "' es obligatorio (formato AAAA-MM-DD)");
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "El parametro '" + nombre + "' no es una fecha valida (formato AAAA-MM-DD): " + valor, e);
        }
    }
}
