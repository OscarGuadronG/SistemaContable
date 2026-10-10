package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Tabla {@code public.lote}.
 */
@Entity
@Table(name = "lote", schema = "public")
public class Lote extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    @NotNull
    private Producto producto;

    @Column(name = "num_lote", nullable = false)
    @NotBlank
    private String numLote;

    @Column(name = "fech_caducidad")
    private LocalDate fechCaducidad;

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public String getNumLote() {
        return numLote;
    }

    public void setNumLote(String numLote) {
        this.numLote = numLote;
    }

    public LocalDate getFechCaducidad() {
        return fechCaducidad;
    }

    public void setFechCaducidad(LocalDate fechCaducidad) {
        this.fechCaducidad = fechCaducidad;
    }

    @Override
    public String toString() {
        return "Lote[id=" + getId() + ", numLote=" + numLote + ", fechCaducidad=" + fechCaducidad + "]";
    }
}
