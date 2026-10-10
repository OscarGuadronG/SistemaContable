package com.sistemacontable.control.service.cmd;

import java.math.BigDecimal;

/** Linea de una venta: lote y almacen de origen por id, cantidad (mayor que cero) y precio unitario (&gt;= 0, sin IVA). */
public record LineaVentaCmd(Long loteId, Long almacenId, BigDecimal cantidad, BigDecimal precio) {
}
