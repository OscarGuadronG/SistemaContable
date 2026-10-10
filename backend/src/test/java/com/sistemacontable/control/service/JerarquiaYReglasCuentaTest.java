package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import org.junit.jupiter.api.Test;

/** Reglas puras de jerarquia y de codigos de cuenta (sin mocks). */
class JerarquiaYReglasCuentaTest {

    private static Cuenta cuenta(long id, int codigo, Cuenta padre) {
        Cuenta c = Fabrica.cuenta(id, codigo, NaturalezaCuenta.Deudora, true, false);
        c.setPadre(padre);
        return c;
    }

    // ------------------------------------------------------------------------------------- Jerarquia

    @Test
    void asignarComoPadreASiMismoEsCiclo() {
        Cuenta a = cuenta(1, 1, null);

        assertTrue(Jerarquia.hayCiclo(a, a, Cuenta::getPadre));
    }

    @Test
    void asignarUnDescendienteComoPadreEsCiclo() {
        Cuenta raiz = cuenta(1, 1, null);
        Cuenta hijo = cuenta(2, 11, raiz);
        Cuenta nieto = cuenta(3, 111, hijo);

        assertTrue(Jerarquia.hayCiclo(raiz, hijo, Cuenta::getPadre));
        assertTrue(Jerarquia.hayCiclo(raiz, nieto, Cuenta::getPadre));
    }

    @Test
    void asignarUnaRamaAjenaOUnAncestroNoEsCiclo() {
        Cuenta raiz = cuenta(1, 1, null);
        Cuenta hijo = cuenta(2, 11, raiz);
        Cuenta otra = cuenta(3, 2, null);

        assertFalse(Jerarquia.hayCiclo(hijo, otra, Cuenta::getPadre));
        assertFalse(Jerarquia.hayCiclo(hijo, raiz, Cuenta::getPadre));
        assertFalse(Jerarquia.hayCiclo(hijo, null, Cuenta::getPadre));
    }

    @Test
    void unCicloPreexistenteEnLosDatosSeRechazaEnVezDeColgarse() {
        Cuenta a = cuenta(1, 1, null);
        Cuenta b = cuenta(2, 11, a);
        a.setPadre(b);
        Cuenta nueva = cuenta(3, 5, null);

        assertTrue(Jerarquia.hayCiclo(nueva, a, Cuenta::getPadre));
    }

    // --------------------------------------------------------------------------------- ReglasCuenta

    @Test
    void elCodigoDelHijoEmpiezaPorElDelPadreYEsMasLargo() {
        Cuenta padre = cuenta(1, 11, null);

        assertTrue(ReglasCuenta.codigoCompatibleConPadre(1101, padre));
        assertTrue(ReglasCuenta.codigoCompatibleConPadre(111, padre));
        assertFalse(ReglasCuenta.codigoCompatibleConPadre(21, padre));
        assertFalse(ReglasCuenta.codigoCompatibleConPadre(1, padre));
        assertFalse(ReglasCuenta.codigoCompatibleConPadre(11, padre));
        assertFalse(ReglasCuenta.codigoCompatibleConPadre(101, padre));
    }
}
