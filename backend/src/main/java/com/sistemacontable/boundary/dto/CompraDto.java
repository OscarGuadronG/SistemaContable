package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Compra. {@code lineas} es nulo (y no se serializa) en los listados; {@code GET /compras/{id}} las incluye. */
public record CompraDto(Long id,
        LocalDate fecha,
        Long proveedorId,
        Long empleadoId,
        String descripcion,
        List<Linea> lineas) {

    /** Linea de una compra. */
    public record Linea(Long id, Long loteId, Long almacenId, BigDecimal cantidad, BigDecimal precio) {
    }
}
