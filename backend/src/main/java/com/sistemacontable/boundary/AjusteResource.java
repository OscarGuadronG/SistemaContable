package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.AjusteRequest;
import com.sistemacontable.boundary.mapper.InventarioMapper;
import com.sistemacontable.control.service.AjusteService;
import com.sistemacontable.control.service.ResultadoAjuste;
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
 * Ajustes de inventario (cantidad con signo explicito y motivo obligatorio). Solo se crean: el movimiento
 * resultante se consulta en {@code /kardex/{id}} (cabecera {@code Location}).
 *
 * <p>Solo delega en {@link AjusteService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/ajustes")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AjusteResource {

    @Inject
    private AjusteService servicio;

    @POST
    public Response registrar(@Valid AjusteRequest cuerpo, @Context UriInfo uriInfo) {
        ResultadoAjuste resultado = servicio.registrar(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "kardex", resultado.movimiento().kardex().getId(),
                InventarioMapper.resultado(resultado));
    }
}
