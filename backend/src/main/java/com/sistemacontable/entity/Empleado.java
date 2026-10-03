package com.sistemacontable.entity;

import com.sistemacontable.entity.enums.EstadoEmpleado;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tabla {@code public.empleado}.
 */
@Entity
@Table(name = "empleado", schema = "public")
public class Empleado extends EntidadBase {

    @Column(name = "nombre", nullable = false)
    @NotBlank
    private String nombre;

    @Column(name = "apellido", nullable = false)
    @NotBlank
    private String apellido;

    @Column(name = "fech_nac")
    private LocalDate fechNac;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_departamento", nullable = false)
    @NotNull
    private Departamento departamento;

    @Column(name = "salario", nullable = false)
    @NotNull
    @PositiveOrZero
    private BigDecimal salario = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @NotNull
    private EstadoEmpleado estado = EstadoEmpleado.Activo;

    @Column(name = "numero_isss", nullable = false)
    @NotBlank
    private String numeroIsss;

    @Column(name = "dui", nullable = false)
    @NotBlank
    private String dui;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public LocalDate getFechNac() {
        return fechNac;
    }

    public void setFechNac(LocalDate fechNac) {
        this.fechNac = fechNac;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = salario;
    }

    public EstadoEmpleado getEstado() {
        return estado;
    }

    public void setEstado(EstadoEmpleado estado) {
        this.estado = estado;
    }

    public String getNumeroIsss() {
        return numeroIsss;
    }

    public void setNumeroIsss(String numeroIsss) {
        this.numeroIsss = numeroIsss;
    }

    public String getDui() {
        return dui;
    }

    public void setDui(String dui) {
        this.dui = dui;
    }

    @Override
    public String toString() {
        return "Empleado[id=" + getId() + ", nombre=" + nombre + ", apellido=" + apellido + ", fechNac=" + fechNac + ", salario=" + salario + ", estado=" + estado + ", numeroIsss=" + numeroIsss + ", dui=" + dui + "]";
    }
}
