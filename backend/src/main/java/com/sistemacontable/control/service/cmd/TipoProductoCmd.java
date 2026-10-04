package com.sistemacontable.control.service.cmd;

/** Datos de un tipo de producto; {@code padreId} nulo = tipo raiz. */
public record TipoProductoCmd(String nombre, Long padreId) {
}
