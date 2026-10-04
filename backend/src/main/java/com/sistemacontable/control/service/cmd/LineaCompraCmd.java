package com.sistemacontable.control.service.cmd;

import java.math.BigDecimal;

/** Linea de una compra: lote y almacen de destino por id, cantidad (mayor que cero) y precio unitario (&gt;= 0). */
public record LineaCompraCmd(Long loteId, Long almacenId, BigDecimal cantidad, BigDecimal precio) {
}
