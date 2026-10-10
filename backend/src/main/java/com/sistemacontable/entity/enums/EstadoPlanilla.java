package com.sistemacontable.entity.enums;

/**
 * Enum de Postgres estado_planilla. Las constantes se llaman exactamente como las etiquetas de la BD,
 * porque se persisten con {@code @Enumerated(EnumType.STRING)}.
 */
public enum EstadoPlanilla {
    Pendiente,
    Pagado
}
