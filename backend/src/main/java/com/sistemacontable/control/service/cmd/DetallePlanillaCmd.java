package com.sistemacontable.control.service.cmd;

/**
 * Datos del detalle de planilla de un empleado. {@code dias} nulo equivale a los dias del mes configurados (30 por
 * defecto), las horas nulas a cero. Al modificar un detalle, {@code empleadoId} debe ser nulo o el mismo.
 */
public record DetallePlanillaCmd(Long empleadoId, Integer dias, Integer horasxDiur, Integer horasxNoct) {
}
