package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.VentaDetalle;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DetalleDAOsTest {

    private SimulacionJpa jpa;

    @BeforeEach
    void preparar() {
        jpa = new SimulacionJpa();
    }

    // ---------------------------------------------------------------- CompraDetalleDAO

    @Test
    void buscarPorCompraFiltraPorIdDeCompra() {
        CompraDetalle d = new CompraDetalle();
        TypedQuery<CompraDetalle> q = jpa.lista(CompraDetalle.class, List.of(d));

        assertEquals(List.of(d), jpa.inyectar(new CompraDetalleDAO()).buscarPorCompra(4L));

        assertTrue(jpa.jpql(CompraDetalle.class).contains("d.compra.id = :compraId"));
        verify(q).setParameter("compraId", 4L);
    }

    @Test
    void totalCompraSumaCantidadPorPrecio() {
        TypedQuery<BigDecimal> q = jpa.unico(BigDecimal.class, new BigDecimal("250.75"));

        BigDecimal total = jpa.inyectar(new CompraDetalleDAO()).totalCompra(4L);

        assertEquals(0, new BigDecimal("250.75").compareTo(total));
        assertTrue(jpa.jpql(BigDecimal.class).contains("SUM(d.cantidad * d.precio)"));
        verify(q).setParameter("compraId", 4L);
    }

    @Test
    void totalCompraSinDetallesEsCeroYNoNulo() {
        jpa.unico(BigDecimal.class, null);

        assertEquals(BigDecimal.ZERO, jpa.inyectar(new CompraDetalleDAO()).totalCompra(4L));
    }

    @Test
    void costoPromedioPonderadoDivideImporteEntreCantidad() {
        // 10 u a 2.00 + 30 u a 4.00 = 140 / 40 = 3.50 (no el promedio simple de precios, que seria 3.00)
        TypedQuery<Object[]> q = jpa.unico(Object[].class, new Object[] {new BigDecimal("140"), new BigDecimal("40")});

        Optional<BigDecimal> costo = jpa.inyectar(new CompraDetalleDAO()).costoPromedioPonderado(9L);

        assertEquals(0, new BigDecimal("3.5").compareTo(costo.orElseThrow()));
        String jpql = jpa.jpql(Object[].class);
        assertTrue(jpql.contains("SUM(d.cantidad * d.precio)") && jpql.contains("SUM(d.cantidad)"), jpql);
        assertTrue(jpql.contains("d.lote.id = :loteId"), jpql);
        verify(q).setParameter("loteId", 9L);
    }

    @Test
    void costoPromedioPonderadoSinComprasEsVacio() {
        jpa.unico(Object[].class, new Object[] {null, null});

        assertEquals(Optional.empty(), jpa.inyectar(new CompraDetalleDAO()).costoPromedioPonderado(9L));
    }

    @Test
    void costoPromedioPonderadoConCantidadCeroEsVacioYNoDividePorCero() {
        jpa.unico(Object[].class, new Object[] {BigDecimal.ZERO, BigDecimal.ZERO});

        assertEquals(Optional.empty(), jpa.inyectar(new CompraDetalleDAO()).costoPromedioPonderado(9L));
    }

    @Test
    void costoPromedioPonderadoNoRedondeaResultadosPeriodicos() {
        jpa.unico(Object[].class, new Object[] {new BigDecimal("10"), new BigDecimal("3")});

        BigDecimal costo = jpa.inyectar(new CompraDetalleDAO()).costoPromedioPonderado(1L).orElseThrow();

        // 10/3 con precision amplia (DECIMAL128), no recortado a 2 decimales
        assertTrue(costo.toPlainString().startsWith("3.333333333333333"), costo.toPlainString());
    }

    @Test
    void metodosDeCompraDetalleRechazanIdsNulos() {
        CompraDetalleDAO dao = jpa.inyectar(new CompraDetalleDAO());

        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorCompra(null));
        assertThrows(IllegalArgumentException.class, () -> dao.totalCompra(null));
        assertThrows(IllegalArgumentException.class, () -> dao.costoPromedioPonderado(null));
    }

    // ---------------------------------------------------------------- VentaDetalleDAO

    @Test
    void buscarPorVentaFiltraPorIdDeVenta() {
        VentaDetalle d = new VentaDetalle();
        TypedQuery<VentaDetalle> q = jpa.lista(VentaDetalle.class, List.of(d));

        assertEquals(List.of(d), jpa.inyectar(new VentaDetalleDAO()).buscarPorVenta(8L));

        assertTrue(jpa.jpql(VentaDetalle.class).contains("d.venta.id = :ventaId"));
        verify(q).setParameter("ventaId", 8L);
    }

    @Test
    void totalVentaSumaCantidadPorPrecioYEsCeroSinDetalles() {
        jpa.unico(BigDecimal.class, null);

        assertEquals(BigDecimal.ZERO, jpa.inyectar(new VentaDetalleDAO()).totalVenta(8L));
        assertTrue(jpa.jpql(BigDecimal.class).contains("SUM(d.cantidad * d.precio)"));
    }

    @Test
    void metodosDeVentaDetalleRechazanIdsNulos() {
        VentaDetalleDAO dao = jpa.inyectar(new VentaDetalleDAO());

        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorVenta(null));
        assertThrows(IllegalArgumentException.class, () -> dao.totalVenta(null));
    }
}
