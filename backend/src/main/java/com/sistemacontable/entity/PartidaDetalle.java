package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Tabla {@code public.partida_detalle}. Solo se agrega (append-only): ninguna columna es actualizable.
 */
@Entity
@Table(name = "partida_detalle", schema = "public")
public class PartidaDetalle extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_partida", nullable = false, updatable = false)
    @NotNull
    private Partida partida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuenta", nullable = false, updatable = false)
    @NotNull
    private Cuenta cuenta;

    @Column(name = "abono", nullable = false, updatable = false)
    @NotNull
    @PositiveOrZero
    private BigDecimal abono = BigDecimal.ZERO;

    @Column(name = "cargo", nullable = false, updatable = false)
    @NotNull
    @PositiveOrZero
    private BigDecimal cargo = BigDecimal.ZERO;

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public Cuenta getCuenta() {
        return cuenta;
    }

    public void setCuenta(Cuenta cuenta) {
        this.cuenta = cuenta;
    }

    public BigDecimal getAbono() {
        return abono;
    }

    public void setAbono(BigDecimal abono) {
        this.abono = abono;
    }

    public BigDecimal getCargo() {
        return cargo;
    }

    public void setCargo(BigDecimal cargo) {
        this.cargo = cargo;
    }

    @Override
    public String toString() {
        return "PartidaDetalle[id=" + getId() + ", abono=" + abono + ", cargo=" + cargo + "]";
    }
}
