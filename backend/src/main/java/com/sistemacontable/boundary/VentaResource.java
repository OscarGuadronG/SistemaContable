package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.VentaRequest;
import com.sistemacontable.boundary.mapper.InventarioMapper;
import com.sistemacontable.control.service.ResultadoVenta;
import com.sistemacontable.control.service.VentaService;
import com.sistemacontable.entity.Venta;
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
 * Ventas. {@code POST} registra la venta completa (existencias, kardex, partida de ingreso y de costo) en una sola
 * transaccion del servicio; las ventas no se modifican ni se borran, por eso no hay PUT ni DELETE.
 *
 * <p>Solo delega en {@link VentaService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/ventas")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VentaResource {

    @Inject
    private VentaService servicio;

    /** Listado paginado (sin lineas); el total va en la cabecera {@code Total-Records}. */
    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                venta -> InventarioMapper.venta(venta, null));
    }

    /** Una venta con sus lineas. */
    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        Venta venta = servicio.buscarPorId(id);
        return Respuestas.ok(InventarioMapper.venta(venta, servicio.detalles(id)));
    }

    @POST
    public Response registrar(@Valid VentaRequest cuerpo, @Context UriInfo uriInfo) {
        ResultadoVenta resultado = servicio.registrar(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "ventas", resultado.venta().getId(), InventarioMapper.resultado(resultado));
    }
}
