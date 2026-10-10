package com.sistemacontable.boundary;

import com.sistemacontable.boundary.mapper.InventarioMapper;
import com.sistemacontable.control.service.ExistenciasService;
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
 * Existencias (stock por lote y almacen). Solo lectura: el stock cambia unicamente por compras, ventas, traslados y
 * ajustes.
 *
 * <p>Solo delega en {@link ExistenciasService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/existencias")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class ExistenciasResource {

    @Inject
    private ExistenciasService servicio;

    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                InventarioMapper::existencias);
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Respuestas.ok(InventarioMapper.existencias(servicio.buscarPorId(id)));
    }
}
