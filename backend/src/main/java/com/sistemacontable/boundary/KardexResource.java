package com.sistemacontable.boundary;

import com.sistemacontable.boundary.mapper.InventarioMapper;
import com.sistemacontable.control.service.KardexService;
import com.sistemacontable.entity.Kardex;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Kardex: consulta de movimientos de inventario. Solo lectura: los movimientos los generan compras, ventas,
 * traslados y ajustes.
 *
 * <p>Solo delega en {@link KardexService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/kardex")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class KardexResource {

    @Inject
    private KardexService servicio;

    /** Listado paginado (sin lineas); el total va en la cabecera {@code Total-Records}. */
    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                movimiento -> InventarioMapper.kardex(movimiento, null));
    }

    /** Un movimiento con sus lineas. */
    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        Kardex movimiento = servicio.buscarPorId(id);
        return Respuestas.ok(InventarioMapper.kardex(movimiento, servicio.detalles(id)));
    }
}
