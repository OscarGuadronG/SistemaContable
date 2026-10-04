package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;

/** Stock de un lote en un almacen. */
public record ExistenciasDto(Long id,
        Long loteId,
        Long almacenId,
        BigDecimal cantidad) {
}
