package com.sistemacontable.boundary.dto;

import com.sistemacontable.security.RolApp;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.json.bind.annotation.JsonbNillable;
import java.util.UUID;

/** DTOs de sesión y administración de usuarios; no exponen entidades JPA. */
public final class AutenticacionDto {
    private AutenticacionDto() { }

    public record EmpleadoMini(Long id, String nombre, String apellido) { }
    @JsonbNillable
    public record Me(UUID id, String email, RolApp rol, String nombre, Long idEmpleado, EmpleadoMini empleado) { }
    public record Usuario(UUID id, String email, RolApp rol, String nombre, Long idEmpleado, boolean activo,
                          String creado) { }
    public record CrearUsuario(@NotBlank @Email String email, @NotBlank String password,
                               @NotNull RolApp rol, @NotBlank String nombre, Long idEmpleado) { }
    public record ActualizarUsuario(@NotNull RolApp rol, @NotBlank String nombre, Long idEmpleado,
                                    @NotNull Boolean activo) { }
}
