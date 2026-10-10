package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.TipoProductoCmd;
import jakarta.validation.constraints.NotBlank;

/** Datos para crear o modificar un tipo de producto ({@code padreId} nulo = raiz). */
public record TipoProductoRequest(@NotBlank String nombre,
        Long padreId) {

    public TipoProductoCmd aCmd() {
        return new TipoProductoCmd(nombre, padreId);
    }
}
