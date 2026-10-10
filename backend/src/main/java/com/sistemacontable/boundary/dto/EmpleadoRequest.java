package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.EmpleadoCmd;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Datos para crear o modificar un empleado ({@code estado} nulo: Activo al crear, conservar al modificar). */
public record EmpleadoRequest(@NotBlank String nombre,
        @NotBlank String apellido,
        LocalDate fechNac,
        @NotNull Long departamentoId,
        @NotNull BigDecimal salario,
        EstadoEmpleado estado,
        @NotBlank String numeroIsss,
        @NotBlank String dui) {

    public EmpleadoCmd aCmd() {
        return new EmpleadoCmd(nombre, apellido, fechNac, departamentoId, salario, estado, numeroIsss, dui);
    }
}
