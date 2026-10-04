package com.sistemacontable.control.service;

import com.sistemacontable.entity.Planilla;
import java.math.BigDecimal;
import java.util.List;

/** Calculo de una planilla completa: una linea por empleado y los totales. */
public record ResumenPlanilla(Planilla planilla, List<LineaPlanilla> lineas, BigDecimal totalDevengado,
        BigDecimal totalIsss, BigDecimal totalAfp, BigDecimal totalRenta, BigDecimal totalDeducciones,
        BigDecimal totalLiquido) {

    public ResumenPlanilla {
        lineas = List.copyOf(lineas);
    }
}
