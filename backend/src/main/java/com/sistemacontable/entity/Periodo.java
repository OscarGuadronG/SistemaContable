package com.sistemacontable.entity;

import com.sistemacontable.entity.enums.EstadoPeriodo;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;

/** Trimestre del calendario fiscal y su instantánea de hoja de trabajo. */
@Entity
@Table(name = "periodo", schema = "public")
public class Periodo extends EntidadBase {
    @Column(name = "anio_fiscal", nullable = false)
    @NotNull private Integer anioFiscal;

    @Column(name = "trimestre", nullable = false)
    @NotNull private Short trimestre;

    @Column(name = "fecha_inicio", nullable = false)
    @NotNull private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    @NotNull private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @NotNull private EstadoPeriodo estado = EstadoPeriodo.Abierto;

    /** PostgreSQL jsonb is stored as JSON text; datasource stringType=unspecified infers jsonb. */
    @Column(name = "hoja_trabajo", columnDefinition = "jsonb")
    private String hojaTrabajo;

    @Column(name = "fecha_cierre")
    private Instant fechaCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_partida_cierre")
    private Partida partidaCierre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_partida_apertura")
    private Partida partidaApertura;

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }
    public Integer getAnioFiscal() { return anioFiscal; }
    public void setAnioFiscal(Integer anioFiscal) { this.anioFiscal = anioFiscal; }
    public Short getTrimestre() { return trimestre; }
    public void setTrimestre(Short trimestre) { this.trimestre = trimestre; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public EstadoPeriodo getEstado() { return estado; }
    public void setEstado(EstadoPeriodo estado) { this.estado = estado; }
    public String getHojaTrabajo() { return hojaTrabajo; }
    public void setHojaTrabajo(String hojaTrabajo) { this.hojaTrabajo = hojaTrabajo; }
    public Instant getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(Instant fechaCierre) { this.fechaCierre = fechaCierre; }
    public Partida getPartidaCierre() { return partidaCierre; }
    public void setPartidaCierre(Partida partidaCierre) { this.partidaCierre = partidaCierre; }
    public Partida getPartidaApertura() { return partidaApertura; }
    public void setPartidaApertura(Partida partidaApertura) { this.partidaApertura = partidaApertura; }
}
