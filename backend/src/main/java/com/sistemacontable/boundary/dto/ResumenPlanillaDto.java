package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;
import java.util.List;

/** Calculo de una planilla completa. */
public record ResumenPlanillaDto(PlanillaDto planilla,
        List<Linea> lineas,
        BigDecimal totalDevengado,
        BigDecimal totalIsss,
        BigDecimal totalAfp,
        BigDecimal totalRenta,
        BigDecimal totalDeducciones,
        BigDecimal totalLiquido) {

    /** Un detalle junto con su calculo. */
    public record Linea(PlanillaDetalleDto detalle, ResultadoPlanillaDto calculo) {
    }
}
