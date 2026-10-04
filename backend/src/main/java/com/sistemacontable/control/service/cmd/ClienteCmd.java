package com.sistemacontable.control.service.cmd;

/** Datos de un cliente; el NIT es obligatorio y unico. */
public record ClienteCmd(String nombre, String descripcion, String nit) {
}
