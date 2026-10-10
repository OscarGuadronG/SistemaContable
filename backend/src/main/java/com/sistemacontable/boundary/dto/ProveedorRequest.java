package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.ProveedorCmd;
import jakarta.validation.constraints.NotBlank;

/** Datos para crear o modificar un proveedor (NIT obligatorio y unico). */
public record ProveedorRequest(@NotBlank String nombre,
        String descripcion,
        @NotBlank String nit) {

    public ProveedorCmd aCmd() {
        return new ProveedorCmd(nombre, descripcion, nit);
    }
}
