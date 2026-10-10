package com.sistemacontable.control.service;

import com.sistemacontable.entity.Cuenta;

/** Reglas puras del catalogo de cuentas. */
public final class ReglasCuenta {

    private ReglasCuenta() {
    }

    /**
     * El codigo de una cuenta hija debe empezar por el codigo del padre y ser mas largo que el (p. ej. padre 11,
     * hijas 111 o 1101). La comparacion es sobre la representacion decimal de los enteros.
     */
    public static boolean codigoCompatibleConPadre(Integer codigo, Cuenta padre) {
        return codigoCompatible(codigo, padre.getCodigo());
    }

    /** Igual que {@link #codigoCompatibleConPadre(Integer, Cuenta)} pero con el codigo del padre ya conocido. */
    public static boolean codigoCompatible(Integer codigoHijo, Integer codigoPadre) {
        String hijo = String.valueOf(codigoHijo);
        String prefijo = String.valueOf(codigoPadre);
        return hijo.length() > prefijo.length() && hijo.startsWith(prefijo);
    }
}
