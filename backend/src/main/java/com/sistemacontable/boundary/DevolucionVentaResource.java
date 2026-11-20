package com.sistemacontable.boundary;
import com.sistemacontable.boundary.dto.DevolucionVentaRequest;
import com.sistemacontable.control.service.DevolucionService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
@Path("/devoluciones-venta")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@com.sistemacontable.security.Roles({com.sistemacontable.security.RolApp.Ventas,com.sistemacontable.security.RolApp.Bodega,com.sistemacontable.security.RolApp.Contabilidad})
public class DevolucionVentaResource {
    @Inject private DevolucionService servicio;
    @GET
    public Response listar(@QueryParam("desde") String desde,@QueryParam("hasta") String hasta,
            @QueryParam("ventaId") Long id, @QueryParam("first") @DefaultValue("0") int first,
            @QueryParam("max") @DefaultValue("50") int max) {
        return Respuestas.paginaLista(servicio.listarVenta(desde == null ? null : Parametros.fecha(desde,"desde"),
                hasta == null ? null : Parametros.fecha(hasta,"hasta"),id),Paginacion.de(first,max));
    }
    @GET @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) { return Respuestas.ok(servicio.obtenerVenta(id)); }
    @POST
    @com.sistemacontable.security.Roles({com.sistemacontable.security.RolApp.Ventas,com.sistemacontable.security.RolApp.Bodega})
    public Response registrar(@Valid DevolucionVentaRequest cuerpo,@Context UriInfo uri) {
        var resultado = servicio.vender(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uri,"devoluciones-venta",resultado.id(),resultado);
    }
}
