package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.EmpleadoRequest;
import com.sistemacontable.boundary.mapper.CatalogoMapper;
import com.sistemacontable.control.service.EmpleadoService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Empleados (el estado Activo/Inactivo/Suspendido se cambia con PUT).
 *
 * <p>Solo delega en {@link EmpleadoService}: sin reglas de negocio ni acceso a datos. La transaccion abarca la
 * peticion para que el mapeo a DTO lea las relaciones perezosas dentro de ella.</p>
 */
@Path("/empleados")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmpleadoResource {

    @Inject
    private EmpleadoService servicio;

    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                CatalogoMapper::empleado);
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Respuestas.ok(CatalogoMapper.empleado(servicio.buscarPorId(id)));
    }

    @POST
    public Response crear(@Valid EmpleadoRequest cuerpo, @Context UriInfo uriInfo) {
        var creado = servicio.crear(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "empleados", creado.getId(), CatalogoMapper.empleado(creado));
    }

    @PUT
    @Path("/{id}")
    public Response modificar(@PathParam("id") Long id, @Valid EmpleadoRequest cuerpo) {
        return Respuestas.ok(CatalogoMapper.empleado(servicio.modificar(id, Respuestas.requerir(cuerpo).aCmd())));
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        servicio.eliminar(id);
        return Respuestas.sinContenido();
    }
}
