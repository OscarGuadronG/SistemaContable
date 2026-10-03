package com.sistemacontable.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * Base de todas las entidades: clave primaria {@code bigint GENERATED ALWAYS AS IDENTITY}.
 *
 * <p>El id lo asigna la base de datos; la aplicacion nunca lo escribe en el INSERT. La igualdad se basa en
 * el id: dos instancias sin id (aun no persistidas) solo son iguales si son la misma referencia. El
 * {@code hashCode} es constante por clase para que no cambie cuando el id se asigna al persistir.</p>
 */
@MappedSuperclass
public abstract class EntidadBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || getClass() != otro.getClass()) {
            return false;
        }
        Long otroId = ((EntidadBase) otro).id;
        return id != null && id.equals(otroId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + "]";
    }
}
