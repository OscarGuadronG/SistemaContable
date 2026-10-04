package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.LineaPartidaCmd;
import com.sistemacontable.control.service.cmd.PartidaCmd;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Orden de crear una partida. {@code compraId} y {@code ventaId} son excluyentes; cada linea lleva un solo importe mayor que cero. */
public record PartidaRequest(@NotNull LocalDate fecha,
        @NotBlank String concepto,
        @NotNull TipoPartida tipo,
        Long compraId,
        Long ventaId,
        Long terceroId,
        @Valid List<@NotNull Linea> lineas) {

    /** Linea del documento. */
    public record Linea(@NotNull Long cuentaId, BigDecimal cargo, BigDecimal abono) {

        LineaPartidaCmd aCmd() {
            return new LineaPartidaCmd(cuentaId, cargo, abono);
        }
    }

    public PartidaCmd aCmd() {
        return new PartidaCmd(fecha, concepto, tipo, compraId, ventaId, terceroId,
                lineas == null ? null : lineas.stream().map(Linea::aCmd).toList());
    }
}
