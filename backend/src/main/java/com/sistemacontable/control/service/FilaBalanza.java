package com.sistemacontable.control.service;

import com.sistemacontable.entity.enums.NaturalezaCuenta;
import java.math.BigDecimal;

/**
 * Fila de la balanza de comprobacion. Para cuentas con hijas, cargos y abonos incluyen los de sus descendientes.
 * El saldo es {@code cargos - abonos} en cuentas deudoras y {@code abonos - cargos} en las acreedoras; es negativo
 * cuando la cuenta tiene saldo contrario a su naturaleza.
 */
public record FilaBalanza(Long cuentaId, Integer codigo, String nombre, NaturalezaCuenta naturaleza, boolean imputable,
        BigDecimal cargos, BigDecimal abonos, BigDecimal saldo) {
}
