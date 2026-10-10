package com.sistemacontable.control.service.cmd;

import java.math.BigDecimal;

/** Linea de una partida pedida desde fuera: cuenta por id y un solo importe (el otro puede ser nulo). */
public record LineaPartidaCmd(Long cuentaId, BigDecimal cargo, BigDecimal abono) {
}
