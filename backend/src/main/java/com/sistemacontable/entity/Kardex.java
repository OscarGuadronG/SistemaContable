package com.sistemacontable.entity;

import com.sistemacontable.entity.enums.TipoMovimiento;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Tabla {@code public.kardex}. Las columnas origen/destino apuntan a almacen; tip_mov se expone como {@code tipoMovimiento}.
 */
@Entity
@Table(name = "kardex", schema = "public")
public class Kardex extends EntidadBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "tip_mov", nullable = false)
    @NotNull
    private TipoMovimiento tipoMovimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origen")
    private Almacen origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destino")
    private Almacen destino;

    @Column(name = "fecha", nullable = false)
    @NotNull
    private OffsetDateTime fecha = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @Column(name = "descripcion")
    private String descripcion;

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public Almacen getOrigen() {
        return origen;
    }

    public void setOrigen(Almacen origen) {
        this.origen = origen;
    }

    public Almacen getDestino() {
        return destino;
    }

    public void setDestino(Almacen destino) {
        this.destino = destino;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }

    public void setFecha(OffsetDateTime fecha) {
        this.fecha = fecha;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return "Kardex[id=" + getId() + ", tipoMovimiento=" + tipoMovimiento + ", fecha=" + fecha + ", descripcion=" + descripcion + "]";
    }
}
