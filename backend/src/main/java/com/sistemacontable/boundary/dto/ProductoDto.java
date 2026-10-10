package com.sistemacontable.boundary.dto;

/** Producto. */
public record ProductoDto(Long id,
        String nombre,
        Long unidadMedidaId,
        Long tipoId,
        String descripcion) {
}
