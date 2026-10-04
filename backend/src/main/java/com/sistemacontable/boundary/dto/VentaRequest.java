package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.LineaVentaCmd;
import com.sistemacontable.control.service.cmd.VentaCmd;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Orden de registrar una venta (todo por ids). {@code fecha} nula equivale a hoy; el precio no incluye IVA. */
public record VentaRequest(@NotNull Long clienteId,
        @NotNull Long empleadoId,
        LocalDate fecha,
        String descripcion,
        @Valid List<@NotNull Linea> lineas) {

    /** Linea del documento. */
    public record Linea(@NotNull Long loteId, @NotNull Long almacenId, @NotNull BigDecimal cantidad, @NotNull BigDecimal precio) {

        LineaVentaCmd aCmd() {
            return new LineaVentaCmd(loteId, almacenId, cantidad, precio);
        }
    }

    public VentaCmd aCmd() {
        return new VentaCmd(clienteId, empleadoId, fecha, descripcion,
                lineas == null ? null : lineas.stream().map(Linea::aCmd).toList());
    }
}
