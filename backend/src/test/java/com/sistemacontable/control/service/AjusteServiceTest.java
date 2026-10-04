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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.AjusteCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Partida;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AjusteServiceTest {

    @Mock
    AlmacenDAO almacenDAO;
    @Mock
    LoteDAO loteDAO;
    @Mock
    ExistenciasService existenciasService;
    @Mock
    KardexService kardexService;
    @Mock
    PartidaService partidaService;
    @Mock
    ConfigContable config;
    @InjectMocks
    AjusteService servicio;

    private final Almacen almacen = Fabrica.almacen(20, true);
    private final Almacen cerrado = Fabrica.almacen(22, false);
    private final Lote lote = Fabrica.lote(10);
    private final Cuenta inventario = Fabrica.cuenta(1, 1104);
    private final Cuenta cuentaAjuste = Fabrica.cuenta(2, 5301);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        when(almacenDAO.buscarPorId(20L)).thenReturn(Optional.of(almacen));
        when(almacenDAO.buscarPorId(22L)).thenReturn(Optional.of(cerrado));
        when(loteDAO.buscarPorId(10L)).thenReturn(Optional.of(lote));
        when(existenciasService.costoPromedio(10L)).thenReturn(Optional.of(bd("4.0000")));
        when(config.getBoolean(ConfigContable.AJUSTE_GENERA_PARTIDA)).thenReturn(false);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO)).thenReturn(inventario);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_AJUSTE_INVENTARIO)).thenReturn(cuentaAjuste);
        when(kardexService.registrar(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new MovimientoKardex(new Kardex(), List.of()));
        when(partidaService.registrar(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Partida p = new Partida();
            p.setId(9L);
            return p;
        });
    }

    private void noSeEscribioNada() {
        verify(existenciasService, never()).disminuir(any(), any(), any());
        verify(existenciasService, never()).aumentar(any(), any(), any());
        verify(kardexService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<LineaAsiento>> lineasCaptor() {
        return ArgumentCaptor.forClass((Class<List<LineaAsiento>>) (Class<?>) List.class);
    }

    @Test
    void ajustePositivoEsEntradaConDestinoAlmacenYCantidadPositiva() {
        ResultadoAjuste r = servicio.registrar(new AjusteCmd(20L, 10L, bd("5"), "Conteo fisico"));

        verify(existenciasService).aumentar(lote, almacen, bd("5"));
        verify(kardexService).registrar(eq(TipoMovimiento.Ajuste), isNull(), same(almacen), isNull(), isNull(),
                eq("Conteo fisico"), eq(List.of(new DetalleKardex(lote, bd("5"), bd("4.0000")))));
        assertNull(r.partida());
        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void ajusteNegativoEsSalidaConOrigenAlmacenCantidadPositivaYVerificaStock() {
        servicio.registrar(new AjusteCmd(20L, 10L, bd("-3"), "Merma"));

        ArgumentCaptor<Map<ClaveStock, BigDecimal>> requeridos = mapaCaptor();
        verify(existenciasService).verificarDisponible(requeridos.capture());
        assertEquals(0, bd("3").compareTo(requeridos.getValue().get(new ClaveStock(10L, 20L))));
        verify(existenciasService).disminuir(lote, almacen, bd("3"));
        verify(kardexService).registrar(eq(TipoMovimiento.Ajuste), same(almacen), isNull(), isNull(), isNull(),
                eq("Merma"), eq(List.of(new DetalleKardex(lote, bd("3"), bd("4.0000")))));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<ClaveStock, BigDecimal>> mapaCaptor() {
        return ArgumentCaptor.forClass((Class<Map<ClaveStock, BigDecimal>>) (Class<?>) Map.class);
    }

    @Test
    void conPartidaActivaUnaEntradaDebitaInventarioYAcreditaLaCuentaDeAjuste() {
        when(config.getBoolean(ConfigContable.AJUSTE_GENERA_PARTIDA)).thenReturn(true);

        ResultadoAjuste r = servicio.registrar(new AjusteCmd(20L, 10L, bd("5"), "Conteo"));

        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(eq(TipoPartida.Ajuste), eq(LocalDate.now()),
                eq("Ajuste de inventario: Conteo"), isNull(), isNull(), isNull(), lineas.capture());
        assertEquals(List.of(LineaAsiento.cargo(inventario, bd("20.00")), LineaAsiento.abono(cuentaAjuste, bd("20.00"))),
                lineas.getValue());
        assertEquals(9L, r.partida().getId());
    }

    @Test
    void conPartidaActivaUnaSalidaDebitaLaCuentaDeAjusteYAcreditaInventario() {
        when(config.getBoolean(ConfigContable.AJUSTE_GENERA_PARTIDA)).thenReturn(true);

        servicio.registrar(new AjusteCmd(20L, 10L, bd("-2.5"), "Merma"));

        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(eq(TipoPartida.Ajuste), any(), any(), isNull(), isNull(), isNull(),
                lineas.capture());
        assertEquals(List.of(LineaAsiento.cargo(cuentaAjuste, bd("10.00")), LineaAsiento.abono(inventario, bd("10.00"))),
                lineas.getValue());
    }

    @Test
    void conPartidaActivaYLoteSinCostoSeRechazaSinEscribir() {
        when(config.getBoolean(ConfigContable.AJUSTE_GENERA_PARTIDA)).thenReturn(true);
        when(existenciasService.costoPromedio(10L)).thenReturn(Optional.empty());

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(new AjusteCmd(20L, 10L, bd("5"), "Conteo")));

        assertTrue(ex.getMessage().contains("costo"), ex.getMessage());
        noSeEscribioNada();
    }

    @Test
    void sinPartidaActivaUnLoteSinCostoSeAjustaConCostoCero() {
        when(existenciasService.costoPromedio(10L)).thenReturn(Optional.empty());

        servicio.registrar(new AjusteCmd(20L, 10L, bd("5"), "Conteo"));

        verify(kardexService).registrar(any(), any(), any(), any(), any(), any(),
                eq(List.of(new DetalleKardex(lote, bd("5"), BigDecimal.ZERO))));
    }

    @Test
    void conStockInsuficienteNoSeEscribeNada() {
        doThrow(new ReglaNegocioException("Stock insuficiente")).when(existenciasService).verificarDisponible(any());

        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(new AjusteCmd(20L, 10L, bd("-100"), "Merma")));

        noSeEscribioNada();
    }

    @Test
    void datosInvalidosSeRechazanSinEscribir() {
        when(almacenDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        when(loteDAO.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(null));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new AjusteCmd(null, 10L, bd("1"), "x")));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new AjusteCmd(20L, null, bd("1"), "x")));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new AjusteCmd(20L, 10L, null, "x")));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new AjusteCmd(20L, 10L, BigDecimal.ZERO, "x")));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new AjusteCmd(20L, 10L, bd("1"), null)));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new AjusteCmd(20L, 10L, bd("1"), "  ")));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new AjusteCmd(22L, 10L, bd("1"), "x")));
        assertThrows(NoEncontradoException.class, () -> servicio.registrar(new AjusteCmd(99L, 10L, bd("1"), "x")));
        assertThrows(NoEncontradoException.class, () -> servicio.registrar(new AjusteCmd(20L, 99L, bd("1"), "x")));
        noSeEscribioNada();
    }
}
