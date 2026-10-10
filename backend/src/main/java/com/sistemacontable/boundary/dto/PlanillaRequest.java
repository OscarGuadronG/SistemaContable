package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.PlanillaCmd;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** Datos para crear o modificar una planilla: fechaInicio &lt;= fechaFin &lt;= fechaPago. */
public record PlanillaRequest(@NotNull OffsetDateTime fechaInicio,
        @NotNull OffsetDateTime fechaFin,
        @NotNull OffsetDateTime fechaPago,
        String descripcion) {

    public PlanillaCmd aCmd() {
        return new PlanillaCmd(fechaInicio, fechaFin, fechaPago, descripcion);
    }
}
