package com.sistemacontable.control.service.cmd;

import java.time.LocalDate;

/**
 * Datos de un lote. Al modificar, {@code productoId} debe ser nulo o el mismo (un lote no cambia de producto).
 */
public record LoteCmd(Long productoId, String numLote, LocalDate fechCaducidad) {
}
