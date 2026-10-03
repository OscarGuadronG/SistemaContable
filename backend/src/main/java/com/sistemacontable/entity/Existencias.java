package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Tabla {@code public.existencias}. No hay UNIQUE(id_lote, id_almacen) en la BD: ver docs/recomendaciones-bd.md.
 */
@Entity
@Table(name = "existencias", schema = "public")
public class Existencias extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    @NotNull
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen", nullable = false)
    @NotNull
    private Almacen almacen;

    @Column(name = "cantidad", nullable = false)
    @NotNull
    @PositiveOrZero
    private BigDecimal cantidad = BigDecimal.ZERO;

    public Lote getLote() {
        return lote;
    }

    public void setLote(Lote lote) {
        this.lote = lote;
    }

    public Almacen getAlmacen() {
        return almacen;
    }

    public void setAlmacen(Almacen almacen) {
        this.almacen = almacen;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public String toString() {
        return "Existencias[id=" + getId() + ", cantidad=" + cantidad + "]";
    }
}
