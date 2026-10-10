package com.sistemacontable.boundary.dto;

/** Resultado de un ajuste de inventario; {@code partidaId} es nulo si no se contabiliza. */
public record ResultadoAjusteDto(KardexDto movimiento,
        Long partidaId) {
}
