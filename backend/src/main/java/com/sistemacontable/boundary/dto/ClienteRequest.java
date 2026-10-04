package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.ClienteCmd;
import jakarta.validation.constraints.NotBlank;

/** Datos para crear o modificar un cliente (NIT obligatorio y unico). */
public record ClienteRequest(@NotBlank String nombre,
        String descripcion,
        @NotBlank String nit) {

    public ClienteCmd aCmd() {
        return new ClienteCmd(nombre, descripcion, nit);
    }
}
