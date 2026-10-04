package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.AlmacenRequest;
import com.sistemacontable.boundary.mapper.CatalogoMapper;
import com.sistemacontable.control.service.AlmacenService;
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
 * Almacenes (el estado habilitado/deshabilitado se cambia con PUT).
 *
 * <p>Solo delega en {@link AlmacenService}: sin reglas de negocio ni acceso a datos. La transaccion abarca la
 * peticion para que el mapeo a DTO lea las relaciones perezosas dentro de ella.</p>
 */
@Path("/almacenes")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AlmacenResource {

    @Inject
    private AlmacenService servicio;

    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                CatalogoMapper::almacen);
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Respuestas.ok(CatalogoMapper.almacen(servicio.buscarPorId(id)));
    }

    @POST
    public Response crear(@Valid AlmacenRequest cuerpo, @Context UriInfo uriInfo) {
        var creado = servicio.crear(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "almacenes", creado.getId(), CatalogoMapper.almacen(creado));
    }

    @PUT
    @Path("/{id}")
    public Response modificar(@PathParam("id") Long id, @Valid AlmacenRequest cuerpo) {
        return Respuestas.ok(CatalogoMapper.almacen(servicio.modificar(id, Respuestas.requerir(cuerpo).aCmd())));
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        servicio.eliminar(id);
        return Respuestas.sinContenido();
    }
}
