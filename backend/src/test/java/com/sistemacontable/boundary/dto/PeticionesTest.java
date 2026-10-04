package com.sistemacontable.boundary.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.control.service.cmd.AjusteCmd;
import com.sistemacontable.control.service.cmd.CompraCmd;
import com.sistemacontable.control.service.cmd.CuentaCmd;
import com.sistemacontable.control.service.cmd.EmpleadoCmd;
import com.sistemacontable.control.service.cmd.LineaCompraCmd;
import com.sistemacontable.control.service.cmd.LineaPartidaCmd;
import com.sistemacontable.control.service.cmd.LineaTrasladoCmd;
import com.sistemacontable.control.service.cmd.LineaVentaCmd;
import com.sistemacontable.control.service.cmd.PartidaCmd;
import com.sistemacontable.control.service.cmd.TrasladoCmd;
import com.sistemacontable.control.service.cmd.VentaCmd;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Restricciones de los DTO de entrada (400 si faltan datos estructurales) y su conversion a comandos del servicio. */
class PeticionesTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> campos(Object peticion) {
        Set<String> campos = new TreeSet<>();
        for (ConstraintViolation<Object> v : validador.validate(peticion)) {
            campos.add(v.getPropertyPath().toString());
        }
        return campos;
    }

    // -------------------------------------------------------------------------------- validacion

    @Test
    void unaPeticionCompletaNoTieneViolaciones() {
        assertTrue(campos(new DepartamentoRequest("Ventas", null)).isEmpty());
        assertTrue(campos(new CompraRequest(1L, 2L, null, null,
                List.of(new CompraRequest.Linea(3L, 4L, BigDecimal.ONE, BigDecimal.TEN)))).isEmpty());
    }

    @Test
    void faltanLosCamposObligatorios() {
        assertEquals(Set.of("nombre"), campos(new DepartamentoRequest("  ", "x")));
        assertEquals(Set.of("nombre", "apellido", "departamentoId", "salario", "numeroIsss", "dui"),
                campos(new EmpleadoRequest(null, null, null, null, null, null, null, null)));
        assertEquals(Set.of("codigo", "nombre"), campos(new CuentaRequest(null, "", null, null, null)));
        assertEquals(Set.of("nombre", "nit"), campos(new ClienteRequest("", null, " ")));
        assertEquals(Set.of("almacenId", "loteId", "cantidad", "motivo"),
                campos(new AjusteRequest(null, null, null, null)));
        assertEquals(Set.of("fechaInicio", "fechaFin", "fechaPago"),
                campos(new PlanillaRequest(null, null, null, null)));
    }

    @Test
    void lasLineasDeUnDocumentoSeValidanConSuIndice() {
        CompraRequest compra = new CompraRequest(1L, 2L, null, null,
                Arrays.asList(new CompraRequest.Linea(null, 4L, BigDecimal.ONE, null), null));

        Set<String> campos = campos(compra);

        assertTrue(campos.contains("lineas[0].loteId"), campos.toString());
        assertTrue(campos.contains("lineas[0].precio"), campos.toString());
        assertTrue(campos.stream().anyMatch(c -> c.startsWith("lineas[1]")), campos.toString());
    }

    @Test
    void lasReglasDeNegocioNoSeDuplicanEnElDto() {
        // cantidad negativa, lineas ausentes o fecha de planilla invertida son reglas del servicio (422), no del DTO
        assertTrue(campos(new VentaRequest(1L, 2L, null, null,
                List.of(new VentaRequest.Linea(3L, 4L, new BigDecimal("-1"), BigDecimal.ZERO)))).isEmpty());
        assertTrue(campos(new VentaRequest(1L, 2L, null, null, null)).isEmpty());
        assertTrue(campos(new TrasladoRequest(1L, 1L, null, List.of())).isEmpty());
    }

    // ---------------------------------------------------------------------------- a comandos

    @Test
    void laCompraSeConvierteEnSuComando() {
        CompraRequest r = new CompraRequest(1L, 2L, LocalDate.of(2026, 3, 1), "Reposicion", List.of(
                new CompraRequest.Linea(3L, 4L, new BigDecimal("5"), new BigDecimal("1.25")),
                new CompraRequest.Linea(6L, 7L, BigDecimal.ONE, BigDecimal.ZERO)));

        CompraCmd cmd = r.aCmd();

        assertEquals(1L, cmd.proveedorId());
        assertEquals(2L, cmd.empleadoId());
        assertEquals(LocalDate.of(2026, 3, 1), cmd.fecha());
        assertEquals("Reposicion", cmd.descripcion());
        assertEquals(List.of(new LineaCompraCmd(3L, 4L, new BigDecimal("5"), new BigDecimal("1.25")),
                new LineaCompraCmd(6L, 7L, BigDecimal.ONE, BigDecimal.ZERO)), cmd.lineas());
    }

    @Test
    void sinLineasElComandoQuedaConUnaListaVacia() {
        assertTrue(new CompraRequest(1L, 2L, null, null, null).aCmd().lineas().isEmpty());
        assertTrue(new VentaRequest(1L, 2L, null, null, null).aCmd().lineas().isEmpty());
        assertTrue(new TrasladoRequest(1L, 2L, null, null).aCmd().lineas().isEmpty());
        assertTrue(new PartidaRequest(LocalDate.now(), "c", TipoPartida.Diario, null, null, null, null).aCmd()
                .lineas().isEmpty());
    }

    @Test
    void laVentaElTrasladoYElAjusteSeConviertenEnSusComandos() {
        VentaCmd venta = new VentaRequest(1L, 2L, null, "d", List.of(
                new VentaRequest.Linea(3L, 4L, BigDecimal.ONE, BigDecimal.TEN))).aCmd();
        TrasladoCmd traslado = new TrasladoRequest(1L, 2L, "mover", List.of(
                new TrasladoRequest.Linea(3L, new BigDecimal("4")))).aCmd();
        AjusteCmd ajuste = new AjusteRequest(1L, 2L, new BigDecimal("-3"), "merma").aCmd();

        assertEquals(1L, venta.clienteId());
        assertNull(venta.fecha());
        assertEquals(List.of(new LineaVentaCmd(3L, 4L, BigDecimal.ONE, BigDecimal.TEN)), venta.lineas());
        assertEquals(1L, traslado.almacenOrigenId());
        assertEquals(2L, traslado.almacenDestinoId());
        assertEquals(List.of(new LineaTrasladoCmd(3L, new BigDecimal("4"))), traslado.lineas());
        assertEquals(new AjusteCmd(1L, 2L, new BigDecimal("-3"), "merma"), ajuste);
    }

    @Test
    void laPartidaConservaTipoReferenciasYLineas() {
        PartidaCmd cmd = new PartidaRequest(LocalDate.of(2026, 3, 4), "Ajuste", TipoPartida.Ajuste, null, 9L, 5L,
                List.of(new PartidaRequest.Linea(1L, new BigDecimal("10"), null),
                        new PartidaRequest.Linea(2L, null, new BigDecimal("10")))).aCmd();

        assertEquals(TipoPartida.Ajuste, cmd.tipo());
        assertNull(cmd.compraId());
        assertEquals(9L, cmd.ventaId());
        assertEquals(5L, cmd.terceroId());
        assertEquals(List.of(new LineaPartidaCmd(1L, new BigDecimal("10"), null),
                new LineaPartidaCmd(2L, null, new BigDecimal("10"))), cmd.lineas());
    }

    @Test
    void elEmpleadoYLaCuentaSeConviertenCampoACampo() {
        EmpleadoCmd e = new EmpleadoRequest("Ana", "Lopez", LocalDate.of(1990, 1, 2), 3L, new BigDecimal("500"),
                EstadoEmpleado.Inactivo, "111", "222").aCmd();
        CuentaCmd c = new CuentaRequest(1105, "Caja", NaturalezaCuenta.Deudora, true, 11L).aCmd();

        assertEquals(new EmpleadoCmd("Ana", "Lopez", LocalDate.of(1990, 1, 2), 3L, new BigDecimal("500"),
                EstadoEmpleado.Inactivo, "111", "222"), e);
        assertEquals(new CuentaCmd(1105, "Caja", NaturalezaCuenta.Deudora, true, 11L), c);
    }

    @Test
    void todasLasPeticionesTienenUnMetodoACmdQueNoPierdeCampos() {
        // sanidad estructural: cada record Request convierte a un comando con tantos componentes (o los lleva en lineas)
        List<Class<?>> peticiones = new ArrayList<>(List.of(DepartamentoRequest.class, EmpleadoRequest.class,
                CuentaRequest.class, TerceroRequest.class, ClienteRequest.class, ProveedorRequest.class,
                UnidadMedidaRequest.class, TipoProductoRequest.class, ProductoRequest.class, LoteRequest.class,
                AlmacenRequest.class, PlanillaRequest.class, DetallePlanillaRequest.class, AjusteRequest.class,
                CompraRequest.class, VentaRequest.class, TrasladoRequest.class, PartidaRequest.class));
        for (Class<?> tipo : peticiones) {
            assertTrue(tipo.isRecord(), tipo + " debe ser un record");
            boolean tieneACmd = Arrays.stream(tipo.getMethods()).anyMatch(m -> m.getName().equals("aCmd"));
            assertTrue(tieneACmd, tipo.getSimpleName() + " debe poder convertirse en un comando");
            assertFalse(Arrays.stream(tipo.getRecordComponents()).anyMatch(c -> c.getType().getName().contains("entity")
                    && !c.getType().isEnum()), tipo + " no debe exponer entidades JPA");
        }
    }
}
