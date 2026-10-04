package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Invariantes de los movimientos de kardex segun el documento de reglas (puro, sin mocks). */
class ReglasKardexTest {

    private final Almacen a = Fabrica.almacen(1, true);
    private final Almacen b = Fabrica.almacen(2, true);
    private final Compra compra = new Compra();
    private final Venta venta = new Venta();
    private final List<DetalleKardex> unDetalle = List.of(
            new DetalleKardex(Fabrica.lote(1), new BigDecimal("2"), new BigDecimal("1.50")));

    private ReglaNegocioException rechaza(TipoMovimiento tipo, Almacen origen, Almacen destino, Compra c, Venta v,
            List<DetalleKardex> detalles) {
        return assertThrows(ReglaNegocioException.class, () -> ReglasKardex.validar(tipo, origen, destino, c, v, detalles));
    }

    @Test
    void entradaRequiereDestinoYNoLlevaOrigen() {
        assertDoesNotThrow(() -> ReglasKardex.validar(TipoMovimiento.Entrada, null, a, compra, null, unDetalle));
        assertDoesNotThrow(() -> ReglasKardex.validar(TipoMovimiento.Entrada, null, a, null, null, unDetalle));
        rechaza(TipoMovimiento.Entrada, a, a, compra, null, unDetalle);
        rechaza(TipoMovimiento.Entrada, null, null, compra, null, unDetalle);
        rechaza(TipoMovimiento.Entrada, null, a, null, venta, unDetalle);
    }

    @Test
    void salidaRequiereOrigenYNoLlevaDestino() {
        assertDoesNotThrow(() -> ReglasKardex.validar(TipoMovimiento.Salida, a, null, null, venta, unDetalle));
        rechaza(TipoMovimiento.Salida, null, null, null, venta, unDetalle);
        rechaza(TipoMovimiento.Salida, a, b, null, venta, unDetalle);
        rechaza(TipoMovimiento.Salida, a, null, compra, null, unDetalle);
    }

    @Test
    void trasladoRequiereOrigenYDestinoDistintosYNoLlevaDocumento() {
        assertDoesNotThrow(() -> ReglasKardex.validar(TipoMovimiento.Traslado, a, b, null, null, unDetalle));
        assertTrue(rechaza(TipoMovimiento.Traslado, a, a, null, null, unDetalle).getMessage().contains("distint"));
        rechaza(TipoMovimiento.Traslado, a, null, null, null, unDetalle);
        rechaza(TipoMovimiento.Traslado, null, b, null, null, unDetalle);
        rechaza(TipoMovimiento.Traslado, a, b, compra, null, unDetalle);
        rechaza(TipoMovimiento.Traslado, a, b, null, venta, unDetalle);
    }

    @Test
    void ajusteLlevaExactamenteUnAlmacenComoOrigenODestino() {
        assertDoesNotThrow(() -> ReglasKardex.validar(TipoMovimiento.Ajuste, null, a, null, null, unDetalle));
        assertDoesNotThrow(() -> ReglasKardex.validar(TipoMovimiento.Ajuste, a, null, null, null, unDetalle));
        rechaza(TipoMovimiento.Ajuste, a, b, null, null, unDetalle);
        rechaza(TipoMovimiento.Ajuste, null, null, null, null, unDetalle);
        rechaza(TipoMovimiento.Ajuste, null, a, compra, null, unDetalle);
    }

    @Test
    void compraYVentaNoVanJuntas() {
        rechaza(TipoMovimiento.Entrada, null, a, compra, venta, unDetalle);
    }

    @Test
    void tipoObligatorio() {
        rechaza(null, null, a, null, null, unDetalle);
    }

    @Test
    void detallesObligatoriosYValidos() {
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, null);
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, List.of());
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, List.of(
                new DetalleKardex(null, BigDecimal.ONE, BigDecimal.ONE)));
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, List.of(
                new DetalleKardex(Fabrica.lote(1), BigDecimal.ZERO, BigDecimal.ONE)));
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, List.of(
                new DetalleKardex(Fabrica.lote(1), new BigDecimal("-1"), BigDecimal.ONE)));
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, List.of(
                new DetalleKardex(Fabrica.lote(1), BigDecimal.ONE, new BigDecimal("-0.01"))));
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, List.of(
                new DetalleKardex(Fabrica.lote(1), null, BigDecimal.ONE)));
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, List.of(
                new DetalleKardex(Fabrica.lote(1), BigDecimal.ONE, null)));
        ArrayList<DetalleKardex> conNulo = new ArrayList<>();
        conNulo.add(null);
        rechaza(TipoMovimiento.Entrada, null, a, compra, null, conNulo);
    }

    @Test
    void precioCostoCeroEsValido() {
        assertDoesNotThrow(() -> ReglasKardex.validar(TipoMovimiento.Entrada, null, a, compra, null, List.of(
                new DetalleKardex(Fabrica.lote(1), BigDecimal.ONE, BigDecimal.ZERO))));
    }
}
