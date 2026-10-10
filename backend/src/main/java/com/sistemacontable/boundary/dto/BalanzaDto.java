package com.sistemacontable.boundary.dto;

import com.sistemacontable.entity.enums.NaturalezaCuenta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Balanza de comprobacion de un rango de fechas. */
public record BalanzaDto(LocalDate desde,
        LocalDate hasta,
        List<Fila> filas,
        BigDecimal totalCargos,
        BigDecimal totalAbonos,
        Boolean cuadra,
        BigDecimal diferencia) {

    /** Fila de la balanza (las cuentas con hijas incluyen los movimientos de sus descendientes). */
    public record Fila(Long cuentaId, Integer codigo, String nombre, NaturalezaCuenta naturaleza, Boolean imputable,
            BigDecimal cargos, BigDecimal abonos, BigDecimal saldo) {
    }
}
