package com.sistemacontable.control.service;

import java.math.BigDecimal;

/** Subtotal, IVA y total de una compra o venta, cada uno redondeado una sola vez a 2 decimales. */
public record TotalesDocumento(BigDecimal subtotal, BigDecimal iva, BigDecimal total) {

    /**
     * @param subtotalSinRedondear suma de cantidad x precio de las lineas, a precision completa
     * @param tasaIva por ejemplo 0.13; el IVA se calcula sobre el subtotal ya redondeado
     */
    public static TotalesDocumento calcular(BigDecimal subtotalSinRedondear, BigDecimal tasaIva) {
        BigDecimal subtotal = Dinero.redondear(subtotalSinRedondear);
        BigDecimal iva = Dinero.multiplicar(subtotal, tasaIva);
        return new TotalesDocumento(subtotal, iva, subtotal.add(iva));
    }
}
