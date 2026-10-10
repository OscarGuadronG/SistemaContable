package com.sistemacontable.control.exception;

import jakarta.ejb.ApplicationException;

/**
 * Se lanza cuando una operacion viola una regla de negocio (partida descuadrada, stock insuficiente,
 * cuenta no imputable, etc.). Al ser excepcion de aplicacion con {@code rollback = true}, el contenedor
 * revierte la transaccion JTA en curso y la propaga tal cual (sin envolverla en EJBException).
 */
@ApplicationException(rollback = true)
public class ReglaNegocioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    public ReglaNegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
