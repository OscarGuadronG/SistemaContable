package com.sistemacontable.security;

import com.sistemacontable.boundary.dto.ErrorDto;
import com.sistemacontable.boundary.HelloResource;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Arrays;
import java.util.UUID;

/** Autenticación y autorización central fail closed para todo endpoint REST. */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class FiltroSeguridad implements ContainerRequestFilter {
    @Context private ResourceInfo resourceInfo;
    @Inject private PerfilDAO perfiles;
    @Inject private UsuarioActual usuario;
    private final VerificadorJwt verificador = new VerificadorJwt();

    @Override
    public void filter(ContainerRequestContext request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return;
        if (resourceInfo.getResourceClass() == HelloResource.class
                && resourceInfo.getResourceMethod() != null
                && resourceInfo.getResourceMethod().isAnnotationPresent(jakarta.ws.rs.GET.class)) return;

        String header = request.getHeaderString("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)
                || header.substring(7).isBlank()) {
            abortar(request, 401, "NO_AUTENTICADO", "Se requiere autenticacion");
            return;
        }
        VerificadorJwt.Identidad identidad;
        try {
            identidad = verificador.verificar(header.substring(7).trim());
        } catch (RuntimeException e) {
            abortar(request, 401, "NO_AUTENTICADO", "El token de acceso no es valido");
            return;
        }
        UUID userId;
        try {
            userId = UUID.fromString(identidad.sub());
        } catch (IllegalArgumentException e) {
            abortar(request, 401, "NO_AUTENTICADO", "El token de acceso no es valido");
            return;
        }
        Perfil perfil = perfiles.buscar(userId).orElse(null);
        if (perfil == null || !perfil.isActivo()) {
            abortar(request, 403, "SIN_PERFIL", "El usuario no tiene un perfil activo");
            return;
        }
        usuario.establecer(perfil, identidad.email());

        Roles permiso = resourceInfo.getResourceMethod() == null ? null
                : resourceInfo.getResourceMethod().getAnnotation(Roles.class);
        if (permiso == null && resourceInfo.getResourceClass() != null) {
            permiso = resourceInfo.getResourceClass().getAnnotation(Roles.class);
        }
        if (permiso == null) {
            abortar(request, 403, "PROHIBIDO", "El endpoint no tiene permisos configurados");
            return;
        }
        if (perfil.getRol() != RolApp.Administrador
                && Arrays.stream(permiso.value()).noneMatch(r -> r == perfil.getRol())) {
            abortar(request, 403, "PROHIBIDO", "No tiene permiso para esta operacion");
        }
    }

    private void abortar(ContainerRequestContext request, int estado, String codigo, String mensaje) {
        request.abortWith(Response.status(estado).type(MediaType.APPLICATION_JSON_TYPE)
                .entity(ErrorDto.de(codigo, mensaje)).build());
    }
}
