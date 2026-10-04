package com.sistemacontable.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.Balanza;
import com.sistemacontable.control.service.FilaBalanza;
import com.sistemacontable.control.service.MovimientoKardex;
import com.sistemacontable.control.service.ResultadoAjuste;
import com.sistemacontable.control.service.ResultadoCompra;
import com.sistemacontable.control.service.cmd.AjusteCmd;
import com.sistemacontable.control.service.cmd.CompraCmd;
import com.sistemacontable.control.service.cmd.CuentaCmd;
import com.sistemacontable.control.service.cmd.DepartamentoCmd;
import com.sistemacontable.control.service.cmd.LineaCompraCmd;
import com.sistemacontable.control.service.cmd.LineaVentaCmd;
import com.sistemacontable.control.service.cmd.VentaCmd;
import com.sistemacontable.control.service.cmd.PartidaCmd;
import com.sistemacontable.control.service.cmd.TrasladoCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.control.service.ResultadoVenta;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Departamento;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Existencias;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Planilla;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.ejb.EJBException;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Los recursos REST contra un servidor JAX-RS en memoria: estados, cabeceras ({@code Location},
 * {@code Total-Records}), JSON de los records y el traductor de excepciones de extremo a extremo.
 */
class RecursosRestTest {

    private ServidorEnMemoria s;

    @BeforeEach
    void iniciar() throws Exception {
        s = new ServidorEnMemoria();
        s.iniciar();
    }

    @AfterEach
    void detener() throws Exception {
        s.detener();
    }

    private static JsonObject json(Response r) {
        String cuerpo = r.readEntity(String.class);
        return Json.createReader(new StringReader(cuerpo)).readObject();
    }

    private static <T extends com.sistemacontable.entity.EntidadBase> T conId(T entidad, long id) {
        entidad.setId(id);
        return entidad;
    }

    private static Departamento departamento(long id, String nombre) {
        Departamento d = conId(new Departamento(), id);
        d.setNombre(nombre);
        return d;
    }

    // ------------------------------------------------------------------------------------ paginacion

    @Test
    void elListadoUsaFirst0Max50PorDefectoYDevuelveElTotalEnLaCabecera() {
        when(s.departamentos.listar(0, 50)).thenReturn(List.of(departamento(1, "Ventas"), departamento(2, "Bodega")));
        when(s.departamentos.contar()).thenReturn(120L);

        Response r = s.get("/departamentos");

        assertEquals(200, r.getStatus());
        assertEquals("120", r.getHeaderString("Total-Records"));
        String cuerpo = r.readEntity(String.class);
        assertTrue(cuerpo.contains("\"nombre\":\"Ventas\""), cuerpo);
        assertTrue(cuerpo.contains("\"nombre\":\"Bodega\""), cuerpo);
        assertFalse(cuerpo.contains("descripcion"), "los campos nulos no se serializan");
    }

    @Test
    void firstIgualAMaxEsUnaPaginaValida() {
        when(s.departamentos.listar(50, 50)).thenReturn(List.of(departamento(51, "Compras")));
        when(s.departamentos.contar()).thenReturn(120L);

        Response r = s.get("/departamentos?first=50&max=50");

        assertEquals(200, r.getStatus());
        assertEquals("120", r.getHeaderString("Total-Records"));
        verify(s.departamentos).listar(50, 50);
    }

    @Test
    void elTotalSeDevuelveComoLongSinTruncar() {
        when(s.departamentos.listar(0, 10)).thenReturn(List.of());
        when(s.departamentos.contar()).thenReturn(5_000_000_000L);

        Response r = s.get("/departamentos?max=10");

        assertEquals("5000000000", r.getHeaderString("Total-Records"));
        assertEquals("[]", r.readEntity(String.class));
    }

    @Test
    void unaPaginaInvalidaEs400ConCuerpoDeErrorYNoLlegaAlServicio() {
        for (String consulta : List.of("?first=-1", "?max=0", "?max=101", "?max=-3")) {
            Response r = s.get("/departamentos" + consulta);

            assertEquals(400, r.getStatus(), consulta);
            JsonObject error = json(r);
            assertEquals("SOLICITUD_INVALIDA", error.getString("codigo"), consulta);
            assertFalse(error.getString("mensaje").isBlank());
        }
        verifyNoInteractions(s.departamentos);
    }

