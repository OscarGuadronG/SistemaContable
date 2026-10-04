package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.control.exception.ReglaNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Reparto contado/credito de un total (puro, sin mocks). */
class PagoDocumentoTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void cero_porCiento_todoEsCredito() {
        PagoDocumento p = PagoDocumento.dividir(bd("113.00"), bd("0"));

        assertEquals(bd("0.00"), p.contado());
        assertEquals(bd("113.00"), p.credito());
    }

    @Test
    void cien_porCiento_todoEsContado() {
        PagoDocumento p = PagoDocumento.dividir(bd("113.00"), bd("100"));

        assertEquals(bd("113.00"), p.contado());
        assertEquals(bd("0.00"), p.credito());
    }

    @Test
    void cuarentaSesenta_repartoExacto() {
        PagoDocumento p = PagoDocumento.dividir(bd("124.30"), bd("40"));

        assertEquals(bd("49.72"), p.contado());
        assertEquals(bd("74.58"), p.credito());
    }

    @Test
    void porcentajeQueNoDivideExacto_redondeaContadoYElCreditoEsElResto() {
        // 113.00 x 33.33 % = 37.6629 -> 37.66 ; credito = 113.00 - 37.66 = 75.34
        PagoDocumento p = PagoDocumento.dividir(bd("113.00"), bd("33.33"));

        assertEquals(bd("37.66"), p.contado());
        assertEquals(bd("75.34"), p.credito());
        assertEquals(0, bd("113.00").compareTo(p.contado().add(p.credito())));
    }

    @Test
    void elRedondeoDelContadoEsHalfUp() {
        // 0.05 x 50 % = 0.025 -> 0.03 ; credito 0.02
        PagoDocumento p = PagoDocumento.dividir(bd("0.05"), bd("50"));

        assertEquals(bd("0.03"), p.contado());
        assertEquals(bd("0.02"), p.credito());
    }

    @Test
    void elCreditoNuncaEsNegativoNiElContadoSuperaElTotal() {
        // 0.10 x 99.99 % = 0.09999 -> 0.10 ; credito 0.00
        PagoDocumento p = PagoDocumento.dividir(bd("0.10"), bd("99.99"));

        assertEquals(bd("0.10"), p.contado());
        assertEquals(bd("0.00"), p.credito());
        assertTrue(p.credito().signum() >= 0);
    }

    @Test
    void totalCeroNoTieneContadoNiCredito() {
        PagoDocumento p = PagoDocumento.dividir(bd("0.00"), bd("40"));

        assertEquals(bd("0.00"), p.contado());
        assertEquals(bd("0.00"), p.credito());
    }

    @Test
    void lasCotasDelRangoSeAceptan() {
        PagoDocumento.validarPorcentaje(bd("0"));
        PagoDocumento.validarPorcentaje(bd("0.00"));
        PagoDocumento.validarPorcentaje(bd("100"));
        PagoDocumento.validarPorcentaje(bd("100.00"));
        PagoDocumento.validarPorcentaje(bd("0.01"));
        PagoDocumento.validarPorcentaje(bd("99.99"));
    }

    @Test
    void nuloOFueraDeRangoEsReglaDeNegocio() {
        for (String malo : new String[] {"-1", "-0.01", "100.01", "101", "1000"}) {
            assertThrows(ReglaNegocioException.class, () -> PagoDocumento.validarPorcentaje(bd(malo)), malo);
            assertThrows(ReglaNegocioException.class, () -> PagoDocumento.dividir(bd("10.00"), bd(malo)), malo);
        }
        assertThrows(ReglaNegocioException.class, () -> PagoDocumento.validarPorcentaje(null));
        assertThrows(ReglaNegocioException.class, () -> PagoDocumento.dividir(bd("10.00"), null));
    }
}
