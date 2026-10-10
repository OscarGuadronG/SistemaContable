package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DineroTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void redondeaAdosDecimalesConHalfUp() {
        assertEquals(bd("0.13"), Dinero.redondear(bd("0.125")));
        assertEquals(bd("0.12"), Dinero.redondear(bd("0.124999")));
        assertEquals(bd("2.68"), Dinero.redondear(bd("2.675")));
    }

    @Test
    void elRedondeoNormalizaLaEscalaAdos() {
        assertEquals(2, Dinero.redondear(bd("5")).scale());
        assertEquals(2, Dinero.redondear(bd("5.0000")).scale());
    }

    @Test
    void valoresNegativosRedondeanAlejandoseDeCero() {
        assertEquals(bd("-0.13"), Dinero.redondear(bd("-0.125")));
    }

    @Test
    void multiplicarRedondeaElProductoUnaSolaVez() {
        // 3 x 0.335 = 1.005 -> 1.01 (si se redondeara el factor antes darian 3 x 0.34 = 1.02)
        assertEquals(bd("1.01"), Dinero.multiplicar(bd("3"), bd("0.335")));
    }

    @Test
    void costoUnitarioConservaCuatroDecimales() {
        assertEquals(bd("3.3333"), Dinero.redondearCosto(bd("3.33333333")));
        assertEquals(4, Dinero.redondearCosto(bd("1")).scale());
    }

    @Test
    void argumentoNuloEsErrorDeProgramacion() {
        assertThrows(IllegalArgumentException.class, () -> Dinero.redondear(null));
        assertThrows(IllegalArgumentException.class, () -> Dinero.redondearCosto(null));
    }

    @Test
    void sumaIgnoraNulosComoCero() {
        assertEquals(bd("0.00"), Dinero.suma());
        assertEquals(bd("3.00"), Dinero.suma(bd("1"), null, bd("2")));
    }
}
