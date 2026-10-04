package com.sistemacontable.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ParametrosTest {

    @Test
    void leeUnaFechaIso() {
        assertEquals(LocalDate.of(2026, 3, 31), Parametros.fecha("2026-03-31", "desde"));
    }

    @Test
    void ignoraEspaciosAlrededor() {
        assertEquals(LocalDate.of(2026, 1, 5), Parametros.fecha(" 2026-01-05 ", "desde"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void unaFechaObligatoriaAusenteEsUnArgumentoInvalido(String valor) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Parametros.fecha(valor, "hasta"));

        assertEquals("El parametro 'hasta' es obligatorio (formato AAAA-MM-DD)", e.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"31/03/2026", "2026-13-01", "2026-02-30", "ayer", "2026-3-1"})
    void unaFechaMalFormadaEsUnArgumentoInvalido(String valor) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Parametros.fecha(valor, "desde"));

        assertEquals("El parametro 'desde' no es una fecha valida (formato AAAA-MM-DD): " + valor, e.getMessage());
    }
}
