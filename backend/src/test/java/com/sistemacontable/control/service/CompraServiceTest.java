package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.ProveedorDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.CompraCmd;
import com.sistemacontable.control.service.cmd.LineaCompraCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompraServiceTest {

    @Mock
    ProveedorDAO proveedorDAO;
    @Mock
    EmpleadoDAO empleadoDAO;
    @Mock
    LoteDAO loteDAO;
    @Mock
    AlmacenDAO almacenDAO;
    @Mock
    CompraDAO compraDAO;
    @Mock
    CompraDetalleDAO compraDetalleDAO;
    @Mock
    ExistenciasService existenciasService;
    @Mock
    KardexService kardexService;
    @Mock
    PartidaService partidaService;
    @Mock
    ConfigContable config;
    @InjectMocks
    CompraService servicio;

    private final Proveedor proveedor = proveedor();
    private final Empleado empleado = empleado();
    private final Lote lote10 = Fabrica.lote(10);
    private final Lote lote11 = Fabrica.lote(11);
    private final Almacen almacen20 = Fabrica.almacen(20, true);
    private final Almacen cerrado21 = Fabrica.almacen(21, false);
    private final Cuenta inventario = Fabrica.cuenta(1, 1104);
    private final Cuenta ivaCredito = Fabrica.cuenta(2, 1105);
    private final Cuenta proveedores = Fabrica.cuenta(3, 2101, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta caja = Fabrica.cuenta(4, 1101);
    private final LocalDate fecha = LocalDate.of(2026, 5, 4);

    private static Proveedor proveedor() {
        Proveedor p = new Proveedor();
        p.setId(1L);
        return p;
    }

    private static Empleado empleado() {
        Empleado e = new Empleado();
        e.setId(2L);
        return e;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        when(proveedorDAO.buscarPorId(1L)).thenReturn(Optional.of(proveedor));
        when(empleadoDAO.buscarPorId(2L)).thenReturn(Optional.of(empleado));
        when(loteDAO.buscarPorId(10L)).thenReturn(Optional.of(lote10));
        when(loteDAO.buscarPorId(11L)).thenReturn(Optional.of(lote11));
        when(almacenDAO.buscarPorId(20L)).thenReturn(Optional.of(almacen20));
        when(almacenDAO.buscarPorId(21L)).thenReturn(Optional.of(cerrado21));
        when(config.getBigDecimal(ConfigContable.IVA_TASA)).thenReturn(bd("0.13"));
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO)).thenReturn(inventario);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_IVA_CREDITO)).thenReturn(ivaCredito);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_PROVEEDORES)).thenReturn(proveedores);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_CAJA)).thenReturn(caja);
        when(compraDAO.crear(any())).thenAnswer(inv -> {
            Compra c = inv.getArgument(0);
            c.setId(5L);
            return c;
        });
        when(kardexService.registrar(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new MovimientoKardex(new Kardex(), List.of()));
        when(partidaService.registrar(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Partida p = new Partida();
            p.setId(9L);
            return p;
        });
    }

    private CompraCmd cmdValido() {
        return cmdConPorcentaje(bd("0"));
    }

    /** 10 x 5.00 + 4 x 25.50 = 152.00 ; IVA 13 % = 19.76 ; total 171.76. */
    private CompraCmd cmdConPorcentaje(BigDecimal porcentajeContado) {
        return new CompraCmd(1L, 2L, fecha, "Reposicion", porcentajeContado, List.of(
                new LineaCompraCmd(10L, 20L, bd("10"), bd("5.00")),
                new LineaCompraCmd(11L, 20L, bd("4"), bd("25.50"))));
    }

    private List<LineaAsiento> partidaGenerada() {
        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(any(), any(), any(), any(), any(), any(), lineas.capture());
        return lineas.getValue();
    }

    private static void assertSinLineasEnCero(List<LineaAsiento> lineas) {
        assertTrue(lineas.stream().allMatch(l -> l.cargo().signum() > 0 || l.abono().signum() > 0));
    }

    private void noSeEscribioNada() {
        verify(compraDAO, never()).crear(any());
        verify(compraDetalleDAO, never()).crear(any());
        verifyNoInteractions(existenciasService);
        verifyNoInteractions(kardexService);
        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    private void rechaza(CompraCmd cmd, Class<? extends RuntimeException> tipo) {
        assertThrows(tipo, () -> servicio.registrar(cmd));
        noSeEscribioNada();
    }

    // ---------------------------------------------------------------------------------- caso feliz

    @Test
    void registraCompraConStockKardexYPartidaDeDiario() {
        ResultadoCompra r = servicio.registrar(cmdValido());

        ArgumentCaptor<Compra> compra = ArgumentCaptor.forClass(Compra.class);
        verify(compraDAO).crear(compra.capture());
        Compra c = compra.getValue();
        assertSame(c, r.compra());
        assertEquals(fecha, c.getFecha());
        assertSame(proveedor, c.getProveedor());
        assertSame(empleado, c.getEmpleado());
        assertEquals("Reposicion", c.getDescripcion());

        ArgumentCaptor<CompraDetalle> detalles = ArgumentCaptor.forClass(CompraDetalle.class);
        verify(compraDetalleDAO, times(2)).crear(detalles.capture());
        CompraDetalle d1 = detalles.getAllValues().get(0);
        assertSame(c, d1.getCompra());
        assertSame(lote10, d1.getLote());
        assertSame(almacen20, d1.getAlmacen());
        assertEquals(bd("10"), d1.getCantidad());
        assertEquals(bd("5.00"), d1.getPrecio());
        assertEquals(2, r.detalles().size());

        verify(existenciasService).aumentar(lote10, almacen20, bd("10"));
        verify(existenciasService).aumentar(lote11, almacen20, bd("4"));

        // un kardex de Entrada por detalle: destino = almacen, id_compra, precio_costo = precio de compra
        verify(kardexService).registrar(eq(TipoMovimiento.Entrada), isNull(), same(almacen20), same(c), isNull(),
                eq("Compra #5"), eq(List.of(new DetalleKardex(lote10, bd("10"), bd("5.00")))));
        verify(kardexService).registrar(eq(TipoMovimiento.Entrada), isNull(), same(almacen20), same(c), isNull(),
                eq("Compra #5"), eq(List.of(new DetalleKardex(lote11, bd("4"), bd("25.50")))));
        assertEquals(2, r.movimientos().size());

        // 10 x 5.00 + 4 x 25.50 = 152.00 ; IVA 13 % = 19.76 ; total 171.76
        assertEquals(bd("152.00"), r.subtotal());
        assertEquals(bd("19.76"), r.iva());
        assertEquals(bd("171.76"), r.total());
        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(eq(TipoPartida.Diario), eq(fecha), eq("Compra #5"), same(c), isNull(),
                isNull(), lineas.capture());
        assertEquals(List.of(
                LineaAsiento.cargo(inventario, bd("152.00")),
                LineaAsiento.cargo(ivaCredito, bd("19.76")),
                LineaAsiento.abono(proveedores, bd("171.76"))), lineas.getValue());
        assertNotNull(r.partida());
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<LineaAsiento>> lineasCaptor() {
        return ArgumentCaptor.forClass((Class<List<LineaAsiento>>) (Class<?>) List.class);
    }

    @Test
    void conCeroPorCientoDeContadoTodoVaAProveedoresYNoSeExigeLaCuentaDeCaja() {
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_CAJA))
                .thenThrow(new ReglaNegocioException("La cuenta configurada en 'cuenta.caja' no existe"));

        servicio.registrar(cmdConPorcentaje(bd("0")));

        List<LineaAsiento> lineas = partidaGenerada();
        verify(partidaService, never()).cuentaConfigurada(ConfigContable.CUENTA_CAJA);
        assertEquals(LineaAsiento.abono(proveedores, bd("171.76")), lineas.get(2));
        assertTrue(lineas.stream().noneMatch(l -> l.cuenta() == caja));
        assertSinLineasEnCero(lineas);
    }

    @Test
    void conCienPorCientoDeContadoTodoSalePorCajaYNoSeExigeLaCuentaDeProveedores() {
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_PROVEEDORES))
                .thenThrow(new ReglaNegocioException("La cuenta configurada en 'cuenta.proveedores' no existe"));

        servicio.registrar(cmdConPorcentaje(bd("100")));

        List<LineaAsiento> lineas = partidaGenerada();
        verify(partidaService, never()).cuentaConfigurada(ConfigContable.CUENTA_PROVEEDORES);
        assertEquals(List.of(
                LineaAsiento.cargo(inventario, bd("152.00")),
                LineaAsiento.cargo(ivaCredito, bd("19.76")),
                LineaAsiento.abono(caja, bd("171.76"))), lineas);
        assertSinLineasEnCero(lineas);
    }

    @Test
    void conCuarentaPorCientoDeContadoElPagoSeReparteEntreCajaYProveedores() {
        ResultadoCompra r = servicio.registrar(cmdConPorcentaje(bd("40")));

        // total 171.76 -> contado 68.70 ; credito 103.06
        assertEquals(List.of(
                LineaAsiento.cargo(inventario, bd("152.00")),
                LineaAsiento.cargo(ivaCredito, bd("19.76")),
                LineaAsiento.abono(caja, bd("68.70")),
                LineaAsiento.abono(proveedores, bd("103.06"))), partidaGenerada());
        assertEquals(bd("171.76"), r.total());
    }

    @Test
    void unPorcentajeQueNoDivideExactoElTotalCuadraAlCentavo() {
        servicio.registrar(cmdConPorcentaje(bd("33.33")));

        List<LineaAsiento> lineas = partidaGenerada();
        // 171.76 x 0.3333 = 57.2476 -> 57.25 ; credito = 171.76 - 57.25 = 114.51
        assertEquals(LineaAsiento.abono(caja, bd("57.25")), lineas.get(2));
        assertEquals(LineaAsiento.abono(proveedores, bd("114.51")), lineas.get(3));
        BigDecimal cargos = lineas.stream().map(LineaAsiento::cargo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonos = lineas.stream().map(LineaAsiento::abono).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, cargos.compareTo(abonos));
        assertSinLineasEnCero(lineas);
    }

    @Test
    void unPorcentajeNuloOFueraDeRangoSeRechazaAntesDeEscribirNada() {
        for (BigDecimal malo : new BigDecimal[] {null, bd("-1"), bd("-0.01"), bd("100.01"), bd("150")}) {
            ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                    () -> servicio.registrar(cmdConPorcentaje(malo)));
            assertTrue(ex.getMessage().contains("porcentaje de contado"), ex.getMessage());
            noSeEscribioNada();
        }
    }

    @Test
    void laValidacionDelPorcentajeOcurreAntesDeBuscarCualquierEntidad() {
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(cmdConPorcentaje(bd("-1"))));

        verifyNoInteractions(proveedorDAO, empleadoDAO, loteDAO, almacenDAO);
    }

    @Test
    void losExtremosDelRangoSeAceptan() {
        servicio.registrar(cmdConPorcentaje(bd("0")));
        servicio.registrar(cmdConPorcentaje(bd("0.00")));
        servicio.registrar(cmdConPorcentaje(bd("100")));
        servicio.registrar(cmdConPorcentaje(bd("100.00")));

        verify(compraDAO, times(4)).crear(any());
    }

    @Test
    void unaCompraGratisNoExigeNingunaCuentaDePago() {
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_CAJA))
                .thenThrow(new ReglaNegocioException("sin caja"));
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_PROVEEDORES))
                .thenThrow(new ReglaNegocioException("sin proveedores"));
        CompraCmd cmd = new CompraCmd(1L, 2L, fecha, "Donacion", bd("40"), List.of(
                new LineaCompraCmd(10L, 20L, bd("3"), BigDecimal.ZERO)));

        ResultadoCompra r = servicio.registrar(cmd);

        assertNull(r.partida());
    }

    @Test
    void elOrdenDeEscrituraEsCompraStockKardexYPartida() {
        servicio.registrar(cmdValido());

        InOrder orden = inOrder(compraDAO, compraDetalleDAO, existenciasService, kardexService, partidaService);
        orden.verify(compraDAO).crear(any());
        orden.verify(compraDetalleDAO).crear(any());
        orden.verify(existenciasService).aumentar(any(), any(), any());
        orden.verify(kardexService).registrar(any(), any(), any(), any(), any(), any(), any());
        orden.verify(partidaService).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sinFechaUsaLaFechaDeHoy() {
        CompraCmd cmd = new CompraCmd(1L, 2L, null, null, bd("0"), cmdValido().lineas());

        ResultadoCompra r = servicio.registrar(cmd);

        assertEquals(LocalDate.now(), r.compra().getFecha());
    }

    @Test
    void unaCompraDeImporteCeroActualizaStockYKardexPeroNoGeneraPartida() {
        CompraCmd cmd = new CompraCmd(1L, 2L, fecha, "Donacion", bd("0"), List.of(
                new LineaCompraCmd(10L, 20L, bd("3"), BigDecimal.ZERO)));

        ResultadoCompra r = servicio.registrar(cmd);

        verify(existenciasService).aumentar(lote10, almacen20, bd("3"));
        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
        assertNull(r.partida());
        assertEquals(bd("0.00"), r.total());
    }

    // ---------------------------------------------------------------------------- validaciones

    @Test
    void proveedorOEmpleadoInexistentesNoEscribenNada() {
        when(proveedorDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        when(empleadoDAO.buscarPorId(98L)).thenReturn(Optional.empty());

        rechaza(new CompraCmd(99L, 2L, fecha, null, bd("0"), cmdValido().lineas()), NoEncontradoException.class);
        rechaza(new CompraCmd(1L, 98L, fecha, null, bd("0"), cmdValido().lineas()), NoEncontradoException.class);
    }

    @Test
    void datosObligatoriosAusentesNoEscribenNada() {
        rechaza(null, ReglaNegocioException.class);
        rechaza(new CompraCmd(null, 2L, fecha, null, bd("0"), cmdValido().lineas()), ReglaNegocioException.class);
        rechaza(new CompraCmd(1L, null, fecha, null, bd("0"), cmdValido().lineas()), ReglaNegocioException.class);
        rechaza(new CompraCmd(1L, 2L, fecha, null, bd("0"), List.of()), ReglaNegocioException.class);
        rechaza(new CompraCmd(1L, 2L, fecha, null, bd("0"), null), ReglaNegocioException.class);
    }

    @Test
    void unaLineaInvalidaDespuesDeUnaValidaNoDejaNadaEscrito() {
        for (LineaCompraCmd mala : List.of(
                new LineaCompraCmd(11L, 20L, BigDecimal.ZERO, bd("1")),
                new LineaCompraCmd(11L, 20L, bd("-2"), bd("1")),
                new LineaCompraCmd(11L, 20L, null, bd("1")),
                new LineaCompraCmd(11L, 20L, bd("1"), bd("-0.01")),
                new LineaCompraCmd(11L, 20L, bd("1"), null),
                new LineaCompraCmd(null, 20L, bd("1"), bd("1")),
                new LineaCompraCmd(11L, null, bd("1"), bd("1")))) {
            CompraCmd cmd = new CompraCmd(1L, 2L, fecha, null,
                    bd("0"),
                    List.of(new LineaCompraCmd(10L, 20L, bd("1"), bd("1")), mala));
            rechaza(cmd, ReglaNegocioException.class);
        }
    }

    @Test
    void loteOAlmacenInexistentesFallanConNoEncontradoSinEscribir() {
        when(loteDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        when(almacenDAO.buscarPorId(98L)).thenReturn(Optional.empty());

        rechaza(new CompraCmd(1L, 2L, fecha, null, bd("0"), List.of(new LineaCompraCmd(99L, 20L, bd("1"), bd("1")))),
                NoEncontradoException.class);
        rechaza(new CompraCmd(1L, 2L, fecha, null, bd("0"), List.of(new LineaCompraCmd(10L, 98L, bd("1"), bd("1")))),
                NoEncontradoException.class);
    }

    @Test
    void unAlmacenDeshabilitadoSeRechazaSinEscribir() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(
                new CompraCmd(1L, 2L, fecha, null, bd("0"), List.of(new LineaCompraCmd(10L, 21L, bd("1"), bd("1"))))));

        assertTrue(ex.getMessage().contains("deshabilitado"), ex.getMessage());
        noSeEscribioNada();
    }

    @Test
    void siUnaCuentaConfiguradaFallaNoSeEscribeNadaPorqueSeResuelveAntes() {
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_PROVEEDORES))
                .thenThrow(new ReglaNegocioException("La cuenta configurada en 'cuenta.proveedores' no existe"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(cmdValido()));

        assertTrue(ex.getMessage().contains("cuenta.proveedores"), ex.getMessage());
        noSeEscribioNada();
    }
}
