package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Tabla {@code public.unidad_medida}.
 */
@Entity
@Table(name = "unidad_medida", schema = "public")
public class UnidadMedida extends EntidadBase {

    @Column(name = "nombre", nullable = false)
    @NotBlank
    private String nombre;

    @Column(name = "abrev", nullable = false)
    @NotBlank
    private String abrev;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getAbrev() {
        return abrev;
    }

    public void setAbrev(String abrev) {
        this.abrev = abrev;
    }

    @Override
    public String toString() {
        return "UnidadMedida[id=" + getId() + ", nombre=" + nombre + ", abrev=" + abrev + "]";
    }
}
