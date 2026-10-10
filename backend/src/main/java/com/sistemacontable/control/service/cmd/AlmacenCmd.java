package com.sistemacontable.control.service.cmd;

/** Datos de un almacen. {@code estado} nulo: habilitado al crear, conservar al modificar. */
public record AlmacenCmd(String nombre, String descripcion, Boolean estado) {
}
