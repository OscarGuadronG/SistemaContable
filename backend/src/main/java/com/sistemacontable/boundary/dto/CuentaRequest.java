package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.CuentaCmd;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos para crear o modificar una cuenta ({@code naturaleza} se hereda del padre si se omite). */
public record CuentaRequest(@NotNull Integer codigo,
        @NotBlank String nombre,
        NaturalezaCuenta naturaleza,
        Boolean imputable,
        Long padreId) {

    public CuentaCmd aCmd() {
        return new CuentaCmd(codigo, nombre, naturaleza, imputable, padreId);
    }
}
