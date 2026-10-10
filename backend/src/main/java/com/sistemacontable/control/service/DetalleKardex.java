package com.sistemacontable.control.service;

import com.sistemacontable.entity.Lote;
import java.math.BigDecimal;

/** Linea de un movimiento de kardex: lote, cantidad (siempre positiva) y costo unitario. */
public record DetalleKardex(Lote lote, BigDecimal cantidad, BigDecimal precioCosto) {
}
