package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.KardexDetalleDAO;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.enums.TipoMovimiento;
import java.math.BigDecimal;
import java.util.List;
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
class KardexServiceTest {

    @Mock
    KardexDAO kardexDAO;
    @Mock
    KardexDetalleDAO kardexDetalleDAO;
    @InjectMocks
    KardexService servicio;

    private final Almacen almacen = Fabrica.almacen(1, true);
    private final Lote lote = Fabrica.lote(7);

    @Test
    void registraLaCabeceraYUnaFilaPorDetalle() {
        Compra compra = new Compra();
        Lote otro = Fabrica.lote(8);
        List<DetalleKardex> detalles = List.of(
                new DetalleKardex(lote, new BigDecimal("3"), new BigDecimal("2.50")),
                new DetalleKardex(otro, new BigDecimal("1"), new BigDecimal("9.99")));

        MovimientoKardex m = servicio.registrar(TipoMovimiento.Entrada, null, almacen, compra, null, "Compra #1", detalles);

        ArgumentCaptor<Kardex> cabecera = ArgumentCaptor.forClass(Kardex.class);
        verify(kardexDAO).crear(cabecera.capture());
        Kardex k = cabecera.getValue();
        assertSame(k, m.kardex());
        assertEquals(TipoMovimiento.Entrada, k.getTipoMovimiento());
        assertNull(k.getOrigen());
        assertSame(almacen, k.getDestino());
        assertSame(compra, k.getCompra());
        assertNull(k.getVenta());
        assertEquals("Compra #1", k.getDescripcion());
        assertNotNull(k.getFecha());

        ArgumentCaptor<KardexDetalle> filas = ArgumentCaptor.forClass(KardexDetalle.class);
        verify(kardexDetalleDAO, times(2)).crear(filas.capture());
        KardexDetalle d1 = filas.getAllValues().get(0);
        assertSame(k, d1.getKardex());
        assertSame(lote, d1.getLote());
        assertEquals(new BigDecimal("3"), d1.getCantidad());
        assertEquals(new BigDecimal("2.50"), d1.getPrecioCosto());
        assertSame(otro, filas.getAllValues().get(1).getLote());
        assertEquals(2, m.detalles().size());
        verify(kardexDAO).flush();
    }

    @Test
    void unMovimientoInvalidoNoEscribeNada() {
        List<DetalleKardex> detalles = List.of(new DetalleKardex(lote, BigDecimal.ONE, BigDecimal.ONE));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(TipoMovimiento.Traslado, almacen, almacen, null, null, "x", detalles));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.registrar(TipoMovimiento.Entrada, null, almacen, null, null, "x", List.of()));

        verify(kardexDAO, never()).crear(any());
        verify(kardexDetalleDAO, never()).crear(any());
    }
}
