package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.CuentaDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.TerceroDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaPartidaCmd;
import com.sistemacontable.control.service.cmd.PartidaCmd;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Tercero;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoPartida;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PartidaServiceTest {

    @Mock
    PartidaDAO partidaDAO;
    @Mock
    PartidaDetalleDAO detalleDAO;
    @Mock
    CuentaDAO cuentaDAO;
    @Mock
    CompraDAO compraDAO;
    @Mock
    VentaDAO ventaDAO;
    @Mock
    TerceroDAO terceroDAO;
    @Mock
    ConfigContable config;
    @InjectMocks
    PartidaService servicio;

    private final Cuenta caja = Fabrica.cuenta(1, 1101);
    private final Cuenta ventas = Fabrica.cuenta(2, 4101, NaturalezaCuenta.Acreedora, true, true);
    private final LocalDate fecha = LocalDate.of(2026, 3, 15);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        when(cuentaDAO.buscarPorId(1L)).thenReturn(Optional.of(caja));
        when(cuentaDAO.buscarPorId(2L)).thenReturn(Optional.of(ventas));
    }

    private PartidaCmd cmdCuadrado() {
        return new PartidaCmd(fecha, "Venta de contado", TipoPartida.Ingreso, null, null, null,
                List.of(new LineaPartidaCmd(1L, bd("100.00"), null), new LineaPartidaCmd(2L, null, bd("100.00"))));
    }

    private void noSeEscribioNada() {
        verify(partidaDAO, never()).crear(any());
        verify(detalleDAO, never()).crear(any());
    }

    // ------------------------------------------------------------------------------------------- crear

    @Test
    void crearPersistePartidaYSusLineas() {
        Partida resultado = servicio.crear(cmdCuadrado());

        ArgumentCaptor<Partida> partida = ArgumentCaptor.forClass(Partida.class);
        verify(partidaDAO).crear(partida.capture());
        assertSame(resultado, partida.getValue());
        assertEquals(fecha, resultado.getFecha());
        assertEquals("Venta de contado", resultado.getConcepto());
        assertEquals(TipoPartida.Ingreso, resultado.getTipo());

        ArgumentCaptor<PartidaDetalle> lineas = ArgumentCaptor.forClass(PartidaDetalle.class);
        verify(detalleDAO, times(2)).crear(lineas.capture());
        PartidaDetalle l1 = lineas.getAllValues().get(0);
        PartidaDetalle l2 = lineas.getAllValues().get(1);
        assertSame(resultado, l1.getPartida());
        assertSame(caja, l1.getCuenta());
        assertEquals(0, bd("100.00").compareTo(l1.getCargo()));
        assertEquals(0, BigDecimal.ZERO.compareTo(l1.getAbono()));
        assertSame(ventas, l2.getCuenta());
        assertEquals(0, bd("100.00").compareTo(l2.getAbono()));
        assertEquals(0, BigDecimal.ZERO.compareTo(l2.getCargo()));
        verify(partidaDAO).flush();
    }

    @Test
    void crearRedondeaLosImportesADosDecimalesAntesDeValidar() {
        PartidaCmd cmd = new PartidaCmd(fecha, "Redondeo", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(1L, bd("10.005"), null), new LineaPartidaCmd(2L, null, bd("10.005"))));

        servicio.crear(cmd);

        ArgumentCaptor<PartidaDetalle> lineas = ArgumentCaptor.forClass(PartidaDetalle.class);
        verify(detalleDAO, times(2)).crear(lineas.capture());
        assertEquals(bd("10.01"), lineas.getAllValues().get(0).getCargo());
        assertEquals(bd("10.01"), lineas.getAllValues().get(1).getAbono());
    }

    @Test
    void crearResuelveCompraYTercero() {
        Compra compra = new Compra();
        compra.setId(9L);
        Tercero tercero = new Tercero();
        tercero.setId(4L);
        when(compraDAO.buscarPorId(9L)).thenReturn(Optional.of(compra));
        when(terceroDAO.buscarPorId(4L)).thenReturn(Optional.of(tercero));
        PartidaCmd cmd = new PartidaCmd(fecha, "Compra", TipoPartida.Diario, 9L, null, 4L, cmdCuadrado().lineas());

        Partida p = servicio.crear(cmd);

        assertSame(compra, p.getCompra());
        assertSame(tercero, p.getTercero());
    }

    @Test
    void crearRechazaCompraYVentaAlMismoTiempo() {
        PartidaCmd cmd = new PartidaCmd(fecha, "Doble", TipoPartida.Diario, 9L, 8L, null, cmdCuadrado().lineas());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.crear(cmd));

        assertTrue(ex.getMessage().contains("compra") && ex.getMessage().contains("venta"), ex.getMessage());
        noSeEscribioNada();
    }

    @Test
    void crearRechazaCamposObligatoriosVacios() {
        List<LineaPartidaCmd> l = cmdCuadrado().lineas();
        for (PartidaCmd cmd : List.of(
                new PartidaCmd(null, "x", TipoPartida.Diario, null, null, null, l),
                new PartidaCmd(fecha, "  ", TipoPartida.Diario, null, null, null, l),
                new PartidaCmd(fecha, null, TipoPartida.Diario, null, null, null, l),
                new PartidaCmd(fecha, "x", null, null, null, null, l))) {
            assertThrows(ReglaNegocioException.class, () -> servicio.crear(cmd));
        }
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(null));
        noSeEscribioNada();
    }

    @Test
    void crearRechazaPartidaDescuadradaSinEscribirNada() {
        PartidaCmd cmd = new PartidaCmd(fecha, "Mal", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(1L, bd("100"), null), new LineaPartidaCmd(2L, null, bd("90"))));

        assertThrows(ReglaNegocioException.class, () -> servicio.crear(cmd));

        noSeEscribioNada();
    }

    @Test
    void crearRechazaMenosDeDosLineas() {
        PartidaCmd cmd = new PartidaCmd(fecha, "Mal", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(1L, bd("100"), null)));

        assertThrows(ReglaNegocioException.class, () -> servicio.crear(cmd));

        noSeEscribioNada();
    }

    @Test
    void crearRechazaCuentaInactivaONoImputable() {
        Cuenta inactiva = Fabrica.cuenta(3, 1190, NaturalezaCuenta.Deudora, false, true);
        Cuenta grupo = Fabrica.cuenta(4, 1100, NaturalezaCuenta.Deudora, true, false);
        when(cuentaDAO.buscarPorId(3L)).thenReturn(Optional.of(inactiva));
        when(cuentaDAO.buscarPorId(4L)).thenReturn(Optional.of(grupo));

        for (long id : new long[] {3L, 4L}) {
            PartidaCmd cmd = new PartidaCmd(fecha, "x", TipoPartida.Diario, null, null, null,
                    List.of(new LineaPartidaCmd(id, bd("5"), null), new LineaPartidaCmd(2L, null, bd("5"))));
            assertThrows(ReglaNegocioException.class, () -> servicio.crear(cmd));
        }
        noSeEscribioNada();
    }

    @Test
    void crearFallaConNoEncontradoSiLaCuentaOLaCompraNoExisten() {
        when(cuentaDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        PartidaCmd sinCuenta = new PartidaCmd(fecha, "x", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(99L, bd("5"), null), new LineaPartidaCmd(2L, null, bd("5"))));
        assertThrows(NoEncontradoException.class, () -> servicio.crear(sinCuenta));

        when(compraDAO.buscarPorId(77L)).thenReturn(Optional.empty());
        PartidaCmd sinCompra = new PartidaCmd(fecha, "x", TipoPartida.Diario, 77L, null, null, cmdCuadrado().lineas());
        assertThrows(NoEncontradoException.class, () -> servicio.crear(sinCompra));
        noSeEscribioNada();
    }

    @Test
    void registrarAceptaEntidadesYValidaIgual() {
        Venta venta = new Venta();
        venta.setId(3L);

        Partida p = servicio.registrar(TipoPartida.Ingreso, fecha, "Venta #3", null, venta, null,
                List.of(LineaAsiento.cargo(caja, bd("50")), LineaAsiento.abono(ventas, bd("50"))));

        assertSame(venta, p.getVenta());
        verify(detalleDAO, times(2)).crear(any());

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(TipoPartida.Ingreso, fecha, "Mal", null,
                venta, null, List.of(LineaAsiento.cargo(caja, bd("50")), LineaAsiento.abono(ventas, bd("49")))));
    }

    // ------------------------------------------------------------------------- cuentas configuradas

    @Test
    void cuentaConfiguradaBuscaPorElCodigoDeLaConfiguracion() {
        when(config.getInt("cuenta.caja")).thenReturn(1101);
        when(cuentaDAO.findByCodigo(1101)).thenReturn(Optional.of(caja));

        assertSame(caja, servicio.cuentaConfigurada(ConfigContable.CUENTA_CAJA));
    }

    @Test
    void cuentaConfiguradaInexistenteNombraLaClave() {
        when(config.getInt("cuenta.caja")).thenReturn(1101);
        when(cuentaDAO.findByCodigo(1101)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.cuentaConfigurada(ConfigContable.CUENTA_CAJA));

        assertTrue(ex.getMessage().contains("cuenta.caja") && ex.getMessage().contains("1101"), ex.getMessage());
    }

    @Test
    void cuentaConfiguradaInactivaONoImputableNombraLaClave() {
        Cuenta inactiva = Fabrica.cuenta(5, 1102, NaturalezaCuenta.Deudora, false, true);
        Cuenta grupo = Fabrica.cuenta(6, 1103, NaturalezaCuenta.Deudora, true, false);
        when(config.getInt("cuenta.bancos")).thenReturn(1102);
        when(config.getInt("cuenta.clientes")).thenReturn(1103);
        when(cuentaDAO.findByCodigo(1102)).thenReturn(Optional.of(inactiva));
        when(cuentaDAO.findByCodigo(1103)).thenReturn(Optional.of(grupo));

        ReglaNegocioException a = assertThrows(ReglaNegocioException.class,
                () -> servicio.cuentaConfigurada(ConfigContable.CUENTA_BANCOS));
        ReglaNegocioException b = assertThrows(ReglaNegocioException.class,
                () -> servicio.cuentaConfigurada(ConfigContable.CUENTA_CLIENTES));

        assertTrue(a.getMessage().contains("cuenta.bancos"), a.getMessage());
        assertTrue(b.getMessage().contains("cuenta.clientes"), b.getMessage());
    }

    @Test
    void cuentaConfiguradaConConfiguracionAusentePropagaElErrorDeConfiguracion() {
        when(config.getInt("cuenta.caja")).thenThrow(new IllegalStateException("Falta la configuracion cuenta.caja"));

        assertThrows(IllegalStateException.class, () -> servicio.cuentaConfigurada(ConfigContable.CUENTA_CAJA));
    }

    // ---------------------------------------------------------------------------------- consultas

    @Test
    void buscarPorIdDevuelveLaPartidaOFallaConNoEncontrado() {
        Partida p = new Partida();
        p.setId(1L);
        when(partidaDAO.buscarPorId(1L)).thenReturn(Optional.of(p));
        when(partidaDAO.buscarPorId(2L)).thenReturn(Optional.empty());

        assertSame(p, servicio.buscarPorId(1L));
        assertThrows(NoEncontradoException.class, () -> servicio.buscarPorId(2L));
    }

    @Test
    void lineasDevuelveLasLineasDeUnaPartidaExistente() {
        Partida p = new Partida();
        p.setId(1L);
        PartidaDetalle d = new PartidaDetalle();
        when(partidaDAO.buscarPorId(1L)).thenReturn(Optional.of(p));
        when(detalleDAO.buscarPorPartida(1L)).thenReturn(List.of(d));

        assertEquals(List.of(d), servicio.lineas(1L));

        when(partidaDAO.buscarPorId(2L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.lineas(2L));
    }

    @Test
    void buscarPorRangoValidaElOrdenDeLasFechas() {
        Partida p = new Partida();
        when(partidaDAO.findByRango(fecha, fecha.plusDays(1))).thenReturn(List.of(p));

        assertEquals(List.of(p), servicio.buscarPorRango(fecha, fecha.plusDays(1)));
        assertThrows(ReglaNegocioException.class, () -> servicio.buscarPorRango(fecha.plusDays(1), fecha));
        assertThrows(ReglaNegocioException.class, () -> servicio.buscarPorRango(null, fecha));
    }

    @Test
    void unaPartidaNoSeEditaNiSeBorra() {
        for (Method m : PartidaService.class.getDeclaredMethods()) {
            String nombre = m.getName().toLowerCase();
            assertTrue(!nombre.contains("modific") && !nombre.contains("elimin") && !nombre.contains("edit")
                    && !nombre.contains("borr") && !nombre.contains("actualiz") && !nombre.contains("anular"),
                    "PartidaService no debe exponer: " + m.getName());
        }
    }
}
