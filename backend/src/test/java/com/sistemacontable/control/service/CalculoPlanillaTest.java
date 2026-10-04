package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sistemacontable.control.config.ConfigPruebas;
import com.sistemacontable.control.service.ParametrosPlanilla.Tramo;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Calculo puro de planilla con parametros explicitos (los mismos valores de referencia de contabilidad.properties:
 * ISSS 3 % con techo 1000.00, AFP 7.25 %, valor hora = sueldo / 240, recargo diurno 100 % y nocturno 125 %, mes de
 * 30 dias y tabla de renta de 4 tramos). Los valores esperados estan calculados a mano en cada prueba. Son
 * parametros de REFERENCIA: deben verificarse contra la normativa vigente.
 */
class CalculoPlanillaTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static final List<Tramo> TRAMOS = List.of(
            new Tramo(bd("0.00"), bd("0.00"), bd("0.00"), bd("0.00")),
            new Tramo(bd("472.01"), bd("472.00"), bd("0.10"), bd("17.67")),
            new Tramo(bd("895.25"), bd("895.24"), bd("0.20"), bd("60.00")),
            new Tramo(bd("2038.11"), bd("2038.10"), bd("0.30"), bd("288.57")));

    private static final ParametrosPlanilla P = new ParametrosPlanilla(bd("0.03"), bd("1000.00"), bd("0.0725"),
            bd("1.00"), bd("1.25"), bd("240"), bd("30"), TRAMOS);

    private static ResultadoPlanilla calcular(String sueldo, int dias, int horasDiurnas, int horasNocturnas) {
        return CalculoPlanilla.calcular(bd(sueldo), dias, horasDiurnas, horasNocturnas, P);
    }

    @Test
    void sueldo1000_mesCompleto_isss30_afp72_50_renta60_45_liquido837_05() {
        // ISSS 1000 x 3 % = 30.00 ; AFP 1000 x 7.25 % = 72.50 ; base renta = 1000 - 30 - 72.50 = 897.50
        // tramo 3: 60.00 + (897.50 - 895.24) x 20 % = 60.00 + 0.452 = 60.452 -> 60.45
        // liquido = 1000 - 30 - 72.50 - 60.45 = 837.05
        ResultadoPlanilla r = calcular("1000", 30, 0, 0);

        assertEquals(bd("1000.00"), r.devengado());
        assertEquals(bd("30.00"), r.isss());
        assertEquals(bd("72.50"), r.afp());
        assertEquals(bd("60.45"), r.renta());
        assertEquals(bd("162.95"), r.totalDeducciones());
        assertEquals(bd("837.05"), r.liquido());
    }

    @Test
    void sueldo600_isss18_afp43_50_renta24_32_liquido514_18() {
        // base = 600 - 18 - 43.50 = 538.50 ; tramo 2: 17.67 + (538.50 - 472.00) x 10 % = 17.67 + 6.65 = 24.32
        ResultadoPlanilla r = calcular("600", 30, 0, 0);

        assertEquals(bd("18.00"), r.isss());
        assertEquals(bd("43.50"), r.afp());
        assertEquals(bd("24.32"), r.renta());
        assertEquals(bd("514.18"), r.liquido());
    }

    @Test
    void sueldo400_bajoElPrimerTramo_sinRenta_liquido359() {
        // ISSS 12.00 ; AFP 29.00 ; base 359.00 < 472.01 -> renta 0
        ResultadoPlanilla r = calcular("400", 30, 0, 0);

        assertEquals(bd("12.00"), r.isss());
        assertEquals(bd("29.00"), r.afp());
        assertEquals(bd("0.00"), r.renta());
        assertEquals(bd("359.00"), r.liquido());
    }

    @Test
    void sueldo2400_isssConTecho30_afp174_renta335_94_liquido1860_06() {
        // ISSS: min(2400, 1000) x 3 % = 30.00 ; AFP 2400 x 7.25 % = 174.00 ; base = 2400 - 30 - 174 = 2196.00
        // tramo 4: 288.57 + (2196.00 - 2038.10) x 30 % = 288.57 + 47.37 = 335.94
        ResultadoPlanilla r = calcular("2400", 30, 0, 0);

        assertEquals(bd("30.00"), r.isss());
        assertEquals(bd("174.00"), r.afp());
        assertEquals(bd("335.94"), r.renta());
        assertEquals(bd("1860.06"), r.liquido());
    }

    @Test
    void elIsssSeCalculaSobreElSueldoSiNoLlegaAlTecho() {
        assertEquals(bd("24.00"), calcular("800", 30, 0, 0).isss());
        assertEquals(bd("30.00"), calcular("1000", 30, 0, 0).isss());
        assertEquals(bd("30.00"), calcular("1500", 30, 0, 0).isss());
    }

    @Test
    void horasExtra_sueldo1200_10diurnas4nocturnas_devengado1345_liquido1093_04() {
        // valor hora = 1200 / 240 = 5.00 ; diurnas 10 x 5.00 x (1 + 1.00) = 100.00 ; nocturnas 4 x 5.00 x 2.25 = 45.00
        // devengado = 1200 + 100 + 45 = 1345.00 ; ISSS = min(1345, 1000) x 3 % = 30.00
        // AFP = 1345 x 7.25 % = 97.5125 -> 97.51 ; base = 1345 - 30 - 97.51 = 1217.49
        // tramo 3: 60.00 + (1217.49 - 895.24) x 20 % = 60.00 + 64.45 = 124.45
        // liquido = 1345 - 30 - 97.51 - 124.45 = 1093.04
        ResultadoPlanilla r = calcular("1200", 30, 10, 4);

        assertEquals(bd("1200.00"), r.sueldoProporcional());
        assertEquals(bd("5.0000"), r.valorHora());
        assertEquals(bd("100.00"), r.pagoHorasDiurnas());
        assertEquals(bd("45.00"), r.pagoHorasNocturnas());
        assertEquals(bd("1345.00"), r.devengado());
        assertEquals(bd("30.00"), r.isss());
        assertEquals(bd("97.51"), r.afp());
        assertEquals(bd("124.45"), r.renta());
        assertEquals(bd("1093.04"), r.liquido());
    }

    @Test
    void proporcionalPorDias_sueldo900_15dias_devengado450_afp32_63_liquido403_87() {
        // devengado = 900 x 15 / 30 = 450.00 ; ISSS 450 x 3 % = 13.50 ; AFP 450 x 7.25 % = 32.625 -> 32.63
        // base = 450 - 13.50 - 32.63 = 403.87 < 472.01 -> renta 0 ; liquido = 450 - 13.50 - 32.63 = 403.87
        ResultadoPlanilla r = calcular("900", 15, 0, 0);

        assertEquals(bd("450.00"), r.devengado());
        assertEquals(bd("13.50"), r.isss());
        assertEquals(bd("32.63"), r.afp());
        assertEquals(bd("0.00"), r.renta());
        assertEquals(bd("403.87"), r.liquido());
    }

    @Test
    void diasSeRedondeanSobreElDevengado_sueldo1000_10dias_devengado333_33() {
        // 1000 x 10 / 30 = 333.333... -> 333.33
        assertEquals(bd("333.33"), calcular("1000", 10, 0, 0).devengado());
    }

    @Test
    void un_dia31NoPagaMasQueElMesCompleto() {
        // dias efectivos = min(31, 30) = 30
        assertEquals(bd("900.00"), calcular("900", 31, 0, 0).devengado());
    }

    @Test
    void sinDiasTrabajadosNoHayDevengadoNiDeducciones() {
        ResultadoPlanilla r = calcular("900", 0, 0, 0);

        assertEquals(bd("0.00"), r.devengado());
        assertEquals(bd("0.00"), r.isss());
        assertEquals(bd("0.00"), r.afp());
        assertEquals(bd("0.00"), r.renta());
        assertEquals(bd("0.00"), r.liquido());
    }

    @Test
    void lasHorasExtraSePagaAunqueLosDiasSeanProporcionales() {
        // valor hora sobre el sueldo mensual completo: 960 / 240 = 4.00 ; 5 diurnas x 4.00 x 2 = 40.00
        ResultadoPlanilla r = calcular("960", 15, 5, 0);

        assertEquals(bd("480.00"), r.sueldoProporcional());
        assertEquals(bd("40.00"), r.pagoHorasDiurnas());
        assertEquals(bd("520.00"), r.devengado());
    }

    // ------------------------------------------------------------------------ tabla de renta

    @Test
    void rentaEnLosLimitesDeLosTramos() {
        // 472.00 -> tramo 1 (0.00) ; 472.01 -> 17.67 + 0.01 x 10 % = 17.671 -> 17.67
        assertEquals(bd("0.00"), CalculoPlanilla.retencionRenta(bd("472.00"), TRAMOS));
        assertEquals(bd("17.67"), CalculoPlanilla.retencionRenta(bd("472.01"), TRAMOS));
        // 895.24 -> 17.67 + 423.24 x 10 % = 59.994 -> 59.99 ; 895.25 -> 60.00 + 0.01 x 20 % = 60.002 -> 60.00
        assertEquals(bd("59.99"), CalculoPlanilla.retencionRenta(bd("895.24"), TRAMOS));
        assertEquals(bd("60.00"), CalculoPlanilla.retencionRenta(bd("895.25"), TRAMOS));
        // 2038.10 -> 60.00 + 1142.86 x 20 % = 288.572 -> 288.57 ; 2038.11 -> 288.57 + 0.01 x 30 % = 288.573 -> 288.57
        assertEquals(bd("288.57"), CalculoPlanilla.retencionRenta(bd("2038.10"), TRAMOS));
        assertEquals(bd("288.57"), CalculoPlanilla.retencionRenta(bd("2038.11"), TRAMOS));
        // base muy alta: 288.57 + (10000 - 2038.10) x 30 % = 288.57 + 2388.57 = 2677.14
        assertEquals(bd("2677.14"), CalculoPlanilla.retencionRenta(bd("10000.00"), TRAMOS));
    }

    @Test
    void rentaDeBaseCeroONegativaEsCero() {
        assertEquals(bd("0.00"), CalculoPlanilla.retencionRenta(BigDecimal.ZERO, TRAMOS));
        assertEquals(bd("0.00"), CalculoPlanilla.retencionRenta(bd("-5"), TRAMOS));
    }

    // ------------------------------------------------------------------------------ entradas

    @Test
    void entradasNegativasONulasSonErrorDeProgramacion() {
        assertThrows(IllegalArgumentException.class, () -> CalculoPlanilla.calcular(bd("-1"), 30, 0, 0, P));
        assertThrows(IllegalArgumentException.class, () -> CalculoPlanilla.calcular(null, 30, 0, 0, P));
        assertThrows(IllegalArgumentException.class, () -> CalculoPlanilla.calcular(bd("1"), -1, 0, 0, P));
        assertThrows(IllegalArgumentException.class, () -> CalculoPlanilla.calcular(bd("1"), 30, -1, 0, P));
        assertThrows(IllegalArgumentException.class, () -> CalculoPlanilla.calcular(bd("1"), 30, 0, -1, P));
        assertThrows(IllegalArgumentException.class, () -> CalculoPlanilla.calcular(bd("1"), 30, 0, 0, null));
    }

    // ----------------------------------------------------------------------------- parametros

    @Test
    void losParametrosSeLeenDeLaConfiguracionPorDefecto() {
        ParametrosPlanilla p = ParametrosPlanilla.desde(ConfigPruebas.porDefecto());

        assertEquals(0, bd("0.03").compareTo(p.isssTasa()));
        assertEquals(0, bd("1000.00").compareTo(p.isssTecho()));
        assertEquals(0, bd("0.0725").compareTo(p.afpTasa()));
        assertEquals(0, bd("1.00").compareTo(p.recargoDiurna()));
        assertEquals(0, bd("1.25").compareTo(p.recargoNocturna()));
        assertEquals(0, bd("240").compareTo(p.horasMes()));
        assertEquals(0, bd("30").compareTo(p.diasMes()));
        assertEquals(4, p.tramos().size());
        assertEquals(0, bd("2038.11").compareTo(p.tramos().get(3).desde()));
        assertEquals(0, bd("288.57").compareTo(p.tramos().get(3).cuotaFija()));
        // y reproduce el calculo manual del caso de 1000
        assertEquals(bd("837.05"), CalculoPlanilla.calcular(bd("1000"), 30, 0, 0, p).liquido());
    }

    @Test
    void parametrosInvalidosSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> new ParametrosPlanilla(bd("0.03"), bd("1000"), bd("0.07"),
                bd("1"), bd("1.25"), bd("240"), bd("30"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ParametrosPlanilla(bd("0.03"), bd("1000"), bd("0.07"),
                bd("1"), bd("1.25"), BigDecimal.ZERO, bd("30"), TRAMOS));
        assertThrows(IllegalArgumentException.class, () -> new ParametrosPlanilla(bd("0.03"), bd("1000"), bd("0.07"),
                bd("1"), bd("1.25"), bd("240"), BigDecimal.ZERO, TRAMOS));
        assertThrows(IllegalArgumentException.class, () -> new ParametrosPlanilla(bd("-0.03"), bd("1000"), bd("0.07"),
                bd("1"), bd("1.25"), bd("240"), bd("30"), TRAMOS));
        // tramos desordenados
        assertThrows(IllegalArgumentException.class, () -> new ParametrosPlanilla(bd("0.03"), bd("1000"), bd("0.07"),
                bd("1"), bd("1.25"), bd("240"), bd("30"), List.of(TRAMOS.get(1), TRAMOS.get(0))));
        // el primer tramo debe empezar en cero
        assertThrows(IllegalArgumentException.class, () -> new ParametrosPlanilla(bd("0.03"), bd("1000"), bd("0.07"),
                bd("1"), bd("1.25"), bd("240"), bd("30"), List.of(TRAMOS.get(1), TRAMOS.get(2))));
    }
}
