package com.sistemacontable.boundary;

import com.sistemacontable.boundary.mapper.ContabilidadMapper;
import com.sistemacontable.control.service.BalanzaService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Balanza de comprobacion de un rango de fechas: {@code GET /balanza?desde=AAAA-MM-DD&hasta=AAAA-MM-DD}. Ambas
 * fechas son obligatorias (400 si faltan o estan mal formadas; 422 si el rango es invalido).
 *
 * <p>Solo delega en {@link BalanzaService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/balanza")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class BalanzaResource {

    @Inject
    private BalanzaService servicio;

    @GET
    public Response balanza(@QueryParam("desde") String desde, @QueryParam("hasta") String hasta) {
        return Respuestas.ok(ContabilidadMapper.balanza(
                servicio.balanza(Parametros.fecha(desde, "desde"), Parametros.fecha(hasta, "hasta"))));
    }
}
