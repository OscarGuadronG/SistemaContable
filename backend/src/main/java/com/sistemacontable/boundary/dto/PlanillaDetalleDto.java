package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;

/** Detalle de planilla de un empleado. */
public record PlanillaDetalleDto(Long id,
        Long planillaId,
        Long empleadoId,
        Short dias,
        Short horasxDiur,
        Short horasxNoct,
        BigDecimal sueldoBase) {
}
