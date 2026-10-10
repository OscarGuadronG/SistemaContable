package com.sistemacontable.entity;

import com.sistemacontable.entity.enums.EstadoPlanilla;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Tabla {@code public.planilla}.
 */
@Entity
@Table(name = "planilla", schema = "public")
public class Planilla extends EntidadBase {

    @Column(name = "fecha_inicio", nullable = false)
    @NotNull
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    @NotNull
    private OffsetDateTime fechaFin;

    @Column(name = "fecha_pago", nullable = false)
    @NotNull
    private OffsetDateTime fechaPago;

    @Column(name = "descripcion")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @NotNull
    private EstadoPlanilla estado = EstadoPlanilla.Pendiente;

    public OffsetDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(OffsetDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public OffsetDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(OffsetDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public OffsetDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(OffsetDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoPlanilla getEstado() {
        return estado;
    }

    public void setEstado(EstadoPlanilla estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Planilla[id=" + getId() + ", fechaInicio=" + fechaInicio + ", fechaFin=" + fechaFin + ", fechaPago=" + fechaPago + ", descripcion=" + descripcion + ", estado=" + estado + "]";
    }
}