    @Test
    void todosLosListadosPaginanYExponenElTotal() {
        when(s.empleados.contar()).thenReturn(1L);
        when(s.cuentas.contar()).thenReturn(2L);
        when(s.terceros.contar()).thenReturn(3L);
        when(s.clientes.contar()).thenReturn(4L);
        when(s.proveedores.contar()).thenReturn(5L);
        when(s.unidades.contar()).thenReturn(6L);
        when(s.tipos.contar()).thenReturn(7L);
        when(s.productos.contar()).thenReturn(8L);
        when(s.lotes.contar()).thenReturn(9L);
        when(s.almacenes.contar()).thenReturn(10L);
        when(s.compras.contar()).thenReturn(11L);
        when(s.ventas.contar()).thenReturn(12L);
        when(s.kardex.contar()).thenReturn(13L);
        when(s.existencias.contar()).thenReturn(14L);
        when(s.partidas.contar()).thenReturn(15L);
        when(s.planillas.contar()).thenReturn(16L);
        String[] rutas = {"/empleados", "/cuentas", "/terceros", "/clientes", "/proveedores", "/unidades-medida",
                "/tipos-producto", "/productos", "/lotes", "/almacenes", "/compras", "/ventas", "/kardex",
                "/existencias", "/partidas", "/planillas"};

        for (int i = 0; i < rutas.length; i++) {
            Response r = s.get(rutas[i] + "?first=20&max=20");

            assertEquals(200, r.getStatus(), rutas[i]);
            assertEquals(String.valueOf(i + 1), r.getHeaderString("Total-Records"), rutas[i]);
            assertEquals("[]", r.readEntity(String.class), rutas[i]);
        }
    }

    // ------------------------------------------------------------------------------- CRUD de catalogo

    @Test
    void obtenerPorIdDevuelveElDto() {
        when(s.departamentos.buscarPorId(3L)).thenReturn(departamento(3, "Ventas"));

        Response r = s.get("/departamentos/3");

        assertEquals(200, r.getStatus());
        JsonObject dto = json(r);
        assertEquals(3, dto.getInt("id"));
        assertEquals("Ventas", dto.getString("nombre"));
    }

    @Test
    void unIdInexistenteEs404ConSuCodigo() {
        when(s.departamentos.buscarPorId(99L)).thenThrow(new NoEncontradoException("Departamento", 99L));

        Response r = s.get("/departamentos/99");

        assertEquals(404, r.getStatus());
        JsonObject error = json(r);
        assertEquals("NO_ENCONTRADO", error.getString("codigo"));
        assertEquals("Departamento con id 99 no existe", error.getString("mensaje"));
    }

    @Test
    void crearDevuelve201ConLocationYElRecursoCreado() {
        when(s.departamentos.crear(any(DepartamentoCmd.class))).thenReturn(departamento(7, "Ventas"));

        Response r = s.post("/departamentos", "{\"nombre\":\"Ventas\",\"descripcion\":\"Area comercial\"}");

        assertEquals(201, r.getStatus());
        assertTrue(r.getHeaderString("Location").endsWith("/departamentos/7"), r.getHeaderString("Location"));
        assertEquals(7, json(r).getInt("id"));
        ArgumentCaptor<DepartamentoCmd> cmd = ArgumentCaptor.forClass(DepartamentoCmd.class);
        verify(s.departamentos).crear(cmd.capture());
        assertEquals(new DepartamentoCmd("Ventas", "Area comercial"), cmd.getValue());
    }

    @Test
    void modificarDevuelve200ConElRecursoActualizado() {
        when(s.departamentos.modificar(anyLong(), any(DepartamentoCmd.class)))
                .thenReturn(departamento(7, "Ventas y mercadeo"));

        Response r = s.put("/departamentos/7", "{\"nombre\":\"Ventas y mercadeo\"}");

        assertEquals(200, r.getStatus());
        assertEquals("Ventas y mercadeo", json(r).getString("nombre"));
        verify(s.departamentos).modificar(7L, new DepartamentoCmd("Ventas y mercadeo", null));
    }

    @Test
    void eliminarDevuelve204() {
        Response r = s.delete("/departamentos/7");

        assertEquals(204, r.getStatus());
        verify(s.departamentos).eliminar(7L);
    }

