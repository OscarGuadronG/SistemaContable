package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Tabla {@code public.cliente}.
 */
@Entity
@Table(name = "cliente", schema = "public")
public class Cliente extends EntidadBase {

    @Column(name = "nombre", nullable = false)
    @NotBlank
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "nit", nullable = false)
    @NotBlank
    private String nit;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    @Override
    public String toString() {
        return "Cliente[id=" + getId() + ", nombre=" + nombre + ", descripcion=" + descripcion + ", nit=" + nit + "]";
    }
}
