package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.TerceroCmd;
import jakarta.validation.constraints.NotBlank;

/** Datos para crear o modificar un tercero. */
public record TerceroRequest(@NotBlank String nombre,
        String descripcion) {

    public TerceroCmd aCmd() {
        return new TerceroCmd(nombre, descripcion);
    }
}
