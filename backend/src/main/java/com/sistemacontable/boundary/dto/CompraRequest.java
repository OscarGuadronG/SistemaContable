package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.CompraCmd;
import com.sistemacontable.control.service.cmd.LineaCompraCmd;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Orden de registrar una compra (todo por ids). {@code fecha} nula equivale a hoy. */
public record CompraRequest(@NotNull Long proveedorId,
        @NotNull Long empleadoId,
        LocalDate fecha,
        String descripcion,
        @Valid List<@NotNull Linea> lineas) {

    /** Linea del documento. */
    public record Linea(@NotNull Long loteId, @NotNull Long almacenId, @NotNull BigDecimal cantidad, @NotNull BigDecimal precio) {

        LineaCompraCmd aCmd() {
            return new LineaCompraCmd(loteId, almacenId, cantidad, precio);
        }
    }

    public CompraCmd aCmd() {
        return new CompraCmd(proveedorId, empleadoId, fecha, descripcion,
                lineas == null ? null : lineas.stream().map(Linea::aCmd).toList());
    }
}