    @Test
    void eliminarConReferenciasEs422ConElMotivo() {
        org.mockito.Mockito.doThrow(new ReglaNegocioException("No se puede eliminar el departamento 7 porque tiene empleados"))
                .when(s.departamentos).eliminar(7L);

        Response r = s.delete("/departamentos/7");

        assertEquals(422, r.getStatus());
        JsonObject error = json(r);
        assertEquals("REGLA_NEGOCIO", error.getString("codigo"));
        assertTrue(error.getString("mensaje").contains("tiene empleados"));
    }

    @Test
    void unaReglaDeNegocioAlCrearEs422() {
        when(s.departamentos.crear(any())).thenThrow(new ReglaNegocioException("El nombre es obligatorio"));

        Response r = s.post("/departamentos", "{\"nombre\":\"x\"}");

        assertEquals(422, r.getStatus());
    }

    // ----------------------------------------------------------------- cuerpos invalidos (400/415)

    @Test
    void unPostSinCuerpoEs400() {
        Response r = s.postVacio("/departamentos");

        assertTrue(r.getStatus() == 400 || r.getStatus() == 415, "estado " + r.getStatus());
        verify(s.departamentos, never()).crear(any());
    }

    @Test
    void unJsonMalFormadoEs400() {
        Response r = s.post("/departamentos", "{\"nombre\": ");

        assertEquals(400, r.getStatus());
        assertEquals("JSON_INVALIDO", json(r).getString("codigo"));
        verify(s.departamentos, never()).crear(any());
    }

    @Test
    void unValorDeEnumDesconocidoEs400() {
        Response r = s.post("/cuentas", "{\"codigo\":1,\"nombre\":\"Caja\",\"naturaleza\":\"Neutra\"}");

        assertEquals(400, r.getStatus());
        assertEquals("JSON_INVALIDO", json(r).getString("codigo"));
        verify(s.cuentas, never()).crear(any());
    }

    @Test
    void unTipoDeContenidoNoSoportadoEs415ConCuerpoJson() {
        Response r = s.ruta("/departamentos").request().post(
                jakarta.ws.rs.client.Entity.entity("nombre=Ventas", "text/plain"));

        assertEquals(415, r.getStatus());
        assertEquals("HTTP_415", json(r).getString("codigo"));
    }

    @Test
    void unaRutaInexistenteEs404ConCuerpoJson() {
        Response r = s.get("/no-existe");

        assertEquals(404, r.getStatus());
        assertEquals("HTTP_404", json(r).getString("codigo"));
    }

    // ------------------------------------------------------------------------ metodos no permitidos

    @Test
    void lasCuentasNoSeBorranConDeleteYElRecursoLoDeclara() {
        Response r = s.delete("/cuentas/1");

        assertEquals(405, r.getStatus());
        assertNotNull(r.getHeaderString("Allow"));
        assertEquals("HTTP_405", json(r).getString("codigo"));
    }

    @Test
    void lasPartidasSonInmutablesPutYDeleteDan405() {
        Response put = s.put("/partidas/1", "{\"concepto\":\"x\"}");
        Response del = s.delete("/partidas/1");

        assertEquals(405, put.getStatus());
        assertEquals(405, del.getStatus());
        verifyNoInteractions(s.partidas);
    }

    @Test
    void kardexExistenciasYComprasSonSoloLectura() {
        assertEquals(405, s.post("/kardex", "{}").getStatus());
        assertEquals(405, s.put("/kardex/1", "{}").getStatus());
        assertEquals(405, s.delete("/kardex/1").getStatus());
        assertEquals(405, s.post("/existencias", "{}").getStatus());
        assertEquals(405, s.put("/existencias/1", "{}").getStatus());
        assertEquals(405, s.delete("/existencias/1").getStatus());
        assertEquals(405, s.put("/compras/1", "{}").getStatus());
        assertEquals(405, s.delete("/compras/1").getStatus());
        assertEquals(405, s.put("/ventas/1", "{}").getStatus());
        assertEquals(405, s.delete("/ventas/1").getStatus());
        verifyNoInteractions(s.kardex, s.existencias, s.compras, s.ventas);
    }

    // ------------------------------------------------------------------------------- cuentas (extras)

