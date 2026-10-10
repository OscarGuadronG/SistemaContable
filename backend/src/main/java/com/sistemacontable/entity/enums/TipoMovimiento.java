package com.sistemacontable.entity.enums;

/**
 * Enum de Postgres tipo_movimiento. Las constantes se llaman exactamente como las etiquetas de la BD,
 * porque se persisten con {@code @Enumerated(EnumType.STRING)}.
 */
public enum TipoMovimiento {
    Entrada,
    Salida,
    Traslado,
    Ajuste
}
