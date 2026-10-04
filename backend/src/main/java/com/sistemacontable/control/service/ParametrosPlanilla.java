package com.sistemacontable.control.service;

import com.sistemacontable.control.config.ConfigContable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parametros del calculo de planilla: ISSS (tasa del empleado y techo mensual), AFP (tasa del empleado), recargos de
 * horas extra, horas por mes (valor hora), dias del mes comercial y tabla progresiva de renta.
 *
 * <p>ATENCION: los valores por defecto de {@code contabilidad.properties} son de REFERENCIA (placeholders) y deben
 * verificarse contra la normativa vigente antes de usarse en una planilla real.</p>
 *
 * @param isssTasa tasa del empleado, p. ej. 0.03
 * @param isssTecho techo mensual del salario sobre el que se calcula el ISSS
 * @param afpTasa tasa del empleado, sin techo
 * @param recargoDiurna recargo de la hora extra diurna sobre el valor de la hora (1.00 = 100 %, se paga hora x (1 +
 *        recargo))
 * @param recargoNocturna igual para la hora extra nocturna
 * @param horasMes horas ordinarias del mes: valor hora = sueldo mensual / horasMes
 * @param diasMes dias del mes comercial usados para prorratear (los dias trabajados se topan en este valor)
 * @param tramos tabla de renta mensual ordenada por {@code desde}; el primero debe empezar en cero
 */
public record ParametrosPlanilla(BigDecimal isssTasa, BigDecimal isssTecho, BigDecimal afpTasa,
        BigDecimal recargoDiurna, BigDecimal recargoNocturna, BigDecimal horasMes, BigDecimal diasMes,
        List<Tramo> tramos) {

    /**
     * Tramo de renta: se aplica a bases {@code >= desde} hasta el {@code desde} del siguiente; retencion =
     * {@code cuotaFija + (base - sobre) x tasa}.
     */
    public record Tramo(BigDecimal desde, BigDecimal sobre, BigDecimal tasa, BigDecimal cuotaFija) {
    }

    public ParametrosPlanilla {
        noNegativo(isssTasa, "la tasa de ISSS");
        noNegativo(isssTecho, "el techo de ISSS");
        noNegativo(afpTasa, "la tasa de AFP");
        noNegativo(recargoDiurna, "el recargo de horas extra diurnas");
        noNegativo(recargoNocturna, "el recargo de horas extra nocturnas");
        if (horasMes == null || horasMes.signum() <= 0) {
            throw new IllegalArgumentException("Las horas del mes deben ser mayores que cero");
        }
        if (diasMes == null || diasMes.signum() <= 0) {
            throw new IllegalArgumentException("Los dias del mes deben ser mayores que cero");
        }
        if (tramos == null || tramos.isEmpty()) {
            throw new IllegalArgumentException("La tabla de renta debe tener al menos un tramo");
        }
        if (tramos.get(0).desde().signum() != 0) {
            throw new IllegalArgumentException("El primer tramo de renta debe empezar en cero");
        }
        for (int i = 1; i < tramos.size(); i++) {
            if (tramos.get(i).desde().compareTo(tramos.get(i - 1).desde()) <= 0) {
                throw new IllegalArgumentException("Los tramos de renta deben estar en orden ascendente por 'desde'");
            }
        }
        tramos = List.copyOf(tramos);
    }

    /**
     * Lee los parametros de la configuracion contable. Los tramos son {@code renta.tramo.N.desde|sobre|tasa|cuota.fija}
     * para N = 1, 2, ... mientras exista {@code renta.tramo.N.desde}.
     *
     * @throws IllegalStateException si falta una clave o un valor no es numerico
     */
    public static ParametrosPlanilla desde(ConfigContable config) {
        List<Tramo> tramos = new ArrayList<>();
        for (int n = 1; config.tiene("renta.tramo." + n + ".desde"); n++) {
            String base = "renta.tramo." + n + ".";
            tramos.add(new Tramo(config.getBigDecimal(base + "desde"), config.getBigDecimal(base + "sobre"),
                    config.getBigDecimal(base + "tasa"), config.getBigDecimal(base + "cuota.fija")));
        }
        return new ParametrosPlanilla(config.getBigDecimal(ConfigContable.ISSS_TASA_EMPLEADO),
                config.getBigDecimal(ConfigContable.ISSS_TECHO), config.getBigDecimal(ConfigContable.AFP_TASA_EMPLEADO),
                config.getBigDecimal(ConfigContable.HORA_EXTRA_DIURNA_RECARGO),
                config.getBigDecimal(ConfigContable.HORA_EXTRA_NOCTURNA_RECARGO),
                config.getBigDecimal(ConfigContable.HORAS_MES), config.getBigDecimal(ConfigContable.DIAS_MES), tramos);
    }

    private static void noNegativo(BigDecimal valor, String que) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("Falta o es negativo " + que);
        }
    }
}
