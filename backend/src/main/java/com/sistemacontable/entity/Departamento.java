package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Tabla {@code public.departamento}.
 */
@Entity
@Table(name = "departamento", schema = "public")
public class Departamento extends EntidadBase {

    @Column(name = "nombre", nullable = false)
    @NotBlank
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

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

    @Override
    public String toString() {
        return "Departamento[id=" + getId() + ", nombre=" + nombre + ", descripcion=" + descripcion + "]";
    }
}
