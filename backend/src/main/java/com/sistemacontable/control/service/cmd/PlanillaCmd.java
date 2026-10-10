package com.sistemacontable.control.service.cmd;

import java.time.OffsetDateTime;

/** Datos de una planilla. Debe cumplirse {@code fechaInicio <= fechaFin <= fechaPago}. */
public record PlanillaCmd(OffsetDateTime fechaInicio, OffsetDateTime fechaFin, OffsetDateTime fechaPago,
        String descripcion) {
}
