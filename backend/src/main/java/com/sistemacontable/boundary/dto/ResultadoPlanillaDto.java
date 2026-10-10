package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;

/** Calculo de un detalle de planilla (valores de referencia: ver contabilidad.properties). */
public record ResultadoPlanillaDto(BigDecimal sueldoProporcional,
        BigDecimal valorHora,
        BigDecimal pagoHorasDiurnas,
        BigDecimal pagoHorasNocturnas,
        BigDecimal devengado,
        BigDecimal isss,
        BigDecimal afp,
        BigDecimal renta,
        BigDecimal totalDeducciones,
        BigDecimal liquido) {
}
