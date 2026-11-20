package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.ParametroRequest;
import com.sistemacontable.control.service.ParametroService;
import com.sistemacontable.security.Roles;
import com.sistemacontable.security.RolApp;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import com.sistemacontable.security.Roles;
import com.sistemacontable.security.RolApp;

@Path("/parametros") @RequestScoped @Transactional @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
@Roles({RolApp.Ventas, RolApp.Bodega, RolApp.Contabilidad, RolApp.RRHH})
public class ParametroResource {
    @Inject private ParametroService service;
    @GET public Response listar() { return Respuestas.ok(service.listar()); }
    @PUT @Path("/{clave}") @Roles({RolApp.Contabilidad}) public Response actualizar(@PathParam("clave") String clave, @Valid ParametroRequest request) {
        return Respuestas.ok(service.actualizar(clave, Respuestas.requerir(request).valor()));
    }
}