    @Test
    void activarUnaCuentaNoNecesitaCuerpo() {
        Cuenta c = conId(new Cuenta(), 4L);
        c.setCodigo(11);
        c.setNombre("Caja");
        c.setNaturaleza(NaturalezaCuenta.Deudora);
        when(s.cuentas.activar(4L)).thenReturn(c);

        Response r = s.postVacio("/cuentas/4/activar");

        assertEquals(200, r.getStatus());
        JsonObject dto = json(r);
        assertEquals("Deudora", dto.getString("naturaleza"));
        assertTrue(dto.getBoolean("activo"));
    }

    @Test
    void crearUnaCuentaMapeaElEnumYElPadre() {
        Cuenta c = conId(new Cuenta(), 5L);
        c.setCodigo(1105);
        c.setNombre("Caja chica");
        c.setNaturaleza(NaturalezaCuenta.Deudora);
        when(s.cuentas.crear(any(CuentaCmd.class))).thenReturn(c);

        Response r = s.post("/cuentas",
                "{\"codigo\":1105,\"nombre\":\"Caja chica\",\"naturaleza\":\"Deudora\",\"imputable\":true,\"padreId\":11}");

        assertEquals(201, r.getStatus());
        ArgumentCaptor<CuentaCmd> cmd = ArgumentCaptor.forClass(CuentaCmd.class);
        verify(s.cuentas).crear(cmd.capture());
        assertEquals(new CuentaCmd(1105, "Caja chica", NaturalezaCuenta.Deudora, true, 11L), cmd.getValue());
    }

    // ------------------------------------------------------------------------ compras (transaccional)

    private Compra compra(long id) {
        Compra c = conId(new Compra(), id);
        c.setFecha(LocalDate.of(2026, 3, 1));
        c.setProveedor(conId(new Proveedor(), 3L));
        c.setEmpleado(conId(new Empleado(), 4L));
        return c;
    }

    @Test
    void registrarUnaCompraDevuelve201ConLocationYElResultadoCompleto() {
        Compra compra = compra(10);
        Kardex k = conId(new Kardex(), 20L);
        k.setTipoMovimiento(TipoMovimiento.Entrada);
        k.setDestino(conId(new Almacen(), 2L));
        k.setFecha(OffsetDateTime.of(2026, 3, 1, 8, 0, 0, 0, ZoneOffset.UTC));
        KardexDetalle kd = conId(new KardexDetalle(), 200L);
        kd.setLote(conId(new Lote(), 5L));
        kd.setCantidad(new BigDecimal("2"));
        kd.setPrecioCosto(new BigDecimal("10.25"));
        when(s.compras.registrar(any(CompraCmd.class))).thenReturn(new ResultadoCompra(compra, List.of(),
                List.of(new MovimientoKardex(k, List.of(kd))), conId(new Partida(), 50L), new BigDecimal("20.50"),
                new BigDecimal("2.67"), new BigDecimal("23.17")));

        Response r = s.post("/compras", "{\"proveedorId\":3,\"empleadoId\":4,\"fecha\":\"2026-03-01\","
                + "\"porcentajeContado\":40,\"lineas\":[{\"loteId\":5,\"almacenId\":2,\"cantidad\":2,\"precio\":10.25}]}");

        assertEquals(201, r.getStatus());
        assertTrue(r.getHeaderString("Location").endsWith("/compras/10"), r.getHeaderString("Location"));
        JsonObject dto = json(r);
        assertEquals(10, dto.getJsonObject("compra").getInt("id"));
        assertEquals("2026-03-01", dto.getJsonObject("compra").getString("fecha"));
        assertEquals(50, dto.getInt("partidaId"));
        assertEquals(new BigDecimal("23.17"), dto.getJsonNumber("total").bigDecimalValue());
        assertEquals("2026-03-01T08:00:00Z", dto.getJsonArray("movimientos").getJsonObject(0).getString("fecha"));
        assertEquals("Entrada", dto.getJsonArray("movimientos").getJsonObject(0).getString("tipoMovimiento"));
        ArgumentCaptor<CompraCmd> cmd = ArgumentCaptor.forClass(CompraCmd.class);
        verify(s.compras).registrar(cmd.capture());
        assertEquals(new CompraCmd(3L, 4L, LocalDate.of(2026, 3, 1), null,
                new BigDecimal("40"),
                List.of(new LineaCompraCmd(5L, 2L, new BigDecimal("2"), new BigDecimal("10.25")))), cmd.getValue());
    }

