package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.LineaTrasladoCmd;
import com.sistemacontable.control.service.cmd.TrasladoCmd;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** Orden de trasladar stock de un almacen a otro (distintos). */
public record TrasladoRequest(@NotNull Long almacenOrigenId,
        @NotNull Long almacenDestinoId,
        String descripcion,
        @Valid List<@NotNull Linea> lineas) {

    /** Linea del documento. */
    public record Linea(@NotNull Long loteId, @NotNull BigDecimal cantidad) {

        LineaTrasladoCmd aCmd() {
            return new LineaTrasladoCmd(loteId, cantidad);
        }
    }

    public TrasladoCmd aCmd() {
        return new TrasladoCmd(almacenOrigenId, almacenDestinoId, descripcion,
                lineas == null ? null : lineas.stream().map(Linea::aCmd).toList());
    }
}
