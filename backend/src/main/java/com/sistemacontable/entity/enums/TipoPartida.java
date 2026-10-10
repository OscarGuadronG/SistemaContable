package com.sistemacontable.entity.enums;

/**
 * Enum de Postgres tipo_partida. Las constantes se llaman exactamente como las etiquetas de la BD,
 * porque se persisten con {@code @Enumerated(EnumType.STRING)}.
 */
public enum TipoPartida {
    Diario,
    Ingreso,
    Egreso,
    Ajuste,
    Cierre
}
