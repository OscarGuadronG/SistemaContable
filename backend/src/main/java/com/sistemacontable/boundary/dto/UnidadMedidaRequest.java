package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.UnidadMedidaCmd;
import jakarta.validation.constraints.NotBlank;

/** Datos para crear o modificar una unidad de medida. */
public record UnidadMedidaRequest(@NotBlank String nombre,
        @NotBlank String abrev) {

    public UnidadMedidaCmd aCmd() {
        return new UnidadMedidaCmd(nombre, abrev);
    }
}
