package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.TrasladoRequest;
import com.sistemacontable.boundary.mapper.InventarioMapper;
import com.sistemacontable.control.service.MovimientoKardex;
import com.sistemacontable.control.service.TrasladoService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * Traslados de stock entre almacenes. Solo se crean: el movimiento resultante se consulta en
 * {@code /kardex/{id}} (cabecera {@code Location}).
 *
 * <p>Solo delega en {@link TrasladoService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/traslados")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TrasladoResource {

    @Inject
    private TrasladoService servicio;

    @POST
    public Response registrar(@Valid TrasladoRequest cuerpo, @Context UriInfo uriInfo) {
        MovimientoKardex movimiento = servicio.registrar(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "kardex", movimiento.kardex().getId(),
                InventarioMapper.movimiento(movimiento));
    }
}
