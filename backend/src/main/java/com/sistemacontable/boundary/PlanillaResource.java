package com.sistemacontable.boundary;

import com.sistemacontable.boundary.dto.DetallePlanillaRequest;
import com.sistemacontable.boundary.dto.PlanillaRequest;
import com.sistemacontable.boundary.mapper.PlanillaMapper;
import com.sistemacontable.control.service.PlanillaService;
import com.sistemacontable.entity.Planilla;
import com.sistemacontable.entity.PlanillaDetalle;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

/**
 * Planillas de pago: creacion, detalle por empleado, calculo y pago (con su partida de egreso). Una planilla pagada
 * es inmutable. Las tasas del calculo son valores de referencia de {@code contabilidad.properties}: deben
 * verificarse contra la normativa vigente antes de usar planillas reales.
 *
 * <p>Solo delega en {@link PlanillaService}: sin reglas de negocio ni acceso a datos.</p>
 */
@Path("/planillas")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlanillaResource {

    @Inject
    private PlanillaService servicio;

    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.listar(pagina.first(), pagina.max()), servicio.contar(),
                PlanillaMapper::planilla);
    }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Respuestas.ok(PlanillaMapper.planilla(servicio.buscarPorId(id)));
    }

    /** Crea una planilla en estado Pendiente. */
    @POST
    public Response crear(@Valid PlanillaRequest cuerpo, @Context UriInfo uriInfo) {
        Planilla planilla = servicio.crear(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "planillas", planilla.getId(), PlanillaMapper.planilla(planilla));
    }

    @PUT
    @Path("/{id}")
    public Response modificar(@PathParam("id") Long id, @Valid PlanillaRequest cuerpo) {
        return Respuestas.ok(PlanillaMapper.planilla(servicio.modificar(id, Respuestas.requerir(cuerpo).aCmd())));
    }

    // ------------------------------------------------------------------------------------ detalles

    @GET
    @Path("/{id}/detalles")
    public Response detalles(@PathParam("id") Long id) {
        servicio.buscarPorId(id);
        return Respuestas.ok(servicio.detalles(id).stream().map(PlanillaMapper::detalle).toList());
    }

    /** Agrega el detalle de un empleado Activo (un solo detalle por empleado y planilla). */
    @POST
    @Path("/{id}/detalles")
    public Response agregarDetalle(@PathParam("id") Long id, @Valid DetallePlanillaRequest cuerpo,
            @Context UriInfo uriInfo) {
        PlanillaDetalle detalle = servicio.agregarDetalle(id, Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "planillas/detalles", detalle.getId(), PlanillaMapper.detalle(detalle));
    }

    /** Crea el detalle por defecto de cada empleado Activo que aun no lo tiene; devuelve solo los creados. */
    @POST
    @Path("/{id}/detalles/generar")
    public Response generarDetalles(@PathParam("id") Long id) {
        List<PlanillaDetalle> creados = servicio.generarDetalles(id);
        return Respuestas.ok(creados.stream().map(PlanillaMapper::detalle).toList());
    }

    @GET
    @Path("/detalles/{detalleId}")
    public Response obtenerDetalle(@PathParam("detalleId") Long detalleId) {
        return Respuestas.ok(PlanillaMapper.detalle(servicio.buscarDetalle(detalleId)));
    }

    /** Cambia dias y horas de un detalle de una planilla Pendiente (el empleado no se puede cambiar). */
    @PUT
    @Path("/detalles/{detalleId}")
    public Response modificarDetalle(@PathParam("detalleId") Long detalleId, @Valid DetallePlanillaRequest cuerpo) {
        return Respuestas.ok(PlanillaMapper.detalle(
                servicio.modificarDetalle(detalleId, Respuestas.requerir(cuerpo).aCmd())));
    }

    @DELETE
    @Path("/detalles/{detalleId}")
    public Response eliminarDetalle(@PathParam("detalleId") Long detalleId) {
        servicio.eliminarDetalle(detalleId);
        return Respuestas.sinContenido();
    }

    // ------------------------------------------------------------------------------ calcular / pagar

    /** Calculo de la planilla (no escribe nada). */
    @GET
    @Path("/{id}/calculo")
    public Response calcular(@PathParam("id") Long id) {
        return Respuestas.ok(PlanillaMapper.resumen(servicio.calcular(id)));
    }

    /** Paga la planilla: pasa a Pagado y registra su partida de egreso, en una sola transaccion. */
    @POST
    @Path("/{id}/pagar")
    public Response pagar(@PathParam("id") Long id) {
        return Respuestas.ok(PlanillaMapper.pago(servicio.pagar(id)));
    }
}
