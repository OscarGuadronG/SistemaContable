package com.sistemacontable.control.service;

import com.sistemacontable.control.exception.ReglaNegocioException;
import java.math.BigDecimal;

/**
 * Reparto del total de una compra o venta entre contado y credito. La forma de pago NO se guarda en la base de datos:
 * viaja en la orden de cada operacion como {@code porcentajeContado} (0 a 100, ambos incluidos) y solo decide con que
 * cuentas se arma la partida.
 *
 * <p>El contado es {@code total x porcentajeContado / 100} redondeado una sola vez (HALF_UP, 2 decimales, via
 * {@link Dinero}) y el credito es {@code total - contado}, de modo que {@code contado + credito == total} exacto y la
 * partida cuadra al centavo aunque el porcentaje no divida el total.</p>
 */
public record PagoDocumento(BigDecimal contado, BigDecimal credito) {

    private static final BigDecimal CIEN = new BigDecimal("100");

    /**
     * Valida el porcentaje de contado.
     *
     * @throws ReglaNegocioException si es nulo o esta fuera de [0, 100]
     */
    public static void validarPorcentaje(BigDecimal porcentajeContado) {
        if (porcentajeContado == null) {
            throw new ReglaNegocioException("El porcentaje de contado es obligatorio (0 a 100)");
        }
        if (porcentajeContado.signum() < 0 || porcentajeContado.compareTo(CIEN) > 0) {
            throw new ReglaNegocioException("El porcentaje de contado debe estar entre 0 y 100, y se recibio "
                    + porcentajeContado.toPlainString());
        }
    }

    /**
     * Reparte {@code total} (ya con IVA) segun el porcentaje.
     *
     * @throws ReglaNegocioException si el porcentaje es nulo o esta fuera de rango
     */
    public static PagoDocumento dividir(BigDecimal total, BigDecimal porcentajeContado) {
        validarPorcentaje(porcentajeContado);
        BigDecimal totalRedondeado = Dinero.redondear(total);
        BigDecimal contado = Dinero.multiplicar(totalRedondeado, porcentajeContado.movePointLeft(2));
        return new PagoDocumento(contado, totalRedondeado.subtract(contado));
    }
}
