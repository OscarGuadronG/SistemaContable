package com.sistemacontable.security;

import jakarta.enterprise.context.RequestScoped;
import java.util.UUID;
import jakarta.ws.rs.ForbiddenException;

/** Identidad verificada durante la petición HTTP actual. */
@RequestScoped
public class UsuarioActual {
    private UUID id;
    private String email;
    private RolApp rol;
    private Long idEmpleado;
    private String nombre;

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public RolApp getRol() { return rol; }
    public Long getIdEmpleado() { return idEmpleado; }
    public String getNombre() { return nombre; }

    /** Restricción de crédito para servicios que conocen el tipo de documento, incluso con query/body variable. */
    public void exigirCredito(boolean venta) {
        if (rol == null || id == null) throw new ForbiddenException("Se requiere un usuario autenticado");
        if (rol == RolApp.Administrador || rol == RolApp.Contabilidad) return;
        if (venta && rol == RolApp.Ventas) return;
        throw new ForbiddenException("No tiene permiso para esta operacion");
    }

    public void establecer(Perfil perfil, String email) {
        this.id = perfil.getId();
        this.email = email;
        this.rol = perfil.getRol();
        this.idEmpleado = perfil.getIdEmpleado();
        this.nombre = perfil.getNombre();
    }
}
