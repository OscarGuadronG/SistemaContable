package com.sistemacontable.control.service;

import com.sistemacontable.control.service.ParametrosPlanilla.Tramo;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * Calculo puro de la planilla de UN empleado (sin acceso a datos). Los parametros vienen de
 * {@link ParametrosPlanilla}; sus valores por defecto son de REFERENCIA y deben verificarse contra la normativa
 * vigente.
 *
 * <p>Formulas (cada importe se redondea una sola vez a 2 decimales, HALF_UP, via {@link Dinero}):</p>
 * <ul>
 *   <li>dias efectivos = min(dias, diasMes); sueldo proporcional = sueldoBase x dias efectivos / diasMes.</li>
 *   <li>valor hora = sueldoBase / horasMes (sobre el sueldo mensual completo, no el proporcional).</li>
 *   <li>horas extra = horas x valor hora x (1 + recargo), con recargo diurno o nocturno (el recargo es el
 *       incremento sobre la hora ordinaria: 1.00 paga el doble).</li>
 *   <li>devengado = sueldo proporcional + pago de horas extra.</li>
 *   <li>ISSS = min(devengado, techo) x tasa ISSS; AFP = devengado x tasa AFP (sin techo).</li>
 *   <li>base de renta = devengado - ISSS - AFP; renta = tabla progresiva ({@link #retencionRenta}).</li>
 *   <li>liquido = devengado - ISSS - AFP - renta.</li>
 * </ul>
 * La tabla de renta se interpreta como mensual: una planilla de otro periodo requeriria otra tabla. No se calculan
 * aportes patronales.
 */
public final class CalculoPlanilla {

    private CalculoPlanilla() {
    }

    /**
     * @param sueldoBase sueldo mensual (congelado en el detalle), no negativo
     * @param dias dias trabajados (&gt;= 0)
     * @param horasDiurnas horas extra diurnas (&gt;= 0)
     * @param horasNocturnas horas extra nocturnas (&gt;= 0)
     * @throws IllegalArgumentException ante entradas nulas o negativas (los servicios las validan antes)
     */
    public static ResultadoPlanilla calcular(BigDecimal sueldoBase, int dias, int horasDiurnas, int horasNocturnas,
            ParametrosPlanilla p) {
        if (sueldoBase == null || sueldoBase.signum() < 0) {
            throw new IllegalArgumentException("El sueldo base debe ser mayor o igual que cero");
        }
        if (dias < 0 || horasDiurnas < 0 || horasNocturnas < 0) {
            throw new IllegalArgumentException("Los dias y las horas extra no pueden ser negativos");
        }
        if (p == null) {
            throw new IllegalArgumentException("Los parametros de planilla son obligatorios");
        }
        BigDecimal diasEfectivos = BigDecimal.valueOf(dias).min(p.diasMes());
        BigDecimal sueldoProporcional = Dinero.redondear(
                sueldoBase.multiply(diasEfectivos).divide(p.diasMes(), MathContext.DECIMAL128));
        BigDecimal valorHora = sueldoBase.divide(p.horasMes(), MathContext.DECIMAL128);
        BigDecimal pagoDiurnas = Dinero.redondear(extra(valorHora, horasDiurnas, p.recargoDiurna()));
        BigDecimal pagoNocturnas = Dinero.redondear(extra(valorHora, horasNocturnas, p.recargoNocturna()));
        BigDecimal devengado = sueldoProporcional.add(pagoDiurnas).add(pagoNocturnas);

        BigDecimal isss = Dinero.multiplicar(devengado.min(p.isssTecho()), p.isssTasa());
        BigDecimal afp = Dinero.multiplicar(devengado, p.afpTasa());
        BigDecimal renta = retencionRenta(devengado.subtract(isss).subtract(afp), p.tramos());
        BigDecimal deducciones = isss.add(afp).add(renta);
        return new ResultadoPlanilla(sueldoProporcional, Dinero.redondearCosto(valorHora), pagoDiurnas,
                pagoNocturnas, devengado, isss, afp, renta, deducciones, devengado.subtract(deducciones));
    }

    /**
     * Retencion de renta para una base mensual: se toma el ultimo tramo cuyo {@code desde} es menor o igual que la
     * base y se aplica {@code cuotaFija + (base - sobre) x tasa}. Una base cero o negativa no retiene.
     */
    public static BigDecimal retencionRenta(BigDecimal base, List<Tramo> tramos) {
        if (base.signum() <= 0) {
            return Dinero.redondear(BigDecimal.ZERO);
        }
        Tramo aplicable = tramos.get(0);
        for (Tramo t : tramos) {
            if (base.compareTo(t.desde()) >= 0) {
                aplicable = t;
            }
        }
        return Dinero.redondear(aplicable.cuotaFija().add(base.subtract(aplicable.sobre()).multiply(aplicable.tasa())));
    }

    private static BigDecimal extra(BigDecimal valorHora, int horas, BigDecimal recargo) {
        return valorHora.multiply(BigDecimal.valueOf(horas)).multiply(BigDecimal.ONE.add(recargo));
    }
}
