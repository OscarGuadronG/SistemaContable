package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Tabla {@code public.kardex_detalle}. Solo se agrega (append-only): ninguna columna es actualizable.
 */
@Entity
@Table(name = "kardex_detalle", schema = "public")
public class KardexDetalle extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_kardex", nullable = false, updatable = false)
    @NotNull
    private Kardex kardex;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false, updatable = false)
    @NotNull
    private Lote lote;

    @Column(name = "cantidad", nullable = false, updatable = false)
    @NotNull
    @Positive
    private BigDecimal cantidad;

    @Column(name = "precio_costo", nullable = false, updatable = false)
    @NotNull
    @PositiveOrZero
    private BigDecimal precioCosto;

    public Kardex getKardex() {
        return kardex;
    }

    public void setKardex(Kardex kardex) {
        this.kardex = kardex;
    }

    public Lote getLote() {
        return lote;
    }

    public void setLote(Lote lote) {
        this.lote = lote;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioCosto() {
        return precioCosto;
    }

    public void setPrecioCosto(BigDecimal precioCosto) {
        this.precioCosto = precioCosto;
    }

    @Override
    public String toString() {
        return "KardexDetalle[id=" + getId() + ", cantidad=" + cantidad + ", precioCosto=" + precioCosto + "]";
    }
}
