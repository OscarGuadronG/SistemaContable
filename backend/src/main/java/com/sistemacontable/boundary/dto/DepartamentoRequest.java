package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.DepartamentoCmd;
import jakarta.validation.constraints.NotBlank;

/** Datos para crear o modificar un departamento. */
public record DepartamentoRequest(@NotBlank String nombre,
        String descripcion) {

    public DepartamentoCmd aCmd() {
        return new DepartamentoCmd(nombre, descripcion);
    }
}
