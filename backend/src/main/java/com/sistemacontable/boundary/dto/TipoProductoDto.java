package com.sistemacontable.boundary.dto;

/** Tipo de producto ({@code padreId} nulo = tipo raiz). */
public record TipoProductoDto(Long id,
        String nombre,
        Long padreId) {
}
