package com.sistemacontable.boundary;

import com.sistemacontable.security.Roles;
import com.sistemacontable.security.RolApp;
import com.sistemacontable.boundary.dto.EmpleadoRequest;
import com.sistemacontable.boundary.mapper.CatalogoMapper;
import com.sistemacontable.control.service.EmpleadoService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.DELETE;
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

/**
 * Empleados (el estado Activo/Inactivo/Suspendido se cambia con PUT).
 *
 * <p>Solo delega en {@link EmpleadoService}: sin reglas de negocio ni acceso a datos. La transaccion abarca la
 * peticion para que el mapeo a DTO lea las relaciones perezosas dentro de ella.</p>
 */
@Roles({RolApp.Contabilidad, RolApp.RRHH})
@Path("/empleados")
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmpleadoResource {

    @Inject
    private EmpleadoService servicio;

    @GET
    public Response listar(@QueryParam("first") @DefaultValue(Paginacion.PRIMERO_POR_DEFECTO_TEXTO) int first,
            @QueryParam("max") @DefaultValue(Paginacion.MAX_POR_DEFECTO_TEXTO) int max,
            @QueryParam("estado") EstadoEmpleado estado,@QueryParam("departamentoId") Long departamentoId,@QueryParam("buscar") String buscar) {
        Paginacion pagina = Paginacion.de(first, max);
        return Respuestas.pagina(servicio.buscar(estado,departamentoId,buscar,pagina.first(),pagina.max()), servicio.contar(estado,departamentoId,buscar),
                CatalogoMapper::empleado);
    }

    @Roles({RolApp.Ventas,RolApp.Bodega,RolApp.Contabilidad,RolApp.Administrador,RolApp.RRHH})
    @GET @Path("/seleccion")
    public Response seleccion() { return Respuestas.ok(servicio.activos().stream().map(e -> java.util.Map.of("id",e.getId(),"nombreCompleto",e.getNombre()+" "+e.getApellido(),"departamento",e.getDepartamento().getNombre())).toList()); }

    @Roles({RolApp.RRHH})
    @POST @Path("/{id}/activar") public Response activar(@PathParam("id") Long id) { return Respuestas.ok(CatalogoMapper.empleado(servicio.cambiarEstado(id,EstadoEmpleado.Activo))); }
    @Roles({RolApp.RRHH})
    @POST @Path("/{id}/inactivar") public Response inactivar(@PathParam("id") Long id) { return Respuestas.ok(CatalogoMapper.empleado(servicio.cambiarEstado(id,EstadoEmpleado.Inactivo))); }

    @GET
    @Path("/{id}")
    public Response obtener(@PathParam("id") Long id) {
        return Respuestas.ok(CatalogoMapper.empleado(servicio.buscarPorId(id)));
    }

    @Roles({RolApp.RRHH})
    @POST
    public Response crear(@Valid EmpleadoRequest cuerpo, @Context UriInfo uriInfo) {
        var creado = servicio.crear(Respuestas.requerir(cuerpo).aCmd());
        return Respuestas.creado(uriInfo, "empleados", creado.getId(), CatalogoMapper.empleado(creado));
    }

    @Roles({RolApp.RRHH})
    @PUT
    @Path("/{id}")
    public Response modificar(@PathParam("id") Long id, @Valid EmpleadoRequest cuerpo) {
        return Respuestas.ok(CatalogoMapper.empleado(servicio.modificar(id, Respuestas.requerir(cuerpo).aCmd())));
    }

    @Roles({RolApp.RRHH})
    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") Long id) {
        servicio.eliminar(id);
        return Respuestas.sinContenido();
    }
}
