package com.sistemacontable.boundary;
import com.sistemacontable.boundary.dto.InventarioDto;
import com.sistemacontable.control.service.InventarioService;
import com.sistemacontable.security.Roles;
import com.sistemacontable.security.RolApp;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/inventario") @RequestScoped @Transactional @Produces(MediaType.APPLICATION_JSON)
@Roles({RolApp.Ventas, RolApp.Bodega, RolApp.Contabilidad, RolApp.RRHH})
public class InventarioResource {
    @Inject private InventarioService service;
    @GET public Response listar(@QueryParam("buscar") String buscar, @QueryParam("tipoId") Long tipoId,
            @QueryParam("almacenId") Long almacenId, @QueryParam("disponibilidad") String disponibilidad,
            @QueryParam("first") Integer first, @QueryParam("max") Integer max) {
        int f = first == null ? 0 : first, m = max == null ? 50 : max;
        Paginacion pagina = Paginacion.de(f, m);
        var filas = service.listar(buscar, tipoId, almacenId, disponibilidad, pagina.first(), pagina.max());
        return Respuestas.pagina(filas, service.contar(buscar, tipoId, almacenId, disponibilidad), x -> x);
    }
    @GET @Path("/{productoId}") public Response detalle(@PathParam("productoId") Long id, @QueryParam("almacenId") Long almacenId) {
        InventarioDto resultado = service.buscar(id, almacenId); return Respuestas.ok(resultado);
    }
}
