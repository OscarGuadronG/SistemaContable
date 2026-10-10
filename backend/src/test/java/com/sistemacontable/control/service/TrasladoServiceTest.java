package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaTrasladoCmd;
import com.sistemacontable.control.service.cmd.TrasladoCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.enums.TipoMovimiento;
import java.math.BigDecimal;
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
class TrasladoServiceTest {

    @Mock
    AlmacenDAO almacenDAO;
    @Mock
    LoteDAO loteDAO;
    @Mock
    ExistenciasService existenciasService;
    @Mock
    KardexService kardexService;
    @InjectMocks
    TrasladoService servicio;

    private final Almacen origen = Fabrica.almacen(20, true);
    private final Almacen destino = Fabrica.almacen(21, true);
    private final Almacen cerrado = Fabrica.almacen(22, false);
    private final Lote lote10 = Fabrica.lote(10);
    private final Lote lote11 = Fabrica.lote(11);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        when(almacenDAO.buscarPorId(20L)).thenReturn(Optional.of(origen));
        when(almacenDAO.buscarPorId(21L)).thenReturn(Optional.of(destino));
        when(almacenDAO.buscarPorId(22L)).thenReturn(Optional.of(cerrado));
        when(loteDAO.buscarPorId(10L)).thenReturn(Optional.of(lote10));
        when(loteDAO.buscarPorId(11L)).thenReturn(Optional.of(lote11));
        when(existenciasService.costoPromedio(10L)).thenReturn(Optional.of(bd("4.0000")));
        when(existenciasService.costoPromedio(11L)).thenReturn(Optional.empty());
        when(kardexService.registrar(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> new MovimientoKardex(new Kardex(), List.of()));
    }

    private TrasladoCmd cmdValido() {
        return new TrasladoCmd(20L, 21L, "Reabastecimiento", List.of(
                new LineaTrasladoCmd(10L, bd("3")), new LineaTrasladoCmd(11L, bd("2"))));
    }

    private void noSeEscribioNada() {
        verify(existenciasService, never()).disminuir(any(), any(), any());
        verify(existenciasService, never()).aumentar(any(), any(), any());
        verify(kardexService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void trasladaElStockYRegistraUnKardexTrasladoSinPartida() {
        MovimientoKardex m = servicio.registrar(cmdValido());

        verify(existenciasService).disminuir(lote10, origen, bd("3"));
        verify(existenciasService).aumentar(lote10, destino, bd("3"));
        verify(existenciasService).disminuir(lote11, origen, bd("2"));
        verify(existenciasService).aumentar(lote11, destino, bd("2"));
        // un solo kardex Traslado con una linea por lote; costo informativo (cero si el lote nunca se compro)
        verify(kardexService).registrar(eq(TipoMovimiento.Traslado), same(origen), same(destino), isNull(), isNull(),
                eq("Reabastecimiento"), eq(List.of(
                        new DetalleKardex(lote10, bd("3"), bd("4.0000")),
                        new DetalleKardex(lote11, bd("2"), BigDecimal.ZERO))));
        assertNotNull(m.kardex());
    }

    @Test
    void verificaElStockAcumuladoDelOrigenAntesDeEscribir() {
        TrasladoCmd cmd = new TrasladoCmd(20L, 21L, null, List.of(
                new LineaTrasladoCmd(10L, bd("3")), new LineaTrasladoCmd(10L, bd("4"))));

        servicio.registrar(cmd);

        ArgumentCaptor<Map<ClaveStock, BigDecimal>> requeridos = mapaCaptor();
        InOrder orden = inOrder(existenciasService);
        orden.verify(existenciasService).verificarDisponible(requeridos.capture());
        orden.verify(existenciasService, org.mockito.Mockito.atLeastOnce()).disminuir(any(), any(), any());
        assertEquals(1, requeridos.getValue().size());
        assertEquals(0, bd("7").compareTo(requeridos.getValue().get(new ClaveStock(10L, 20L))));
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<ClaveStock, BigDecimal>> mapaCaptor() {
        return ArgumentCaptor.forClass((Class<Map<ClaveStock, BigDecimal>>) (Class<?>) Map.class);
    }

    @Test
    void conStockInsuficienteNoSeEscribeNada() {
        doThrow(new ReglaNegocioException("Stock insuficiente del lote 11 en el almacen 20"))
                .when(existenciasService).verificarDisponible(any());

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(cmdValido()));

        noSeEscribioNada();
    }

    @Test
    void origenYDestinoIgualesSeRechazan() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.registrar(
                new TrasladoCmd(20L, 20L, null, cmdValido().lineas())));

        assertTrue(ex.getMessage().contains("distint"), ex.getMessage());
        noSeEscribioNada();
    }

    @Test
    void datosObligatoriosYAlmacenesInvalidosSeRechazanSinEscribir() {
        when(almacenDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        List<LineaTrasladoCmd> l = cmdValido().lineas();

        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(null));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new TrasladoCmd(null, 21L, null, l)));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new TrasladoCmd(20L, null, null, l)));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new TrasladoCmd(20L, 21L, null, List.of())));
        assertThrows(NoEncontradoException.class, () -> servicio.registrar(new TrasladoCmd(99L, 21L, null, l)));
        assertThrows(NoEncontradoException.class, () -> servicio.registrar(new TrasladoCmd(20L, 99L, null, l)));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new TrasladoCmd(20L, 22L, null, l)));
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new TrasladoCmd(22L, 21L, null, l)));
        noSeEscribioNada();
    }

    @Test
    void lineasInvalidasSeRechazanSinEscribir() {
        when(loteDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        for (LineaTrasladoCmd mala : List.of(
                new LineaTrasladoCmd(11L, BigDecimal.ZERO), new LineaTrasladoCmd(11L, bd("-1")),
                new LineaTrasladoCmd(11L, null), new LineaTrasladoCmd(null, bd("1")))) {
            assertThrows(ReglaNegocioException.class, () -> servicio.registrar(new TrasladoCmd(20L, 21L, null,
                    List.of(new LineaTrasladoCmd(10L, bd("1")), mala))));
        }
        assertThrows(NoEncontradoException.class, () -> servicio.registrar(new TrasladoCmd(20L, 21L, null,
                List.of(new LineaTrasladoCmd(99L, bd("1"))))));
        noSeEscribioNada();
    }
}
