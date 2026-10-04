package com.sistemacontable.control.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Balanza de comprobacion de un rango de fechas. Los totales suman los movimientos directos de las partidas (sin
 * doble conteo de las filas agregadas de cuentas padre); si no coinciden, {@link #cuadra()} es falso y
 * {@link #diferencia()} indica cuanto (senal de datos inconsistentes: una partida siempre se crea cuadrada).
 */
public record Balanza(LocalDate desde, LocalDate hasta, List<FilaBalanza> filas, BigDecimal totalCargos,
        BigDecimal totalAbonos) {

    public Balanza {
        filas = List.copyOf(filas);
    }

    public boolean cuadra() {
        return totalCargos.compareTo(totalAbonos) == 0;
    }

    /** Cargos menos abonos totales. */
    public BigDecimal diferencia() {
        return Dinero.redondear(totalCargos.subtract(totalAbonos));
    }
}
