package com.sistemacontable.control.service;

import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Cuenta;
import java.math.BigDecimal;
import java.util.List;

/**
 * Reglas puras de la partida doble (sin acceso a datos): al menos dos lineas; cada linea con exactamente uno de
 * cargo/abono mayor que cero; cuentas activas e imputables; suma de cargos igual a suma de abonos.
 */
public final class ReglasPartida {

    private ReglasPartida() {
    }

    /**
     * Valida las lineas de una partida.
     *
     * @throws ReglaNegocioException ante la primera regla violada
     */
    public static void validar(List<LineaAsiento> lineas) {
        if (lineas == null || lineas.size() < 2) {
            throw new ReglaNegocioException("La partida debe tener al menos dos lineas");
        }
        BigDecimal cargos = BigDecimal.ZERO;
        BigDecimal abonos = BigDecimal.ZERO;
        int numero = 0;
        for (LineaAsiento linea : lineas) {
            numero++;
            if (linea == null) {
                throw new ReglaNegocioException("La linea " + numero + " de la partida es nula");
            }
            validarLinea(linea, numero);
            cargos = cargos.add(linea.cargo());
            abonos = abonos.add(linea.abono());
        }
        if (cargos.compareTo(abonos) != 0) {
            throw new ReglaNegocioException("Partida descuadrada: cargos " + Dinero.redondear(cargos)
                    + " distinto de abonos " + Dinero.redondear(abonos));
        }
    }

    /**
     * Exige que la cuenta este activa y sea imputable.
     *
     * @param etiqueta como nombrar la cuenta en el mensaje (p. ej. la clave de configuracion)
     */
    public static void validarCuentaImputable(Cuenta cuenta, String etiqueta) {
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new ReglaNegocioException("La cuenta " + etiqueta + " (codigo " + cuenta.getCodigo()
                    + ") debe estar activa");
        }
        if (!Boolean.TRUE.equals(cuenta.getImputable())) {
            throw new ReglaNegocioException("La cuenta " + etiqueta + " (codigo " + cuenta.getCodigo()
                    + ") debe ser imputable");
        }
    }

    private static void validarLinea(LineaAsiento linea, int numero) {
        String donde = "linea " + numero;
        if (linea.cuenta() == null) {
            throw new ReglaNegocioException("La " + donde + " de la partida no tiene cuenta");
        }
        if (linea.cargo() == null || linea.abono() == null) {
            throw new ReglaNegocioException("La " + donde + " de la partida tiene un importe nulo");
        }
        if (linea.cargo().signum() < 0 || linea.abono().signum() < 0) {
            throw new ReglaNegocioException("La " + donde + " de la partida tiene un importe negativo");
        }
        boolean hayCargo = linea.cargo().signum() > 0;
        boolean hayAbono = linea.abono().signum() > 0;
        if (hayCargo && hayAbono) {
            throw new ReglaNegocioException("La " + donde + " de la partida no puede tener cargo y abono a la vez");
        }
        if (!hayCargo && !hayAbono) {
            throw new ReglaNegocioException("La " + donde + " de la partida debe tener cargo o abono mayor que cero");
        }
        validarCuentaImputable(linea.cuenta(), "de la " + donde);
    }
}
