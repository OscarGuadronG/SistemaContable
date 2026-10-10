package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.ProductoCmd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos para crear o modificar un producto. */
public record ProductoRequest(@NotBlank String nombre,
        @NotNull Long unidadMedidaId,
        @NotNull Long tipoId,
        String descripcion) {

    public ProductoCmd aCmd() {
        return new ProductoCmd(nombre, unidadMedidaId, tipoId, descripcion);
    }
}
