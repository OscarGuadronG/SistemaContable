package com.sistemacontable.control.service.cmd;

/** Datos de un producto. */
public record ProductoCmd(String nombre, Long unidadMedidaId, Long tipoId, String descripcion) {
}
