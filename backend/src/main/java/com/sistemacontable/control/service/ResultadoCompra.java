package com.sistemacontable.control.service;

import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.Partida;
import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de registrar una compra. {@code partida} es nula cuando el importe total es cero (no hay nada que
 * contabilizar).
 */
public record ResultadoCompra(Compra compra, List<CompraDetalle> detalles, List<MovimientoKardex> movimientos,
        Partida partida, BigDecimal subtotal, BigDecimal iva, BigDecimal total) {

    public ResultadoCompra {
        detalles = List.copyOf(detalles);
        movimientos = List.copyOf(movimientos);
    }
}
