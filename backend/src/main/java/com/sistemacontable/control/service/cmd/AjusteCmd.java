package com.sistemacontable.control.service.cmd;

import java.math.BigDecimal;

/**
 * Ajuste de inventario de un lote en un almacen. {@code cantidad} lleva el signo explicito: positiva = entrada de
 * ajuste, negativa = salida de ajuste; cero no es valido. {@code motivo} es obligatorio.
 */
public record AjusteCmd(Long almacenId, Long loteId, BigDecimal cantidad, String motivo) {
}
