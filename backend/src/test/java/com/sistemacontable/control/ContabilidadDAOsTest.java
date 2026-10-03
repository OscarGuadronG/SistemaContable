package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.sistemacontable.control.PartidaDetalleDAO.SumaCuenta;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContabilidadDAOsTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    private SimulacionJpa jpa;

    @BeforeEach
    void preparar() {
        jpa = new SimulacionJpa();
    }

    @Test
    void findByRangoIncluyeAmbosExtremosYOrdenaPorFechaEId() {
        Partida p = new Partida();
        TypedQuery<Partida> q = jpa.lista(Partida.class, List.of(p));

        assertEquals(List.of(p), jpa.inyectar(new PartidaDAO()).findByRango(DESDE, HASTA));

        String jpql = jpa.jpql(Partida.class);
        assertTrue(jpql.contains("p.fecha BETWEEN :desde AND :hasta"), jpql);
        assertTrue(jpql.contains("ORDER BY p.fecha, p.id"), jpql);
        verify(q).setParameter("desde", DESDE);
        verify(q).setParameter("hasta", HASTA);
    }

    @Test
    void findByRangoValidaArgumentos() {
        PartidaDAO dao = jpa.inyectar(new PartidaDAO());

        assertThrows(IllegalArgumentException.class, () -> dao.findByRango(null, HASTA));
        assertThrows(IllegalArgumentException.class, () -> dao.findByRango(DESDE, null));
        assertThrows(IllegalArgumentException.class, () -> dao.findByRango(HASTA, DESDE));
    }

    @Test
    void buscarPorPartidaFiltraPorIdDePartida() {
        PartidaDetalle d = new PartidaDetalle();
        TypedQuery<PartidaDetalle> q = jpa.lista(PartidaDetalle.class, List.of(d));

        assertEquals(List.of(d), jpa.inyectar(new PartidaDetalleDAO()).buscarPorPartida(3L));

        assertTrue(jpa.jpql(PartidaDetalle.class).contains("d.partida.id = :partidaId"));
        verify(q).setParameter("partidaId", 3L);
    }

    @Test
    void sumasPorCuentaAgrupaPorCuentaYConvierteLasFilas() {
        TypedQuery<Object[]> q = jpa.lista(Object[].class, List.of(
                new Object[] {10L, new BigDecimal("100"), new BigDecimal("0")},
                new Object[] {20L, new BigDecimal("0"), new BigDecimal("100")}));

        List<SumaCuenta> sumas = jpa.inyectar(new PartidaDetalleDAO()).sumasPorCuenta(DESDE, HASTA);

        assertEquals(2, sumas.size());
        assertEquals(10L, sumas.get(0).cuentaId());
        assertEquals(0, new BigDecimal("100").compareTo(sumas.get(0).totalCargo()));
        assertEquals(0, new BigDecimal("100").compareTo(sumas.get(1).totalAbono()));
        String jpql = jpa.jpql(Object[].class);
        assertTrue(jpql.contains("SUM(d.cargo)") && jpql.contains("SUM(d.abono)"), jpql);
        assertTrue(jpql.contains("GROUP BY d.cuenta.id"), jpql);
        assertTrue(jpql.contains("d.partida.fecha BETWEEN :desde AND :hasta"), jpql);
        verify(q).setParameter("desde", DESDE);
        verify(q).setParameter("hasta", HASTA);
    }

    @Test
    void sumasPorCuentaValidaArgumentos() {
        PartidaDetalleDAO dao = jpa.inyectar(new PartidaDetalleDAO());

        assertThrows(IllegalArgumentException.class, () -> dao.sumasPorCuenta(null, HASTA));
        assertThrows(IllegalArgumentException.class, () -> dao.sumasPorCuenta(HASTA, DESDE));
        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorPartida(null));
    }
}
