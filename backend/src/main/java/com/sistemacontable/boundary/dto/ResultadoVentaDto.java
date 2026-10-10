package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de registrar una venta; {@code costoVentas} es el costo promedio ponderado de lo vendido. */
public record ResultadoVentaDto(VentaDto venta,
        List<KardexDto> movimientos,
        Long partidaId,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        BigDecimal costoVentas) {
}
