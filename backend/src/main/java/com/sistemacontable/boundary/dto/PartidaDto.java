package com.sistemacontable.boundary.dto;

import com.sistemacontable.entity.enums.TipoPartida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Partida contable. {@code lineas} es nulo (y no se serializa) en los listados. */
public record PartidaDto(Long id,
        LocalDate fecha,
        String concepto,
        TipoPartida tipo,
        Long compraId,
        Long ventaId,
        Long terceroId,
        List<Linea> lineas) {

    /** Linea de una partida: exactamente uno de cargo o abono es mayor que cero. */
    public record Linea(Long id, Long cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
