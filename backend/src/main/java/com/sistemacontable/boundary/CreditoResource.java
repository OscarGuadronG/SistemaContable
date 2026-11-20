package com.sistemacontable.boundary;
import com.sistemacontable.boundary.dto.PagoCreditoRequest;
import com.sistemacontable.control.service.CreditoService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("/creditos")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@com.sistemacontable.security.Roles({com.sistemacontable.security.RolApp.Ventas,com.sistemacontable.security.RolApp.Contabilidad})
public class CreditoResource {
    @Inject private CreditoService servicio;
    @GET
    public Response listar(@QueryParam("tipo") String tipo, @QueryParam("estado") String estado,
            @QueryParam("terceroId") Long terceroId, @QueryParam("first") @DefaultValue("0") int first,
            @QueryParam("max") @DefaultValue("50") int max) {
        return Respuestas.paginaLista(servicio.listar(tipo, estado, terceroId), Paginacion.de(first,max));
    }
    @GET @Path("/{tipo}/{documentoId}")
    public Response obtener(@PathParam("tipo") String tipo, @PathParam("documentoId") Long id, @QueryParam("fecha") String fecha) {
        return Respuestas.ok(servicio.obtener(tipo,id,fecha == null ? null : Parametros.fecha(fecha,"fecha")));
    }
    @POST @Path("/pagos")
    public Response pagar(@Valid PagoCreditoRequest cuerpo,@Context UriInfo uri) {
        var resultado = servicio.pagar(Respuestas.requerir(cuerpo).aCmd());
        return Response.created(uri.getBaseUriBuilder().path("creditos").path(cuerpo.tipo()).path(cuerpo.documentoId().toString()).build()).entity(resultado).build();
    }
}
