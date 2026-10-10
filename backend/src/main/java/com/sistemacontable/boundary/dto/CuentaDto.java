package com.sistemacontable.boundary.dto;

import com.sistemacontable.entity.enums.NaturalezaCuenta;

/** Cuenta contable ({@code padreId} nulo = cuenta raiz). */
public record CuentaDto(Long id,
        Integer codigo,
        String nombre,
        NaturalezaCuenta naturaleza,
        Boolean activo,
        Boolean imputable,
        Long padreId) {
}
