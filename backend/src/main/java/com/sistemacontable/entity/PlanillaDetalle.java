package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Tabla {@code public.planilla_detalle}.
 */
@Entity
@Table(name = "planilla_detalle", schema = "public")
public class PlanillaDetalle extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planilla_id", nullable = false)
    @NotNull
    private Planilla planilla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    @NotNull
    private Empleado empleado;

    @Column(name = "dias", nullable = false)
    @NotNull
    @Min(0)
    @Max(31)
    private Short dias = (short) 30;

    @Column(name = "horasx_diur", nullable = false)
    @NotNull
    @PositiveOrZero
    private Short horasxDiur = (short) 0;

    @Column(name = "horasx_noct", nullable = false)
    @NotNull
    @PositiveOrZero
    private Short horasxNoct = (short) 0;

    @Column(name = "sueldo_base", nullable = false)
    @NotNull
    @PositiveOrZero
    private BigDecimal sueldoBase;

    public Planilla getPlanilla() {
        return planilla;
    }

    public void setPlanilla(Planilla planilla) {
        this.planilla = planilla;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public Short getDias() {
        return dias;
    }

    public void setDias(Short dias) {
        this.dias = dias;
    }

    public Short getHorasxDiur() {
        return horasxDiur;
    }

    public void setHorasxDiur(Short horasxDiur) {
        this.horasxDiur = horasxDiur;
    }

    public Short getHorasxNoct() {
        return horasxNoct;
    }

    public void setHorasxNoct(Short horasxNoct) {
        this.horasxNoct = horasxNoct;
    }

    public BigDecimal getSueldoBase() {
        return sueldoBase;
    }

    public void setSueldoBase(BigDecimal sueldoBase) {
        this.sueldoBase = sueldoBase;
    }

    @Override
    public String toString() {
        return "PlanillaDetalle[id=" + getId() + ", dias=" + dias + ", horasxDiur=" + horasxDiur + ", horasxNoct=" + horasxNoct + ", sueldoBase=" + sueldoBase + "]";
    }
}
