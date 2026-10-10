package com.sistemacontable.control.service.cmd;

import java.math.BigDecimal;

/** Linea de un traslado: lote y cantidad (mayor que cero). */
public record LineaTrasladoCmd(Long loteId, BigDecimal cantidad) {
}
