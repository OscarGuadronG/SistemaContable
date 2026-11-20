package com.sistemacontable.control.service;

import com.sistemacontable.entity.Cuenta;

/** Reglas puras del catalogo de cuentas. */
public final class ReglasCuenta {

    private ReglasCuenta() {
    }

    /**
     * El código de una hija debe llevar el prefijo del padre y la longitud del nivel siguiente: 1, 2, 4 o 5 dígitos.
     */
    public static boolean codigoCompatibleConPadre(Integer codigo, Cuenta padre) {
        return codigoCompatible(codigo, padre.getCodigo());
    }

    /** Igual que {@link #codigoCompatibleConPadre(Integer, Cuenta)} pero con el codigo del padre ya conocido. */
    public static boolean codigoCompatible(Integer codigoHijo, Integer codigoPadre) {
        String hijo = String.valueOf(codigoHijo);
        String prefijo = String.valueOf(codigoPadre);
        int longitudSiguiente = switch (prefijo.length()) { case 1 -> 2; case 2 -> 4; case 4 -> 5; default -> -1; };
        return hijo.length() == longitudSiguiente && hijo.startsWith(prefijo);
    }

    public static boolean codigoDeNivelValido(Integer codigo) {
        if(codigo==null||codigo<=0) return false;
        int longitud=String.valueOf(codigo).length();
        return longitud==1||longitud==2||longitud==4||longitud==5;
    }
}
