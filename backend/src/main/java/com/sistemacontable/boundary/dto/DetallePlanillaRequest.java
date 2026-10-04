package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.DetallePlanillaCmd;

/** Datos del detalle de un empleado en una planilla (dias y horas nulos toman el valor por defecto o el actual). */
public record DetallePlanillaRequest(Long empleadoId,
        Integer dias,
        Integer horasxDiur,
        Integer horasxNoct) {

    public DetallePlanillaCmd aCmd() {
        return new DetallePlanillaCmd(empleadoId, dias, horasxDiur, horasxNoct);
    }
}
