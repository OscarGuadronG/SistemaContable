package com.sistemacontable.boundary;
import com.sistemacontable.control.service.LibroService;
import com.sistemacontable.security.Roles;
import com.sistemacontable.security.RolApp;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;

@Path("/libros") @RequestScoped @Transactional @Produces(MediaType.APPLICATION_JSON)
public class LibroResource {
    @Inject private LibroService service;
    @GET @Path("/compras") @Roles({RolApp.Bodega, RolApp.Contabilidad})
    public Response compras(@QueryParam("desde") String desde, @QueryParam("hasta") String hasta) {
        return Respuestas.ok(service.compras(fecha(desde,"desde"), fecha(hasta,"hasta")));
    }
    @GET @Path("/ventas") @Roles({RolApp.Ventas, RolApp.Contabilidad})
    public Response ventas(@QueryParam("desde") String desde, @QueryParam("hasta") String hasta) {
        return Respuestas.ok(service.ventas(fecha(desde,"desde"), fecha(hasta,"hasta")));
    }
    private static LocalDate fecha(String value, String name) { return value == null || value.isBlank() ? null : Parametros.fecha(value,name); }
}
