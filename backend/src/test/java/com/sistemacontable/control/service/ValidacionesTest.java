package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.control.exception.ReglaNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ValidacionesTest {

    @Test
    void textoObligatorioDevuelveElTextoSinEspacios() {
        assertEquals("Ana", Validaciones.texto("  Ana ", "nombre"));
    }

    @Test
    void textoObligatorioNombraElCampoSiFalta() {
        for (String vacio : new String[] {null, "", "   "}) {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> Validaciones.texto(vacio, "NIT"));
            assertTrue(ex.getMessage().contains("NIT"), ex.getMessage());
        }
    }

    @Test
    void textoOpcionalConvierteVacioANulo() {
        assertNull(Validaciones.textoOpcional(null));
        assertNull(Validaciones.textoOpcional("  "));
        assertEquals("x", Validaciones.textoOpcional(" x "));
    }

    @Test
    void requerirRechazaNulos() {
        assertEquals(5, Validaciones.requerir(5, "valor"));
        assertThrows(ReglaNegocioException.class, () -> Validaciones.requerir(null, "valor"));
    }

    @Test
    void noNegativoAceptaCeroYRechazaNegativosONulos() {
        assertEquals(BigDecimal.ZERO, Validaciones.noNegativo(BigDecimal.ZERO, "salario"));
        assertThrows(ReglaNegocioException.class, () -> Validaciones.noNegativo(new BigDecimal("-0.01"), "salario"));
        assertThrows(ReglaNegocioException.class, () -> Validaciones.noNegativo(null, "salario"));
    }
}
