package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.ClienteDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.VentaDetalleDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaVentaCmd;
import com.sistemacontable.control.service.cmd.VentaCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
class VentaServiceTest {

    @Mock
    ClienteDAO clienteDAO;
    @Mock
    EmpleadoDAO empleadoDAO;
    @Mock
    LoteDAO loteDAO;
    @Mock
    AlmacenDAO almacenDAO;
    @Mock
    VentaDAO ventaDAO;
    @Mock
    VentaDetalleDAO ventaDetalleDAO;
    @Mock
    ExistenciasService existenciasService;
    @Mock
    KardexService kardexService;
    @Mock
    PartidaService partidaService;
    @Mock
    ConfigContable config;
    @InjectMocks
    VentaService servicio;

    private final Cliente cliente = cliente();
    private final Empleado empleado = empleado();
    private final Lote lote10 = Fabrica.lote(10);
    private final Lote lote11 = Fabrica.lote(11);
    private final Almacen almacen20 = Fabrica.almacen(20, true);
    private final Almacen cerrado21 = Fabrica.almacen(21, false);
    private final Cuenta caja = Fabrica.cuenta(1, 1101);
    private final Cuenta clientes = Fabrica.cuenta(2, 1103);
    private final Cuenta inventario = Fabrica.cuenta(3, 1104);
    private final Cuenta ventas = Fabrica.cuenta(4, 4101, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta ivaDebito = Fabrica.cuenta(5, 2102, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta costoVentas = Fabrica.cuenta(6, 5101);
    private final LocalDate fecha = LocalDate.of(2026, 5, 4);

    private static Cliente cliente() {
        Cliente c = new Cliente();
        c.setId(1L);
        return c;
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
        when(clienteDAO.buscarPorId(1L)).thenReturn(Optional.of(cliente));
        when(empleadoDAO.buscarPorId(2L)).thenReturn(Optional.of(empleado));
        when(loteDAO.buscarPorId(10L)).thenReturn(Optional.of(lote10));
        when(loteDAO.buscarPorId(11L)).thenReturn(Optional.of(lote11));
        when(almacenDAO.buscarPorId(20L)).thenReturn(Optional.of(almacen20));
        when(almacenDAO.buscarPorId(21L)).thenReturn(Optional.of(cerrado21));
        when(config.getBigDecimal(ConfigContable.IVA_TASA)).thenReturn(bd("0.13"));
        when(existenciasService.costoPromedio(10L)).thenReturn(Optional.of(bd("4.0000")));
        when(existenciasService.costoPromedio(11L)).thenReturn(Optional.of(bd("20.0000")));
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_CAJA)).thenReturn(caja);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_CLIENTES)).thenReturn(clientes);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO)).thenReturn(inventario);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_VENTAS)).thenReturn(ventas);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_IVA_DEBITO)).thenReturn(ivaDebito);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_COSTO_VENTAS)).thenReturn(costoVentas);
        when(ventaDAO.crear(any())).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            v.setId(7L);
            return v;
        });
        when(kardexService.registrar(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new MovimientoKardex(new Kardex(), List.of()));
        when(partidaService.registrar(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Partida p = new Partida();
            p.setId(9L);
            return p;
        });
    }

    private VentaCmd cmdValido() {
        return cmdConPorcentaje(bd("100"));
    }

    /** 3 x 10 + 2 x 40 = 110.00 ; IVA 14.30 ; total 124.30 ; costo 3 x 4 + 2 x 20 = 52.00. */
    private VentaCmd cmdConPorcentaje(BigDecimal porcentajeContado) {
        return new VentaCmd(1L, 2L, fecha, "Mostrador", porcentajeContado, List.of(
                new LineaVentaCmd(10L, 20L, bd("3"), bd("10.00")),
                new LineaVentaCmd(11L, 20L, bd("2"), bd("40.00"))));
    }

    private void noSeEscribioNada() {
        verify(ventaDAO, never()).crear(any());
        verify(ventaDetalleDAO, never()).crear(any());
        verify(existenciasService, never()).disminuir(any(), any(), any());
        verify(existenciasService, never()).aumentar(any(), any(), any());
        verify(kardexService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<LineaAsiento>> lineasCaptor() {
        return ArgumentCaptor.forClass((Class<List<LineaAsiento>>) (Class<?>) List.class);
    }

    // ---------------------------------------------------------------------------------- caso feliz

    @Test
    void registraVentaDeContadoConStockKardexAlCostoPromedioYPartidaDeIngreso() {
        ResultadoVenta r = servicio.registrar(cmdValido());

        ArgumentCaptor<Venta> venta = ArgumentCaptor.forClass(Venta.class);
        verify(ventaDAO).crear(venta.capture());
        Venta v = venta.getValue();
        assertSame(v, r.venta());
        assertEquals(fecha, v.getFecha());
        assertSame(cliente, v.getCliente());
        assertSame(empleado, v.getEmpleado());

        ArgumentCaptor<VentaDetalle> detalles = ArgumentCaptor.forClass(VentaDetalle.class);
        verify(ventaDetalleDAO, times(2)).crear(detalles.capture());
        VentaDetalle d1 = detalles.getAllValues().get(0);
        assertSame(v, d1.getVenta());
        assertSame(lote10, d1.getLote());
        assertSame(almacen20, d1.getAlmacen());
        assertEquals(bd("3"), d1.getCantidad());
        assertEquals(bd("10.00"), d1.getPrecio());

        verify(existenciasService).disminuir(lote10, almacen20, bd("3"));
        verify(existenciasService).disminuir(lote11, almacen20, bd("2"));

        // un kardex de Salida por detalle: origen = almacen, id_venta, precio_costo = costo promedio del lote
        verify(kardexService).registrar(eq(TipoMovimiento.Salida), same(almacen20), isNull(), isNull(), same(v),
                eq("Venta #7"), eq(List.of(new DetalleKardex(lote10, bd("3"), bd("4.0000")))));
        verify(kardexService).registrar(eq(TipoMovimiento.Salida), same(almacen20), isNull(), isNull(), same(v),
                eq("Venta #7"), eq(List.of(new DetalleKardex(lote11, bd("2"), bd("20.0000")))));

        // ingreso: 3 x 10 + 2 x 40 = 110.00 ; IVA 14.30 ; total 124.30 ; costo: 3 x 4 + 2 x 20 = 52.00
        assertEquals(bd("110.00"), r.subtotal());
        assertEquals(bd("14.30"), r.iva());
        assertEquals(bd("124.30"), r.total());
        assertEquals(bd("52.00"), r.costoVentas());
        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(eq(TipoPartida.Ingreso), eq(fecha), eq("Venta #7"), isNull(), same(v),
                isNull(), lineas.capture());
        assertEquals(List.of(
                LineaAsiento.cargo(caja, bd("124.30")),
                LineaAsiento.abono(ventas, bd("110.00")),
                LineaAsiento.abono(ivaDebito, bd("14.30")),
                LineaAsiento.cargo(costoVentas, bd("52.00")),
                LineaAsiento.abono(inventario, bd("52.00"))), lineas.getValue());
    }

    @Test
    void conCeroPorCientoDeContadoTodoVaAClientesYNoSeExigeLaCuentaDeCaja() {
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_CAJA))
                .thenThrow(new ReglaNegocioException("La cuenta configurada en 'cuenta.caja' no existe"));

        servicio.registrar(cmdConPorcentaje(bd("0")));

        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(any(), any(), any(), any(), any(), any(), lineas.capture());
        verify(partidaService, never()).cuentaConfigurada(ConfigContable.CUENTA_CAJA);
        assertEquals(LineaAsiento.cargo(clientes, bd("124.30")), lineas.getValue().get(0));
        assertTrue(lineas.getValue().stream().noneMatch(l -> l.cuenta() == caja));
        assertSinLineasEnCero(lineas.getValue());
    }

    @Test
    void conCienPorCientoDeContadoTodoVaACajaYNoSeExigeLaCuentaDeClientes() {
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_CLIENTES))
                .thenThrow(new ReglaNegocioException("La cuenta configurada en 'cuenta.clientes' no existe"));

        servicio.registrar(cmdConPorcentaje(bd("100")));

        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(any(), any(), any(), any(), any(), any(), lineas.capture());
        verify(partidaService, never()).cuentaConfigurada(ConfigContable.CUENTA_CLIENTES);
        assertEquals(LineaAsiento.cargo(caja, bd("124.30")), lineas.getValue().get(0));
        assertTrue(lineas.getValue().stream().noneMatch(l -> l.cuenta() == clientes));
        assertSinLineasEnCero(lineas.getValue());
    }

    @Test
    void conCuarentaPorCientoDeContadoElCobroSeReparteEntreCajaYClientes() {
        ResultadoVenta r = servicio.registrar(cmdConPorcentaje(bd("40")));

        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(any(), any(), any(), any(), any(), any(), lineas.capture());
        // total 124.30 -> contado 49.72 ; credito 74.58 ; el resto de la partida no cambia
        assertEquals(List.of(
                LineaAsiento.cargo(caja, bd("49.72")),
                LineaAsiento.cargo(clientes, bd("74.58")),
                LineaAsiento.abono(ventas, bd("110.00")),
                LineaAsiento.abono(ivaDebito, bd("14.30")),
                LineaAsiento.cargo(costoVentas, bd("52.00")),
                LineaAsiento.abono(inventario, bd("52.00"))), lineas.getValue());
        assertEquals(bd("124.30"), r.total());
        ReglasPartida.validar(lineas.getValue());
    }

    @Test
    void unPorcentajeQueNoDivideExactoElTotalCuadraAlCentavo() {
        servicio.registrar(cmdConPorcentaje(bd("33.33")));

        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(any(), any(), any(), any(), any(), any(), lineas.capture());
        // 124.30 x 0.3333 = 41.4292 -> 41.43 ; credito = 124.30 - 41.43 = 82.87
        assertEquals(LineaAsiento.cargo(caja, bd("41.43")), lineas.getValue().get(0));
        assertEquals(LineaAsiento.cargo(clientes, bd("82.87")), lineas.getValue().get(1));
        BigDecimal cargos = lineas.getValue().stream().map(LineaAsiento::cargo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonos = lineas.getValue().stream().map(LineaAsiento::abono).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, cargos.compareTo(abonos));
        assertSinLineasEnCero(lineas.getValue());
    }

    private static void assertSinLineasEnCero(List<LineaAsiento> lineas) {
        assertTrue(lineas.stream().allMatch(l -> l.cargo().signum() > 0 || l.abono().signum() > 0));
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
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(cmdConPorcentaje(bd("100.01"))));

        org.mockito.Mockito.verifyNoInteractions(clienteDAO, empleadoDAO, loteDAO, almacenDAO, existenciasService);
    }

    @Test
    void losExtremosDelRangoSeAceptan() {
        servicio.registrar(cmdConPorcentaje(bd("0")));
        servicio.registrar(cmdConPorcentaje(bd("0.00")));
        servicio.registrar(cmdConPorcentaje(bd("100")));
        servicio.registrar(cmdConPorcentaje(bd("100.00")));

        verify(ventaDAO, times(4)).crear(any());
    }

    @Test
    void elStockSeVerificaConTodasLasLineasAcumuladasAntesDeEscribirNada() {
        VentaCmd cmd = new VentaCmd(1L, 2L, fecha, null, bd("100"), List.of(
                new LineaVentaCmd(10L, 20L, bd("3"), bd("10")),
                new LineaVentaCmd(10L, 20L, bd("4"), bd("10")),
                new LineaVentaCmd(11L, 20L, bd("1"), bd("10"))));

        servicio.registrar(cmd);

        ArgumentCaptor<Map<ClaveStock, BigDecimal>> requeridos = mapaCaptor();
        InOrder orden = inOrder(existenciasService, ventaDAO);
        orden.verify(existenciasService).verificarDisponible(requeridos.capture());
        orden.verify(ventaDAO).crear(any());
        orden.verify(existenciasService, times(3)).disminuir(any(), any(), any());
        Map<ClaveStock, BigDecimal> mapa = requeridos.getValue();
        assertEquals(2, mapa.size());
        assertEquals(0, bd("7").compareTo(mapa.get(new ClaveStock(10L, 20L))));
        assertEquals(0, bd("1").compareTo(mapa.get(new ClaveStock(11L, 20L))));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<ClaveStock, BigDecimal>> mapaCaptor() {
        return ArgumentCaptor.forClass((Class<Map<ClaveStock, BigDecimal>>) (Class<?>) Map.class);
    }

    @Test
    void conStockInsuficienteEnUnaLineaNoSeEscribeNadaDeNinguna() {
        doThrow(new ReglaNegocioException("Stock insuficiente del lote 11 en el almacen 20"))
                .when(existenciasService).verificarDisponible(any());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(cmdValido()));

        assertTrue(ex.getMessage().contains("Stock insuficiente"), ex.getMessage());
        noSeEscribioNada();
    }

    @Test
    void unaVentaDeImporteYCostoCeroNoGeneraPartida() {
        when(existenciasService.costoPromedio(10L)).thenReturn(Optional.of(bd("0.0000")));
        VentaCmd cmd = new VentaCmd(1L, 2L, fecha, null, bd("100"), List.of(new LineaVentaCmd(10L, 20L, bd("3"), BigDecimal.ZERO)));

        ResultadoVenta r = servicio.registrar(cmd);

        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
        assertNull(r.partida());
    }

    // ---------------------------------------------------------------------------- validaciones

    private void rechaza(VentaCmd cmd, Class<? extends RuntimeException> tipo) {
        assertThrows(tipo, () -> servicio.registrar(cmd));
        noSeEscribioNada();
    }

    @Test
    void clienteOEmpleadoInexistentesNoEscribenNada() {
        when(clienteDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        when(empleadoDAO.buscarPorId(98L)).thenReturn(Optional.empty());

        rechaza(new VentaCmd(99L, 2L, fecha, null, bd("100"), cmdValido().lineas()), NoEncontradoException.class);
        rechaza(new VentaCmd(1L, 98L, fecha, null, bd("100"), cmdValido().lineas()), NoEncontradoException.class);
    }

    @Test
    void datosObligatoriosAusentesNoEscribenNada() {
        rechaza(null, ReglaNegocioException.class);
        rechaza(new VentaCmd(null, 2L, fecha, null, bd("100"), cmdValido().lineas()), ReglaNegocioException.class);
        rechaza(new VentaCmd(1L, null, fecha, null, bd("100"), cmdValido().lineas()), ReglaNegocioException.class);
        rechaza(new VentaCmd(1L, 2L, fecha, null, bd("100"), List.of()), ReglaNegocioException.class);
    }

    @Test
    void unaLineaInvalidaDespuesDeUnaValidaNoDejaNadaEscrito() {
        for (LineaVentaCmd mala : List.of(
                new LineaVentaCmd(11L, 20L, BigDecimal.ZERO, bd("1")),
                new LineaVentaCmd(11L, 20L, bd("-2"), bd("1")),
                new LineaVentaCmd(11L, 20L, null, bd("1")),
                new LineaVentaCmd(11L, 20L, bd("1"), bd("-0.01")),
                new LineaVentaCmd(11L, 20L, bd("1"), null),
                new LineaVentaCmd(null, 20L, bd("1"), bd("1")),
                new LineaVentaCmd(11L, null, bd("1"), bd("1")))) {
            rechaza(new VentaCmd(1L, 2L, fecha, null,
                    bd("100"),
                    List.of(new LineaVentaCmd(10L, 20L, bd("1"), bd("1")), mala)), ReglaNegocioException.class);
        }
    }

    @Test
    void almacenDeshabilitadoYLoteOAlmacenInexistentesSeRechazan() {
        when(loteDAO.buscarPorId(99L)).thenReturn(Optional.empty());

        rechaza(new VentaCmd(1L, 2L, fecha, null, bd("100"), List.of(new LineaVentaCmd(10L, 21L, bd("1"), bd("1")))),
                ReglaNegocioException.class);
        rechaza(new VentaCmd(1L, 2L, fecha, null, bd("100"), List.of(new LineaVentaCmd(99L, 20L, bd("1"), bd("1")))),
                NoEncontradoException.class);
    }

    @Test
    void unLoteSinComprasNoTieneCostoYSeRechazaSinEscribir() {
        when(existenciasService.costoPromedio(11L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(cmdValido()));

        assertTrue(ex.getMessage().contains("costo") && ex.getMessage().contains("11"), ex.getMessage());
        noSeEscribioNada();
    }

    @Test
    void siUnaCuentaConfiguradaFallaNoSeEscribeNada() {
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_COSTO_VENTAS))
                .thenThrow(new ReglaNegocioException("La cuenta configurada en 'cuenta.costo.ventas' no existe"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(cmdValido()));

        assertTrue(ex.getMessage().contains("cuenta.costo.ventas"), ex.getMessage());
        noSeEscribioNada();
    }
}
