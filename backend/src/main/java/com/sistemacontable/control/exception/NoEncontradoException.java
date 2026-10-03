package com.sistemacontable.control.exception;

import jakarta.ejb.ApplicationException;

/**
 * Se lanza cuando un registro referenciado no existe. Excepcion de aplicacion con {@code rollback = true}.
 */
@ApplicationException(rollback = true)
public class NoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public NoEncontradoException(String entidad, Object id) {
        super(entidad + " con id " + id + " no existe");
    }
}
