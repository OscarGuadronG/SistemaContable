package com.sistemacontable.control.service.cmd;

import com.sistemacontable.entity.enums.EstadoEmpleado;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de un empleado. {@code salario} es obligatorio (&gt;= 0). {@code estado} nulo equivale a Activo al crear y a
 * conservar el actual al modificar.
 */
public record EmpleadoCmd(String nombre, String apellido, LocalDate fechNac, Long departamentoId, BigDecimal salario,
        EstadoEmpleado estado, String numeroIsss, String dui) {
}
