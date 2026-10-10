package com.sistemacontable.boundary.dto;

import com.sistemacontable.entity.enums.EstadoEmpleado;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Empleado. */
public record EmpleadoDto(Long id,
        String nombre,
        String apellido,
        LocalDate fechNac,
        Long departamentoId,
        BigDecimal salario,
        EstadoEmpleado estado,
        String numeroIsss,
        String dui) {
}
