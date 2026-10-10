package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.ExistenciasDAO;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Existencias;
import com.sistemacontable.entity.Lote;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
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
class ExistenciasServiceTest {

    @Mock
    ExistenciasDAO existenciasDAO;
    @Mock
    CompraDetalleDAO compraDetalleDAO;
    @InjectMocks
    ExistenciasService servicio;

    private final Lote lote = Fabrica.lote(10);
    private final Almacen almacen = Fabrica.almacen(20, true);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private Existencias fila(String cantidad) {
        Existencias e = new Existencias();
        e.setLote(lote);
        e.setAlmacen(almacen);
        e.setCantidad(bd(cantidad));
        return e;
    }

    @BeforeEach
    void preparar() {
        when(existenciasDAO.findForUpdate(10L, 20L)).thenReturn(Optional.empty());
    }

    // ---------------------------------------------------------------------------------------- aumentar

    @Test
    void aumentarSinFilaPreviaCreaLaFilaConLaCantidad() {
        Existencias e = servicio.aumentar(lote, almacen, bd("5"));

        ArgumentCaptor<Existencias> captor = ArgumentCaptor.forClass(Existencias.class);
        verify(existenciasDAO).crear(captor.capture());
        assertEquals(e, captor.getValue());
        assertEquals(lote, e.getLote());
        assertEquals(almacen, e.getAlmacen());
        assertEquals(0, bd("5").compareTo(e.getCantidad()));
    }

    @Test
    void aumentarSumaSobreLaFilaExistenteBloqueadaConForUpdate() {
        Existencias existente = fila("3");
        when(existenciasDAO.findForUpdate(10L, 20L)).thenReturn(Optional.of(existente));

        Existencias e = servicio.aumentar(lote, almacen, bd("2.5"));

        assertEquals(0, bd("5.5").compareTo(e.getCantidad()));
        verify(existenciasDAO).modificar(existente);
        verify(existenciasDAO, never()).crear(any());
    }

    @Test
    void aumentarRechazaCantidadNoPositivaONula() {
        assertThrows(ReglaNegocioException.class, () -> servicio.aumentar(lote, almacen, BigDecimal.ZERO));
        assertThrows(ReglaNegocioException.class, () -> servicio.aumentar(lote, almacen, bd("-1")));
        assertThrows(ReglaNegocioException.class, () -> servicio.aumentar(lote, almacen, null));
        verify(existenciasDAO, never()).crear(any());
        verify(existenciasDAO, never()).modificar(any());
    }

    // --------------------------------------------------------------------------------------- disminuir

    @Test
    void disminuirRestaDelStock() {
        Existencias existente = fila("10");
        when(existenciasDAO.findForUpdate(10L, 20L)).thenReturn(Optional.of(existente));

        Existencias e = servicio.disminuir(lote, almacen, bd("4"));

        assertEquals(0, bd("6").compareTo(e.getCantidad()));
        verify(existenciasDAO).modificar(existente);
    }

