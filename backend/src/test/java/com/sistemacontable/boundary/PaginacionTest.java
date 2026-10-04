package com.sistemacontable.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Validacion de first/max: el bug del proyecto de referencia era exigir max > first. */
class PaginacionTest {

    @ParameterizedTest
    @CsvSource({"0,50", "0,1", "0,100", "50,50", "50,100", "1000,10", "100,100", "7,3"})
    void aceptaCualquierPrimeroNoNegativoConMaxEntreUnoYCien(int first, int max) {
        Paginacion p = Paginacion.de(first, max);

        assertEquals(first, p.first());
        assertEquals(max, p.max());
    }

    @Test
    void firstIgualAMaxNoEsUnError() {
        // regresion: con "max > first" la pagina 2 de tamano 50 devolvia 422
        assertEquals(50, Paginacion.de(50, 50).first());
    }

    @ParameterizedTest
    @CsvSource({"-1,50", "-100,10"})
    void rechazaFirstNegativo(int first, int max) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Paginacion.de(first, max));

        assertEquals("El parametro 'first' debe ser mayor o igual que 0", e.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"0,0", "0,-5", "0,101", "10,1000"})
    void rechazaMaxFueraDeUnoACien(int first, int max) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Paginacion.de(first, max));

        assertEquals("El parametro 'max' debe estar entre 1 y 100", e.getMessage());
    }

    @Test
    void losValoresPorDefectoSonCeroYCincuenta() {
        assertEquals(0, Paginacion.PRIMERO_POR_DEFECTO);
        assertEquals(50, Paginacion.MAX_POR_DEFECTO);
        assertEquals(100, Paginacion.MAX_PERMITIDO);
    }
}
