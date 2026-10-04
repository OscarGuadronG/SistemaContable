package com.sistemacontable.boundary.dto;

import com.sistemacontable.entity.enums.TipoMovimiento;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** Movimiento de kardex. {@code lineas} es nulo (y no se serializa) en los listados. */
public record KardexDto(Long id,
        TipoMovimiento tipoMovimiento,
        OffsetDateTime fecha,
        Long origenId,
        Long destinoId,
        Long compraId,
        Long ventaId,
        String descripcion,
        List<Linea> lineas) {

    /** Linea de un movimiento: lote, cantidad (siempre positiva) y costo unitario. */
    public record Linea(Long id, Long loteId, BigDecimal cantidad, BigDecimal precioCosto) {
    }
}
