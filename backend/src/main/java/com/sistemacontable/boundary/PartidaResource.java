package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.PartidaRequest;
import com.sistemacontable.boundary.mapper.ContabilidadMapper;
import com.sistemacontable.control.service.PartidaService;
import com.sistemacontable.entity.Partida;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Partidas contables. Son inmutables: solo se crean y se consultan (una correccion se hace con otra partida), por
 * eso PUT y DELETE responden 405.
 *
 * <p>Solo delega en {@link PartidaService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/partidas")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PartidaResource {

    @Inject
    private PartidaService servicio;

    /** Listado paginado (sin lineas); el total va en la cabecera {@code Total-Records}. */
    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                partida -> ContabilidadMapper.partida(partida, null));
    }

    /** Una partida con sus lineas. */
    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        Partida partida = servicio.buscarPorId(id);
        return Respuestas.ok(ContabilidadMapper.partida(partida, servicio.lineas(id)));
    }

    @POST
    public Response crear(@Valid PartidaRequest cuerpo, @Context UriInfo uriInfo) {
        Partida partida = servicio.crear(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "partidas", partida.getId(),
                ContabilidadMapper.partida(partida, servicio.lineas(partida.getId())));
    }
}
