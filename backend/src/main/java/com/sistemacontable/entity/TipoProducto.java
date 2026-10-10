package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Tabla {@code public.tipo_producto}.
 */
@Entity
@Table(name = "tipo_producto", schema = "public")
public class TipoProducto extends EntidadBase {

    @Column(name = "nombre", nullable = false)
    @NotBlank
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_padre")
    private TipoProducto padre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoProducto getPadre() {
        return padre;
    }

    public void setPadre(TipoProducto padre) {
        this.padre = padre;
    }

    @Override
    public String toString() {
        return "TipoProducto[id=" + getId() + ", nombre=" + nombre + "]";
    }
}
