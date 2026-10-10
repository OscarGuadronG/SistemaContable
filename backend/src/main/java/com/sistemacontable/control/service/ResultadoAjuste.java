package com.sistemacontable.control.service;

import com.sistemacontable.entity.Partida;

/** Resultado de un ajuste: el movimiento de kardex y la partida (nula si no se contabiliza). */
public record ResultadoAjuste(MovimientoKardex movimiento, Partida partida) {
}
