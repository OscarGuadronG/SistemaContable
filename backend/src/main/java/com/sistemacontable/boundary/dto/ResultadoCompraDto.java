package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de registrar una compra; {@code partidaId} es nulo si el total es cero. */
public record ResultadoCompraDto(CompraDto compra,
        List<KardexDto> movimientos,
        Long partidaId,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total) {
}
