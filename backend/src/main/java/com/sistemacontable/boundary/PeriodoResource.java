package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.PeriodoDto;
import com.sistemacontable.boundary.mapper.ContabilidadMapper;
import com.sistemacontable.boundary.mapper.PeriodoMapper;
import com.sistemacontable.control.service.CierrePeriodo;
import com.sistemacontable.control.service.PartidaService;
import com.sistemacontable.control.service.PeriodoService;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.security.Roles;
import com.sistemacontable.security.RolApp;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Map;

@Roles({RolApp.Contabilidad})
@Path("/periodos") @RequestScoped @Transactional @Produces(MediaType.APPLICATION_JSON)
public class PeriodoResource {
    @Inject private PeriodoService servicio;
    @Inject private PartidaService partidaService;

    @GET public Response listar() { return Respuestas.ok(servicio.listar().stream().map(PeriodoMapper::periodo).toList()); }
    @GET @Path("/actual") public Response actual() { return Respuestas.ok(PeriodoMapper.periodo(servicio.actual(LocalDate.now()))); }
    @GET @Path("/{id}") public Response obtener(@PathParam("id") Long id) { return Respuestas.ok(PeriodoMapper.periodo(servicio.buscar(id))); }
    @POST @Path("/{id}/cerrar") public Response cerrar(@PathParam("id") Long id) {
        CierrePeriodo c=servicio.cerrar(id);
        return Respuestas.ok(new com.sistemacontable.boundary.dto.CierrePeriodoDto(PeriodoMapper.periodo(c.periodo()),dto(c.partidaCierre()),dto(c.partidaApertura())));
    }
    private com.sistemacontable.boundary.dto.PartidaDto dto(Partida p) { return p == null ? null : ContabilidadMapper.partida(p,partidaService.lineas(p.getId())); }
}
