package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.CuentaRequest;
import com.sistemacontable.boundary.mapper.CatalogoMapper;
import com.sistemacontable.control.service.CuentaService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
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
 * Catalogo de cuentas contables. Una cuenta no se borra: se desactiva (POST /cuentas/{id}/desactivar).
 *
 * <p>Solo delega en {@link CuentaService}: sin reglas de negocio ni acceso a datos. La transaccion abarca la
 * peticion para que el mapeo a DTO lea las relaciones perezosas dentro de ella.</p>
 */
@Path("/cuentas")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CuentaResource {

    @Inject
    private CuentaService servicio;

    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                CatalogoMapper::cuenta);
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Respuestas.ok(CatalogoMapper.cuenta(servicio.buscarPorId(id)));
    }

    @POST
    public Response crear(@Valid CuentaRequest cuerpo, @Context UriInfo uriInfo) {
        var creado = servicio.crear(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "cuentas", creado.getId(), CatalogoMapper.cuenta(creado));
    }

    @PUT
    @Path("/{id}")
    public Response modificar(@PathParam("id") Long id, @Valid CuentaRequest cuerpo) {
        return Respuestas.ok(CatalogoMapper.cuenta(servicio.modificar(id, Respuestas.requerir(cuerpo).aCmd())));
    }

    /** Reactiva la cuenta. */
    @POST
    @Path("/{id}/activar")
    public Response activar(@PathParam("id") Long id) {
        return Respuestas.ok(CatalogoMapper.cuenta(servicio.activar(id)));
    }

    /** Desactiva la cuenta (no se borra): falla si tiene hijas activas o movimientos. */
    @POST
    @Path("/{id}/desactivar")
    public Response desactivar(@PathParam("id") Long id) {
        return Respuestas.ok(CatalogoMapper.cuenta(servicio.desactivar(id)));
    }
}
