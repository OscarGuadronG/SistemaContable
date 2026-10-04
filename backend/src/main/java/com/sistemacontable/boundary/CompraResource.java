package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.CompraRequest;
import com.sistemacontable.boundary.mapper.InventarioMapper;
import com.sistemacontable.control.service.CompraService;
import com.sistemacontable.control.service.ResultadoCompra;
import com.sistemacontable.entity.Compra;
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
 * Compras. {@code POST} registra la compra completa (existencias, kardex y partida) en una sola transaccion del
 * servicio; las compras no se modifican ni se borran (una correccion se hace con otro documento), por eso no hay
 * PUT ni DELETE.
 *
 * <p>Solo delega en {@link CompraService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/compras")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CompraResource {

    @Inject
    private CompraService servicio;

    /** Listado paginado (sin lineas); el total va en la cabecera {@code Total-Records}. */
    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                compra -> InventarioMapper.compra(compra, null));
    }

    /** Una compra con sus lineas. */
    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        Compra compra = servicio.buscarPorId(id);
        return Respuestas.ok(InventarioMapper.compra(compra, servicio.detalles(id)));
    }

    @POST
    public Response registrar(@Valid CompraRequest cuerpo, @Context UriInfo uriInfo) {
        ResultadoCompra resultado = servicio.registrar(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "compras", resultado.compra().getId(), InventarioMapper.resultado(resultado));
    }
}