    @Test
    void registrarUnaVentaPasaElPorcentajeDeContadoAlComando() {
        Venta venta = conId(new Venta(), 11L);
        venta.setFecha(LocalDate.of(2026, 3, 1));
        venta.setCliente(conId(new Cliente(), 3L));
        venta.setEmpleado(conId(new Empleado(), 4L));
        when(s.ventas.registrar(any(VentaCmd.class))).thenReturn(new ResultadoVenta(venta, List.of(), List.of(),
                conId(new Partida(), 51L), new BigDecimal("20.00"), new BigDecimal("2.60"), new BigDecimal("22.60"),
                new BigDecimal("8.00")));

        Response r = s.post("/ventas", "{\"clienteId\":3,\"empleadoId\":4,\"fecha\":\"2026-03-01\","
                + "\"porcentajeContado\":33.33,\"lineas\":[{\"loteId\":5,\"almacenId\":2,\"cantidad\":2,"
                + "\"precio\":10}]}");

        assertEquals(201, r.getStatus());
        assertEquals(51, json(r).getInt("partidaId"));
        ArgumentCaptor<VentaCmd> cmd = ArgumentCaptor.forClass(VentaCmd.class);
        verify(s.ventas).registrar(cmd.capture());
        assertEquals(new VentaCmd(3L, 4L, LocalDate.of(2026, 3, 1), null, new BigDecimal("33.33"),
                List.of(new LineaVentaCmd(5L, 2L, new BigDecimal("2"), new BigDecimal("10")))), cmd.getValue());
    }

    @Test
    void unaReglaDeNegocioEnvueltaPorElContenedorSigueSiendo422() {
        when(s.compras.registrar(any())).thenThrow(new EJBException(
                new ReglaNegocioException("El almacen 2 esta deshabilitado")));

        Response r = s.post("/compras", "{\"proveedorId\":3,\"empleadoId\":4,\"porcentajeContado\":0}");

        assertEquals(422, r.getStatus());
        JsonObject error = json(r);
        assertEquals("REGLA_NEGOCIO", error.getString("codigo"));
        assertEquals("El almacen 2 esta deshabilitado", error.getString("mensaje"));
    }

    @Test
    void unNoEncontradoEnvueltoPorElContenedorSigueSiendo404() {
        when(s.compras.registrar(any())).thenThrow(new EJBException(new NoEncontradoException("Proveedor", 3L)));

        assertEquals(404, s.post("/compras", "{\"proveedorId\":3,\"empleadoId\":4,\"porcentajeContado\":0}").getStatus());
    }

    @Test
    void unErrorInesperadoEs500GenericoSinFiltrarNada() {
        when(s.compras.registrar(any())).thenThrow(new RuntimeException(
                "ERROR: relation \"public.compra\" does not exist; INSERT INTO compra ..."));

        Response r = s.post("/compras", "{\"proveedorId\":3,\"empleadoId\":4,\"porcentajeContado\":0}");

        assertEquals(500, r.getStatus());
        String cuerpo = r.readEntity(String.class);
        JsonObject error = Json.createReader(new StringReader(cuerpo)).readObject();
        assertEquals("ERROR_INTERNO", error.getString("codigo"));
        assertFalse(error.getString("referencia").isBlank());
        assertFalse(cuerpo.contains("INSERT"));
        assertFalse(cuerpo.contains("relation"));
        assertFalse(cuerpo.contains("Cannot access db"));
    }

    @Test
    void obtenerUnaCompraIncluyeSusLineas() {
        Compra compra = compra(10);
        CompraDetalle d = conId(new CompraDetalle(), 100L);
        d.setLote(conId(new Lote(), 5L));
        d.setAlmacen(conId(new Almacen(), 2L));
        d.setCantidad(new BigDecimal("2"));
        d.setPrecio(new BigDecimal("10.25"));
        when(s.compras.buscarPorId(10L)).thenReturn(compra);
        when(s.compras.detalles(10L)).thenReturn(List.of(d));

        JsonObject dto = json(s.get("/compras/10"));

        assertEquals(1, dto.getJsonArray("lineas").size());
        assertEquals(5, dto.getJsonArray("lineas").getJsonObject(0).getInt("loteId"));
    }

