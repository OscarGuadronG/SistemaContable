package com.sistemacontable.entity;

import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Tabla {@code public.partida}.
 */
@Entity
@Table(name = "partida", schema = "public")
public class Partida extends EntidadBase {

    @Column(name = "fecha", nullable = false)
    @NotNull
    private LocalDate fecha = LocalDate.now();

    @Column(name = "concepto", nullable = false)
    @NotBlank
    private String concepto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    @NotNull
    private TipoPartida tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tercero")
    private Tercero tercero;

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public TipoPartida getTipo() {
        return tipo;
    }

    public void setTipo(TipoPartida tipo) {
        this.tipo = tipo;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Tercero getTercero() {
        return tercero;
    }

    public void setTercero(Tercero tercero) {
        this.tercero = tercero;
    }

    @Override
    public String toString() {
        return "Partida[id=" + getId() + ", fecha=" + fecha + ", concepto=" + concepto + ", tipo=" + tipo + "]";
    }
}
