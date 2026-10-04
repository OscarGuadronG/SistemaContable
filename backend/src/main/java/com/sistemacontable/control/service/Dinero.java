package com.sistemacontable.control.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Unico lugar donde se redondea dinero: {@link RoundingMode#HALF_UP} a 2 decimales para importes y a 4 decimales
 * para costos unitarios (promedios ponderados). Los calculos intermedios se hacen con precision completa y se
 * redondea una sola vez al producir el resultado monetario.
 *
 * <p>Para comparar importes use siempre {@link BigDecimal#compareTo}, nunca {@code equals}.</p>
 */
public final class Dinero {

    public static final int ESCALA = 2;
    public static final int ESCALA_COSTO = 4;
    public static final RoundingMode MODO = RoundingMode.HALF_UP;

    private Dinero() {
    }

    /** Redondea un importe a 2 decimales (HALF_UP). */
    public static BigDecimal redondear(BigDecimal valor) {
        return requerir(valor).setScale(ESCALA, MODO);
    }

    /** Redondea un costo unitario a 4 decimales (HALF_UP). */
    public static BigDecimal redondearCosto(BigDecimal valor) {
        return requerir(valor).setScale(ESCALA_COSTO, MODO);
    }

    /** {@code a * b} redondeado una sola vez a 2 decimales. */
    public static BigDecimal multiplicar(BigDecimal a, BigDecimal b) {
        return redondear(requerir(a).multiply(requerir(b)));
    }

    /** Suma ignorando nulos (como cero) y redondea el total a 2 decimales. */
    public static BigDecimal suma(BigDecimal... valores) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : valores) {
            if (v != null) {
                total = total.add(v);
            }
        }
        return redondear(total);
    }

    private static BigDecimal requerir(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalArgumentException("El importe no puede ser nulo");
        }
        return valor;
    }
}