    @Test
    void disminuirTodoElStockDejaCeroPeroNuncaNegativo() {
        Existencias existente = fila("10");
        when(existenciasDAO.findForUpdate(10L, 20L)).thenReturn(Optional.of(existente));

        assertEquals(0, BigDecimal.ZERO.compareTo(servicio.disminuir(lote, almacen, bd("10.00")).getCantidad()));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.disminuir(lote, almacen, bd("0.01")));
        assertTrue(ex.getMessage().contains("insuficiente"), ex.getMessage());
        assertEquals(0, BigDecimal.ZERO.compareTo(existente.getCantidad()));
    }

    @Test
    void disminuirConStockInsuficienteIndicaDisponibleYRequeridoYNoModifica() {
        Existencias existente = fila("3");
        when(existenciasDAO.findForUpdate(10L, 20L)).thenReturn(Optional.of(existente));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.disminuir(lote, almacen, bd("3.01")));

        assertTrue(ex.getMessage().contains("disponible 3") && ex.getMessage().contains("requerido 3.01"),
                ex.getMessage());
        assertEquals(0, bd("3").compareTo(existente.getCantidad()));
        verify(existenciasDAO, never()).modificar(any());
    }

    @Test
    void disminuirSinFilaEsStockInsuficiente() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.disminuir(lote, almacen, bd("1")));

        assertTrue(ex.getMessage().contains("insuficiente") && ex.getMessage().contains("disponible 0"), ex.getMessage());
    }

    @Test
    void disminuirRechazaCantidadNoPositiva() {
        assertThrows(ReglaNegocioException.class, () -> servicio.disminuir(lote, almacen, BigDecimal.ZERO));
        assertThrows(ReglaNegocioException.class, () -> servicio.disminuir(lote, almacen, null));
    }

    // ---------------------------------------------------------------------------- verificarDisponible

    @Test
    void verificarDisponibleBloqueaLasFilasEnOrdenDeClaveParaEvitarInterbloqueos() {
        when(existenciasDAO.findForUpdate(1L, 9L)).thenReturn(Optional.of(fila("100")));
        when(existenciasDAO.findForUpdate(2L, 3L)).thenReturn(Optional.of(fila("100")));
        when(existenciasDAO.findForUpdate(2L, 5L)).thenReturn(Optional.of(fila("100")));
        Map<ClaveStock, BigDecimal> requeridos = new LinkedHashMap<>();
        requeridos.put(new ClaveStock(2L, 5L), bd("1"));
        requeridos.put(new ClaveStock(1L, 9L), bd("1"));
        requeridos.put(new ClaveStock(2L, 3L), bd("1"));

        servicio.verificarDisponible(requeridos);

        InOrder orden = inOrder(existenciasDAO);
        orden.verify(existenciasDAO).findForUpdate(1L, 9L);
        orden.verify(existenciasDAO).findForUpdate(2L, 3L);
        orden.verify(existenciasDAO).findForUpdate(2L, 5L);
    }

    @Test
    void verificarDisponibleRechazaSiAlgunaClaveNoAlcanzaYNoModificaNada() {
        when(existenciasDAO.findForUpdate(1L, 1L)).thenReturn(Optional.of(fila("10")));
        when(existenciasDAO.findForUpdate(2L, 1L)).thenReturn(Optional.of(fila("1")));
        Map<ClaveStock, BigDecimal> requeridos = new LinkedHashMap<>();
        requeridos.put(new ClaveStock(1L, 1L), bd("10"));
        requeridos.put(new ClaveStock(2L, 1L), bd("2"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.verificarDisponible(requeridos));

        assertTrue(ex.getMessage().contains("lote 2") && ex.getMessage().contains("almacen 1"), ex.getMessage());
        verify(existenciasDAO, never()).modificar(any());
        verify(existenciasDAO, never()).crear(any());
    }

    @Test
    void verificarDisponibleSinFilaEquivaleAStockCero() {
        Map<ClaveStock, BigDecimal> requeridos = Map.of(new ClaveStock(10L, 20L), bd("1"));

        assertThrows(ReglaNegocioException.class, () -> servicio.verificarDisponible(requeridos));
    }

    @Test
    void verificarDisponibleConMapaVacioNoHaceNada() {
        servicio.verificarDisponible(Map.of());

        verify(existenciasDAO, never()).findForUpdate(any(), any());
    }

    // ------------------------------------------------------------------------------ costo promedio

    @Test
    void costoPromedioSeRedondeaACuatroDecimales() {
        when(compraDetalleDAO.costoPromedioPonderado(10L)).thenReturn(Optional.of(bd("3.33333333333")));

        assertEquals(Optional.of(bd("3.3333")), servicio.costoPromedio(10L));
    }

    @Test
    void costoPromedioDeUnLoteNuncaCompradoEsVacio() {
        when(compraDetalleDAO.costoPromedioPonderado(10L)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), servicio.costoPromedio(10L));
    }
}
