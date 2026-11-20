package com.sistemacontable.boundary;

import com.sistemacontable.security.Roles;
import com.sistemacontable.security.RolApp;
import com.sistemacontable.boundary.dto.TrasladoRequest;
import com.sistemacontable.boundary.mapper.InventarioMapper;
import com.sistemacontable.boundary.mapper.TrasladoMapper;
import com.sistemacontable.control.service.MovimientoKardex;
import com.sistemacontable.control.service.TrasladoService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
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
@Roles({RolApp.Bodega})
@Path("/traslados")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TrasladoResource {

    @Inject
    private TrasladoService servicio;

    @Roles({RolApp.Bodega})
    @POST
    public Response registrar(@Valid TrasladoRequest cuerpo, @Context UriInfo uriInfo) {
        var resultado = servicio.registrar(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "traslados", resultado.movimiento().kardex().getId(),
                TrasladoMapper.dto(resultado, true));
    }

    @Roles({RolApp.Bodega, RolApp.Contabilidad})
    @GET public Response listar(@QueryParam("desde") String desde, @QueryParam("hasta") String hasta,
            @QueryParam("almacenId") Long almacenId, @QueryParam("first") @jakarta.ws.rs.DefaultValue("0") int first,
            @QueryParam("max") @jakarta.ws.rs.DefaultValue("50") int max) {
        var pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(desde == null ? null : Parametros.fecha(desde,"desde"), hasta == null ? null : Parametros.fecha(hasta,"hasta"), almacenId, pagina.first(), pagina.max()),
                servicio.contar(desde == null ? null : Parametros.fecha(desde,"desde"), hasta == null ? null : Parametros.fecha(hasta,"hasta"), almacenId), x -> TrasladoMapper.dto(x, false));
    }
    @Roles({RolApp.Bodega, RolApp.Contabilidad})
    @GET @Path("/{kardexId}") public Response obtener(@PathParam("kardexId") Long id) { return Respuestas.ok(TrasladoMapper.dto(servicio.buscar(id), true)); }
}
