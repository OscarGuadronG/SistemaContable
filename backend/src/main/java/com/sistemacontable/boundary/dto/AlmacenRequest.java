package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.AlmacenCmd;
import jakarta.validation.constraints.NotBlank;

/** Datos para crear o modificar un almacen ({@code estado} nulo: habilitado al crear, conservar al modificar). */
public record AlmacenRequest(@NotBlank String nombre,
        String descripcion,
        Boolean estado) {

    public AlmacenCmd aCmd() {
        return new AlmacenCmd(nombre, descripcion, estado);
    }
}