    @Test
    void elListadoDeComprasNoIncluyeLineas() {
        when(s.compras.listar(0, 50)).thenReturn(List.of(compra(10)));
        when(s.compras.contar()).thenReturn(1L);

        String cuerpo = s.get("/compras").readEntity(String.class);

        assertTrue(cuerpo.contains("\"proveedorId\":3"), cuerpo);
        assertFalse(cuerpo.contains("lineas"), cuerpo);
    }

    // ------------------------------------------------------------------ traslados, ajustes, kardex

    @Test
    void unTrasladoApuntaConLocationAlKardex() {
        Kardex k = conId(new Kardex(), 30L);
        k.setTipoMovimiento(TipoMovimiento.Traslado);
        k.setOrigen(conId(new Almacen(), 1L));
        k.setDestino(conId(new Almacen(), 2L));
        when(s.traslados.registrar(any(TrasladoCmd.class))).thenReturn(new MovimientoKardex(k, List.of()));

        Response r = s.post("/traslados", "{\"almacenOrigenId\":1,\"almacenDestinoId\":2,\"lineas\":"
                + "[{\"loteId\":5,\"cantidad\":3}]}");

        assertEquals(201, r.getStatus());
        assertTrue(r.getHeaderString("Location").endsWith("/kardex/30"), r.getHeaderString("Location"));
        assertEquals("Traslado", json(r).getString("tipoMovimiento"));
    }

    @Test
    void unAjusteDevuelve201ConElMovimientoYSinPartidaSiNoSeContabiliza() {
        Kardex k = conId(new Kardex(), 31L);
        k.setTipoMovimiento(TipoMovimiento.Ajuste);
        k.setDestino(conId(new Almacen(), 2L));
        when(s.ajustes.registrar(any(AjusteCmd.class))).thenReturn(new ResultadoAjuste(
                new MovimientoKardex(k, List.of()), null));

        Response r = s.post("/ajustes", "{\"almacenId\":2,\"loteId\":5,\"cantidad\":-3,\"motivo\":\"merma\"}");

        assertEquals(201, r.getStatus());
        assertTrue(r.getHeaderString("Location").endsWith("/kardex/31"));
        JsonObject dto = json(r);
        assertEquals(31, dto.getJsonObject("movimiento").getInt("id"));
        assertFalse(dto.containsKey("partidaId"));
        verify(s.ajustes).registrar(new AjusteCmd(2L, 5L, new BigDecimal("-3"), "merma"));
    }

    @Test
    void existenciasSeConsultaPorId() {
        Existencias e = conId(new Existencias(), 8L);
        e.setLote(conId(new Lote(), 5L));
        e.setAlmacen(conId(new Almacen(), 2L));
        e.setCantidad(new BigDecimal("12.5"));
        when(s.existencias.buscarPorId(8L)).thenReturn(e);

        JsonObject dto = json(s.get("/existencias/8"));

        assertEquals(5, dto.getInt("loteId"));
        assertEquals(new BigDecimal("12.5"), dto.getJsonNumber("cantidad").bigDecimalValue());
    }

    // ------------------------------------------------------------------------------------ partidas

    @Test
    void crearUnaPartidaDevuelve201ConSusLineas() {
        Partida p = conId(new Partida(), 60L);
        p.setFecha(LocalDate.of(2026, 3, 4));
        p.setConcepto("Ajuste manual");
        p.setTipo(TipoPartida.Ajuste);
        PartidaDetalle cargo = conId(new PartidaDetalle(), 600L);
        cargo.setCuenta(conId(new Cuenta(), 1L));
        cargo.setCargo(new BigDecimal("10"));
        PartidaDetalle abono = conId(new PartidaDetalle(), 601L);
        abono.setCuenta(conId(new Cuenta(), 2L));
        abono.setAbono(new BigDecimal("10"));
        when(s.partidas.crear(any(PartidaCmd.class))).thenReturn(p);
        when(s.partidas.lineas(60L)).thenReturn(List.of(cargo, abono));

        Response r = s.post("/partidas", "{\"fecha\":\"2026-03-04\",\"concepto\":\"Ajuste manual\",\"tipo\":\"Ajuste\","
                + "\"lineas\":[{\"cuentaId\":1,\"cargo\":10},{\"cuentaId\":2,\"abono\":10}]}");

        assertEquals(201, r.getStatus());
        assertTrue(r.getHeaderString("Location").endsWith("/partidas/60"));
        JsonObject dto = json(r);
        assertEquals("Ajuste", dto.getString("tipo"));
        assertEquals(2, dto.getJsonArray("lineas").size());
        assertEquals(new BigDecimal("10"), dto.getJsonArray("lineas").getJsonObject(0).getJsonNumber("cargo")
                .bigDecimalValue());
    }

