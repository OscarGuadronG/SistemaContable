package com.sistemacontable.boundary;
import com.sistemacontable.control.service.PeriodoService;
import com.sistemacontable.security.Roles;
import com.sistemacontable.security.RolApp;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
@Roles({RolApp.Contabilidad})
@Path("/hoja-trabajo") @RequestScoped @Transactional @Produces(MediaType.APPLICATION_JSON)
public class HojaTrabajoResource {
    @Inject private PeriodoService servicio;
    @GET public Response consultar(@QueryParam("periodoId") Long periodoId,@QueryParam("desde") LocalDate desde,@QueryParam("hasta") LocalDate hasta) {
        if(periodoId!=null) return Respuestas.ok(com.sistemacontable.boundary.mapper.HojaTrabajoMapper.hoja(servicio.hojaTrabajo(periodoId)));
        return Respuestas.ok(com.sistemacontable.boundary.mapper.HojaTrabajoMapper.hoja(servicio.hojaTrabajo(desde,hasta)));
    }
}
