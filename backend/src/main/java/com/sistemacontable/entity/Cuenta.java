package com.sistemacontable.entity;

import com.sistemacontable.entity.enums.NaturalezaCuenta;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Tabla {@code public.cuentas}. La columna 'name' de la tabla se expone como {@code nombre}.
 */
@Entity
@Table(name = "cuentas", schema = "public")
public class Cuenta extends EntidadBase {

    @Column(name = "codigo", nullable = false)
    @NotNull
    private Integer codigo;

    @Column(name = "name", nullable = false)
    @NotBlank
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "naturaleza", nullable = false)
    @NotNull
    private NaturalezaCuenta naturaleza;

    @Column(name = "activo", nullable = false)
    @NotNull
    private Boolean activo = Boolean.TRUE;

    @Column(name = "imputable", nullable = false)
    @NotNull
    private Boolean imputable = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_padre")
    private Cuenta padre;

    public Integer getCodigo() {
        return codigo;
    }

    public void setCodigo(Integer codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public NaturalezaCuenta getNaturaleza() {
        return naturaleza;
    }

    public void setNaturaleza(NaturalezaCuenta naturaleza) {
        this.naturaleza = naturaleza;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getImputable() {
        return imputable;
    }

    public void setImputable(Boolean imputable) {
        this.imputable = imputable;
    }

    public Cuenta getPadre() {
        return padre;
    }

    public void setPadre(Cuenta padre) {
        this.padre = padre;
    }

    @Override
    public String toString() {
        return "Cuenta[id=" + getId() + ", codigo=" + codigo + ", nombre=" + nombre + ", naturaleza=" + naturaleza + ", activo=" + activo + ", imputable=" + imputable + "]";
    }
}
