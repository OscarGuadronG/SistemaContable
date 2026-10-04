package com.sistemacontable.control.service;

import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de registrar una venta. {@code partida} es nula cuando ni el ingreso ni el costo tienen importe.
 * {@code costoVentas} es el costo de lo vendido (costo promedio ponderado por lote).
 */
public record ResultadoVenta(Venta venta, List<VentaDetalle> detalles, List<MovimientoKardex> movimientos,
        Partida partida, BigDecimal subtotal, BigDecimal iva, BigDecimal total, BigDecimal costoVentas) {

    public ResultadoVenta {
        detalles = List.copyOf(detalles);
        movimientos = List.copyOf(movimientos);
    }
}
