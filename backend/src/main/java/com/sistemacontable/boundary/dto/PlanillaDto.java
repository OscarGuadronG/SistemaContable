package com.sistemacontable.boundary.dto;

import com.sistemacontable.entity.enums.EstadoPlanilla;
import java.time.OffsetDateTime;

/** Planilla de pago. */
public record PlanillaDto(Long id,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        OffsetDateTime fechaPago,
        String descripcion,
        EstadoPlanilla estado) {
}
