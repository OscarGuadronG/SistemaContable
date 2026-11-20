package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.AutenticacionDto.ActualizarUsuario;
import com.sistemacontable.boundary.dto.AutenticacionDto.CrearUsuario;
import com.sistemacontable.control.service.AutenticacionService;
import com.sistemacontable.security.RolApp;
import com.sistemacontable.security.Roles;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.UUID;

/** Endpoints del usuario autenticado y administración de perfiles. */
@Path("/")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class AutenticacionResource {
    @Inject private AutenticacionService servicio;

    @GET
    @Path("me")
    @Roles({RolApp.Ventas, RolApp.Bodega, RolApp.Contabilidad, RolApp.RRHH})
    public Response me() {
        return Response.ok(servicio.me()).build();
    }

    @GET
    @Path("usuarios")
    @Roles({RolApp.Administrador})
    public Response listarUsuarios() {
        return Response.ok(servicio.listarUsuarios()).build();
    }

    @POST
    @Path("usuarios")
    @Consumes(MediaType.APPLICATION_JSON)
    @Roles({RolApp.Administrador})
    public Response crearUsuario(@Valid CrearUsuario cuerpo, @Context UriInfo uriInfo) {
        var creado = servicio.crearUsuario(Respuestas.requerir(cuerpo));
        return Response.created(uriInfo.getAbsolutePathBuilder().path(creado.id().toString()).build())
                .entity(creado).build();
    }

    @PUT
    @Path("usuarios/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Roles({RolApp.Administrador})
    public Response actualizarUsuario(@PathParam("id") UUID id, @Valid ActualizarUsuario cuerpo) {
        servicio.actualizarUsuario(id, Respuestas.requerir(cuerpo));
        return Response.noContent().build();
    }
}
