package com.sistemacontable.boundary.dto;

import java.util.List;

/**
 * Cuerpo de toda respuesta de error. {@code violaciones} solo viaja en errores de validacion y {@code referencia}
 * solo en errores internos (es el identificador que tambien se registra en el log del servidor); los campos nulos no
 * se serializan.
 */
public record ErrorDto(String codigo, String mensaje, List<Violacion> violaciones, String referencia) {

    /** Una restriccion incumplida: campo (ruta de la propiedad) y mensaje. */
    public record Violacion(String campo, String mensaje) {
    }

    public static ErrorDto de(String codigo, String mensaje) {
        return new ErrorDto(codigo, mensaje, null, null);
    }
}
