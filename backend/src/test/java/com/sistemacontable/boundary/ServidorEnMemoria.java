package com.sistemacontable.boundary;

import static org.mockito.Mockito.mock;

import com.sistemacontable.control.service.AjusteService;
import com.sistemacontable.control.service.AlmacenService;
import com.sistemacontable.control.service.BalanzaService;
import com.sistemacontable.control.service.ClienteService;
import com.sistemacontable.control.service.CompraService;
import com.sistemacontable.control.service.CuentaService;
import com.sistemacontable.control.service.DepartamentoService;
import com.sistemacontable.control.service.EmpleadoService;
import com.sistemacontable.control.service.ExistenciasService;
import com.sistemacontable.control.service.KardexService;
import com.sistemacontable.control.service.LoteService;
import com.sistemacontable.control.service.PartidaService;
import com.sistemacontable.control.service.PlanillaService;
import com.sistemacontable.control.service.ProductoService;
import com.sistemacontable.control.service.ProveedorService;
import com.sistemacontable.control.service.TerceroService;
import com.sistemacontable.control.service.TipoProductoService;
import com.sistemacontable.control.service.TrasladoService;
import com.sistemacontable.control.service.UnidadMedidaService;
import com.sistemacontable.control.service.VentaService;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

/**
 * Servidor JAX-RS en memoria (Jersey, solo en pruebas) con los recursos y el {@link ManejadorExcepciones} reales y
 * servicios simulados con Mockito: permite comprobar estados, cabeceras y JSON reales sin Liberty ni base de datos.
 * Las anotaciones de CDI y JTA ({@code @RequestScoped}, {@code @Transactional}) no actuan aqui; su efecto en
 * Liberty no se ejerce en estas pruebas.
 */
final class ServidorEnMemoria {

    static final List<Class<?>> RECURSOS = List.of(DepartamentoResource.class, EmpleadoResource.class,
            CuentaResource.class, TerceroResource.class, ClienteResource.class, ProveedorResource.class,
            UnidadMedidaResource.class, TipoProductoResource.class, ProductoResource.class, LoteResource.class,
            AlmacenResource.class, CompraResource.class, VentaResource.class, TrasladoResource.class,
            AjusteResource.class, KardexResource.class, ExistenciasResource.class, PartidaResource.class,
            BalanzaResource.class, PlanillaResource.class);

    final DepartamentoService departamentos = mock(DepartamentoService.class);
    final EmpleadoService empleados = mock(EmpleadoService.class);
    final CuentaService cuentas = mock(CuentaService.class);
    final TerceroService terceros = mock(TerceroService.class);
    final ClienteService clientes = mock(ClienteService.class);
    final ProveedorService proveedores = mock(ProveedorService.class);
    final UnidadMedidaService unidades = mock(UnidadMedidaService.class);
    final TipoProductoService tipos = mock(TipoProductoService.class);
    final ProductoService productos = mock(ProductoService.class);
    final LoteService lotes = mock(LoteService.class);
    final AlmacenService almacenes = mock(AlmacenService.class);
    final CompraService compras = mock(CompraService.class);
    final VentaService ventas = mock(VentaService.class);
    final TrasladoService traslados = mock(TrasladoService.class);
    final AjusteService ajustes = mock(AjusteService.class);
    final KardexService kardex = mock(KardexService.class);
    final ExistenciasService existencias = mock(ExistenciasService.class);
    final PartidaService partidas = mock(PartidaService.class);
    final BalanzaService balanza = mock(BalanzaService.class);
    final PlanillaService planillas = mock(PlanillaService.class);

    private final JerseyTest servidor;

    ServidorEnMemoria() {
        ResourceConfig config = new ResourceConfig();
        RECURSOS.forEach(config::register);
        config.register(ManejadorExcepciones.class);
        config.register(ManejadorValidacion.class);
        config.register(new AbstractBinder() {
            @SuppressWarnings({"unchecked", "rawtypes"})
            private void enlazar(Object servicio) {
                bind(servicio).to((Class) tipoDe(servicio));
            }

            @Override
            protected void configure() {
                for (Object servicio : List.of(departamentos, empleados, cuentas, terceros, clientes, proveedores,
                        unidades, tipos, productos, lotes, almacenes, compras, ventas, traslados, ajustes, kardex,
                        existencias, partidas, balanza, planillas)) {
                    enlazar(servicio);
                }
            }
        });
        servidor = new JerseyTest(config) {
        };
    }

    /** El mock es una subclase generada: se enlaza por la clase del servicio real. */
    private static Class<?> tipoDe(Object mock) {
        Class<?> clase = mock.getClass();
        while (!clase.getPackageName().equals("com.sistemacontable.control.service")) {
            clase = clase.getSuperclass();
        }
        return clase;
    }

    void iniciar() throws Exception {
        servidor.setUp();
    }

    void detener() throws Exception {
        servidor.tearDown();
    }

    WebTarget ruta(String ruta) {
        return servidor.target(ruta);
    }

    Response get(String rutaConConsulta) {
        return cliente(rutaConConsulta).get();
    }

    Response delete(String ruta) {
        return cliente(ruta).delete();
    }

    Response post(String ruta, String json) {
        return cliente(ruta).post(Entity.entity(json, MediaType.APPLICATION_JSON));
    }

    /** POST sin cuerpo ni tipo de contenido (acciones como /activar o /pagar). */
    Response postVacio(String ruta) {
        return cliente(ruta).post(null);
    }

    Response put(String ruta, String json) {
        return cliente(ruta).put(Entity.entity(json, MediaType.APPLICATION_JSON));
    }

    private jakarta.ws.rs.client.Invocation.Builder cliente(String rutaConConsulta) {
        String ruta = rutaConConsulta;
        WebTarget destino;
        int i = rutaConConsulta.indexOf('?');
        if (i >= 0) {
            destino = servidor.target(rutaConConsulta.substring(0, i));
            for (String par : rutaConConsulta.substring(i + 1).split("&")) {
                String[] kv = par.split("=", 2);
                destino = destino.queryParam(kv[0], kv.length > 1 ? kv[1] : "");
            }
        } else {
            destino = servidor.target(ruta);
        }
        return destino.request(MediaType.APPLICATION_JSON);
    }
}
