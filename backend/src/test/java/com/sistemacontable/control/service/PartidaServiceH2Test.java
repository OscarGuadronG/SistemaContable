package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.config.ConfigPruebas;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaPartidaCmd;
import com.sistemacontable.control.service.cmd.PartidaCmd;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** PartidaService con DAOs reales sobre H2 (EclipseLink); todo termina en rollback. */
class PartidaServiceH2Test {

    private EntityManager em;
    private EscenarioContable esc;
    private PartidaService servicio;

    @BeforeEach
    void abrir() {
        em = JpaTestSupport.nuevoEmConTransaccion();
        esc = new EscenarioContable(em);
        servicio = new Cableado(em, ConfigPruebas.porDefecto()).obtener(PartidaService.class);
    }

    @AfterEach
    void cerrar() {
        JpaTestSupport.cerrarConRollback(em);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void creaPartidaConSusLineasYSePuedeConsultar() {
        PartidaCmd cmd = new PartidaCmd(LocalDate.of(2026, 3, 1), "Venta mostrador", TipoPartida.Ingreso, null, null,
                null, List.of(new LineaPartidaCmd(esc.caja.getId(), bd("113.00"), null),
                        new LineaPartidaCmd(esc.ventas.getId(), null, bd("100.00")),
                        new LineaPartidaCmd(esc.ivaDebito.getId(), null, bd("13.00"))));

        Partida p = servicio.crear(cmd);

        assertNotNull(p.getId());
        List<PartidaDetalle> lineas = servicio.lineas(p.getId());
        assertEquals(3, lineas.size());
        assertSame(p, servicio.buscarPorId(p.getId()));
        assertEquals(List.of(p), servicio.buscarPorRango(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));
        assertEquals(0, servicio.buscarPorRango(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)).size());
    }

    @Test
    void unaPartidaDescuadradaNoDejaFilas() {
        PartidaCmd cmd = new PartidaCmd(LocalDate.of(2026, 3, 1), "Mal", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(esc.caja.getId(), bd("10"), null),
                        new LineaPartidaCmd(esc.ventas.getId(), null, bd("9"))));

        assertThrows(ReglaNegocioException.class, () -> servicio.crear(cmd));

        em.flush();
        assertEquals(0, esc.contar(Partida.class));
        assertEquals(0, esc.contar(PartidaDetalle.class));
    }

    @Test
    void cuentaConfiguradaResuelveElCatalogoRealYNombraLaClaveSiFalta() {
        assertSame(esc.inventario, servicio.cuentaConfigurada("cuenta.inventario"));

        PartidaService sinCaja = new Cableado(em, ConfigPruebas.con(Map.of("cuenta.caja", "9999")))
                .obtener(PartidaService.class);
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> sinCaja.cuentaConfigurada("cuenta.caja"));
        assertTrue(ex.getMessage().contains("cuenta.caja"), ex.getMessage());
    }
}
