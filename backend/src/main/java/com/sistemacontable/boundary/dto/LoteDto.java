package com.sistemacontable.boundary.dto;

import java.time.LocalDate;

/** Lote de un producto. */
public record LoteDto(Long id,
        Long productoId,
        String numLote,
        LocalDate fechCaducidad) {
}
