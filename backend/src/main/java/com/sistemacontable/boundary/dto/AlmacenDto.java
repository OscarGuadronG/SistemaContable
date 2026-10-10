package com.sistemacontable.boundary.dto;

/** Almacen ({@code estado} true = habilitado). */
public record AlmacenDto(Long id,
        String nombre,
        String descripcion,
        Boolean estado) {
}
