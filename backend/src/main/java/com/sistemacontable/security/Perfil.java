package com.sistemacontable.security;

import com.sistemacontable.entity.Empleado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Cacheable;
import java.util.UUID;

/** Perfil de autenticación, independiente de las entidades contables. */
@Entity
@Cacheable(false)
@Table(name = "perfil", schema = "public")
public class Perfil {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private RolApp rol;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "id_empleado")
    private Long idEmpleado;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", insertable = false, updatable = false)
    private Empleado empleado;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public RolApp getRol() { return rol; }
    public void setRol(RolApp rol) { this.rol = rol; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Long idEmpleado) { this.idEmpleado = idEmpleado; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Empleado getEmpleado() { return empleado; }
}
