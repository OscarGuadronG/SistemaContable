package com.sistemacontable.entity.enums;

/**
 * Enum de Postgres naturaleza_cuenta. Las constantes se llaman exactamente como las etiquetas de la BD,
 * porque se persisten con {@code @Enumerated(EnumType.STRING)}.
 */
public enum NaturalezaCuenta {
    Acreedora,
    Deudora
}
