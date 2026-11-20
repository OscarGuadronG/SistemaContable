package com.sistemacontable.control.exception;
import jakarta.ejb.ApplicationException;
/** Colisión de una clave de negocio, tanto preventiva como impuesta por la BD. */
@ApplicationException(rollback = true)
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) { super(mensaje); }
}
