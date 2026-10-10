package com.sistemacontable.control.service.cmd;

import com.sistemacontable.entity.enums.NaturalezaCuenta;

/**
 * Datos para crear o modificar una cuenta. {@code naturaleza} puede omitirse en cuentas con padre (se hereda) y, al
 * modificar, para conservar la actual; {@code imputable} nulo equivale a falso al crear y a conservar al modificar.
 */
public record CuentaCmd(Integer codigo, String nombre, NaturalezaCuenta naturaleza, Boolean imputable, Long padreId) {
}
