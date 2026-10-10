package com.sistemacontable.control.service;

import com.sistemacontable.entity.Cuenta;
import java.math.BigDecimal;

/**
 * Linea de una partida ya resuelta (cuenta cargada), usada entre servicios al contabilizar. Los comandos que
 * llegan desde fuera usan ids ({@code LineaPartidaCmd}); esta forma con entidad es solo interna.
 */
public record LineaAsiento(Cuenta cuenta, BigDecimal cargo, BigDecimal abono) {

    /** Linea al debe. */
    public static LineaAsiento cargo(Cuenta cuenta, BigDecimal monto) {
        return new LineaAsiento(cuenta, monto, BigDecimal.ZERO);
    }

    /** Linea al haber. */
    public static LineaAsiento abono(Cuenta cuenta, BigDecimal monto) {
        return new LineaAsiento(cuenta, BigDecimal.ZERO, monto);
    }
}