    @Test
    void unaPartidaDescuadradaEs422() {
        when(s.partidas.crear(any())).thenThrow(new ReglaNegocioException("La partida no cuadra"));

        Response r = s.post("/partidas", "{\"fecha\":\"2026-03-04\",\"concepto\":\"x\",\"tipo\":\"Diario\"}");

        assertEquals(422, r.getStatus());
        assertEquals("La partida no cuadra", json(r).getString("mensaje"));
    }

    // ------------------------------------------------------------------------------------ balanza

    @Test
    void laBalanzaExigeAmbasFechas() {
        assertEquals(400, s.get("/balanza").getStatus());
        assertEquals(400, s.get("/balanza?desde=2026-01-01").getStatus());
        assertEquals(400, s.get("/balanza?desde=2026-01-01&hasta=31/01/2026").getStatus());
        verifyNoInteractions(s.balanza);
    }

    @Test
    void laBalanzaDevuelveSusFilasYElCuadre() {
        when(s.balanza.balanza(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).thenReturn(new Balanza(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                List.of(new FilaBalanza(1L, 11, "Caja", NaturalezaCuenta.Deudora, true, new BigDecimal("100.00"),
                        BigDecimal.ZERO, new BigDecimal("100.00"))),
                new BigDecimal("100.00"), new BigDecimal("100.00")));

        Response r = s.get("/balanza?desde=2026-01-01&hasta=2026-01-31");

        assertEquals(200, r.getStatus());
        JsonObject dto = json(r);
        assertTrue(dto.getBoolean("cuadra"));
        assertEquals("Caja", dto.getJsonArray("filas").getJsonObject(0).getString("nombre"));
        assertEquals("2026-01-31", dto.getString("hasta"));
    }

    @Test
    void unRangoInvalidoDelServicioEs422() {
        when(s.balanza.balanza(any(), any())).thenThrow(new ReglaNegocioException("La fecha inicial es posterior"));

        assertEquals(422, s.get("/balanza?desde=2026-02-01&hasta=2026-01-01").getStatus());
    }

    // ------------------------------------------------------------------------------------ planillas

    @Test
    void pagarUnaPlanillaYaPagadaEs422() {
        when(s.planillas.pagar(5L)).thenThrow(new EJBException(new ReglaNegocioException("La planilla 5 ya esta pagada")));

        Response r = s.postVacio("/planillas/5/pagar");

        assertEquals(422, r.getStatus());
        assertEquals("La planilla 5 ya esta pagada", json(r).getString("mensaje"));
    }

    @Test
    void crearUnaPlanillaDevuelve201() {
        Planilla p = conId(new Planilla(), 5L);
        p.setFechaInicio(OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        p.setFechaFin(OffsetDateTime.of(2026, 3, 15, 0, 0, 0, 0, ZoneOffset.UTC));
        p.setFechaPago(OffsetDateTime.of(2026, 3, 16, 0, 0, 0, 0, ZoneOffset.UTC));
        when(s.planillas.crear(any())).thenReturn(p);

        Response r = s.post("/planillas", "{\"fechaInicio\":\"2026-03-01T00:00:00Z\",\"fechaFin\":"
                + "\"2026-03-15T00:00:00Z\",\"fechaPago\":\"2026-03-16T00:00:00Z\"}");

        assertEquals(201, r.getStatus());
        assertTrue(r.getHeaderString("Location").endsWith("/planillas/5"));
        assertEquals("Pendiente", json(r).getString("estado"));
        verify(s.planillas).crear(any());
    }

    @Test
    void generarDetallesUsaPostYNoCreaEnGet() {
        when(s.planillas.generarDetalles(5L)).thenReturn(List.of());

        assertEquals(200, s.postVacio("/planillas/5/detalles/generar").getStatus());
        assertEquals(405, s.get("/planillas/5/detalles/generar").getStatus());
        verify(s.planillas).generarDetalles(5L);
        verify(s.planillas, never()).agregarDetalle(anyLong(), any());
        verify(s.planillas, never()).listar(anyInt(), anyInt());
    }
}
