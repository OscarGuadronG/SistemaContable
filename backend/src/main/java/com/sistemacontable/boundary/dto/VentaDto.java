package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Venta. {@code lineas} es nulo (y no se serializa) en los listados; {@code GET /ventas/{id}} las incluye. */
public record VentaDto(Long id,
        LocalDate fecha,
        Long clienteId,
        Long empleadoId,
        String descripcion,
        List<Linea> lineas) {

    /** Linea de una venta. */
    public record Linea(Long id, Long loteId, Long almacenId, BigDecimal cantidad, BigDecimal precio) {
    }
}
