package com.sistemacontable.control.service;

import java.math.BigDecimal;

/**
 * Resultado del calculo de planilla de un empleado. Todos los importes en 2 decimales salvo {@code valorHora}
 * (4 decimales, informativo).
 *
 * @param sueldoProporcional sueldo base prorrateado por dias trabajados
 * @param valorHora sueldo mensual / horas del mes
 * @param devengado sueldo proporcional + horas extra (base de las deducciones)
 * @param liquido devengado - ISSS - AFP - renta
 */
public record ResultadoPlanilla(BigDecimal sueldoProporcional, BigDecimal valorHora, BigDecimal pagoHorasDiurnas,
        BigDecimal pagoHorasNocturnas, BigDecimal devengado, BigDecimal isss, BigDecimal afp, BigDecimal renta,
        BigDecimal totalDeducciones, BigDecimal liquido) {
}
