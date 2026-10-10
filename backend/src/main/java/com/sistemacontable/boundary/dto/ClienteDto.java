package com.sistemacontable.boundary.dto;

/** Cliente. */
public record ClienteDto(Long id,
        String nombre,
        String descripcion,
        String nit) {
}
