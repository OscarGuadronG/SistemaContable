package com.sistemacontable.control.service.cmd;

/** Datos de un proveedor; el NIT es obligatorio y unico. */
public record ProveedorCmd(String nombre, String descripcion, String nit) {
}
