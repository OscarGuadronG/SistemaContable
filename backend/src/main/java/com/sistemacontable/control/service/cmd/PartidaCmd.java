package com.sistemacontable.control.service.cmd;

import com.sistemacontable.entity.enums.TipoPartida;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orden de crear una partida. {@code compraId} y {@code ventaId} son excluyentes; {@code terceroId} es opcional.
 */
public record PartidaCmd(LocalDate fecha, String concepto, TipoPartida tipo, Long compraId, Long ventaId,
        Long terceroId, List<LineaPartidaCmd> lineas) {

    public PartidaCmd {
        lineas = lineas == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(lineas));
    }
}
