package com.sistemacontable.control.service;

import com.sistemacontable.control.exception.ReglaNegocioException;
import java.math.BigDecimal;

/** Validaciones simples de campos para los servicios de catalogos; todas fallan con {@link ReglaNegocioException}. */
final class Validaciones {

    private Validaciones() {
    }

    /** Texto obligatorio: devuelve el valor sin espacios en los extremos. */
    static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaNegocioException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }

    /** Texto opcional: sin espacios en los extremos y {@code null} si queda vacio. */
    static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    static <T> T requerir(T valor, String campo) {
        if (valor == null) {
            throw new ReglaNegocioException("El campo '" + campo + "' es obligatorio");
        }
        return valor;
    }

    /** Importe obligatorio y mayor o igual que cero. */
    static BigDecimal noNegativo(BigDecimal valor, String campo) {
        requerir(valor, campo);
        if (valor.signum() < 0) {
            throw new ReglaNegocioException("El campo '" + campo + "' no puede ser negativo");
        }
        return valor;
    }
}
