package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.config.ConfigPruebas;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.DetallePlanillaCmd;
import com.sistemacontable.control.service.cmd.PlanillaCmd;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Planilla;
import com.sistemacontable.entity.PlanillaDetalle;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import com.sistemacontable.entity.enums.EstadoPlanilla;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** PlanillaService con DAOs reales sobre H2 (EclipseLink); todo termina en rollback. */
class PlanillaH2Test {

    private EntityManager em;
    private EscenarioContable esc;
    private PlanillaService servicio;
    private Empleado luis;

    private final OffsetDateTime inicio = OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime fin = OffsetDateTime.of(2026, 6, 30, 23, 0, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime pago = OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void abrir() {
        em = JpaTestSupport.nuevoEmConTransaccion();
        esc = new EscenarioContable(em);
        luis = esc.nuevoEmpleado("Luis", "Perez", "00000002-2", "ISSS-2", bd("600.00"));
        Empleado baja = esc.nuevoEmpleado("Baja", "Inactiva", "00000003-3", "ISSS-3", bd("900.00"));
        baja.setEstado(EstadoEmpleado.Inactivo);
        em.flush();
        servicio = new Cableado(em, ConfigPruebas.porDefecto()).obtener(PlanillaService.class);
    }

    @AfterEach
    void cerrar() {
        JpaTestSupport.cerrarConRollback(em);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private Planilla nuevaPlanilla() {
        return servicio.crear(new PlanillaCmd(inicio, fin, pago, "Junio 2026"));
    }

    @Test
    void generaDetallesSoloParaActivosYCalculaLosTotales() {
        Planilla p = nuevaPlanilla();

        List<PlanillaDetalle> detalles = servicio.generarDetalles(p.getId());

        assertEquals(2, detalles.size()); // Ana y Luis; la inactiva no entra
        assertEquals(0, servicio.generarDetalles(p.getId()).size()); // idempotente: ya estan todos
        ResumenPlanilla r = servicio.calcular(p.getId());
        assertEquals(bd("1600.00"), r.totalDevengado());
        assertEquals(bd("1351.23"), r.totalLiquido());
    }

    @Test
    void elSueldoBaseQuedaCongeladoAunqueElEmpleadoCambieDeSalario() {
        Planilla p = nuevaPlanilla();
        servicio.agregarDetalle(p.getId(), new DetallePlanillaCmd(luis.getId(), 30, 0, 0));
        luis.setSalario(bd("5000"));
        em.flush();
        em.clear();

        assertEquals(bd("600.00"), servicio.calcular(p.getId()).totalDevengado());
    }

    @Test
    void pagarMarcaPagadoYCreaUnaSolaPartidaDeEgresoCuadrada() {
        Planilla p = nuevaPlanilla();
        servicio.generarDetalles(p.getId());

        PagoPlanilla r = servicio.pagar(p.getId());
        em.flush();
        em.clear();

        assertEquals(EstadoPlanilla.Pagado, servicio.buscarPorId(p.getId()).getEstado());
        Partida partida = em.find(Partida.class, r.partida().getId());
        assertEquals(TipoPartida.Egreso, partida.getTipo());
        assertEquals(pago.toLocalDate(), partida.getFecha());
        List<PartidaDetalle> lineas = em.createQuery("SELECT d FROM PartidaDetalle d WHERE d.partida = :p",
                PartidaDetalle.class).setParameter("p", partida).getResultList();
        assertEquals(5, lineas.size());
        BigDecimal cargos = lineas.stream().map(PartidaDetalle::getCargo).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonos = lineas.stream().map(PartidaDetalle::getAbono).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, cargos.compareTo(abonos));
        assertEquals(0, bd("1600.00").compareTo(cargos));

        // no se duplica el pago
        assertThrows(ReglaNegocioException.class, () -> servicio.pagar(p.getId()));
        assertEquals(1, esc.contar(Partida.class));
    }

    @Test
    void unaPlanillaPagadaEsInmutable() {
        Planilla p = nuevaPlanilla();
        PlanillaDetalle d = servicio.agregarDetalle(p.getId(), new DetallePlanillaCmd(esc.empleado.getId(), 30, 0, 0));
        servicio.pagar(p.getId());

        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(p.getId(), new DetallePlanillaCmd(luis.getId(), 30, 0, 0)));
        assertThrows(ReglaNegocioException.class, () -> servicio.modificarDetalle(d.getId(),
                new DetallePlanillaCmd(null, 10, 0, 0)));
        assertThrows(ReglaNegocioException.class, () -> servicio.eliminarDetalle(d.getId()));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(p.getId(), new PlanillaCmd(inicio, fin, pago, "otra")));
    }

    @Test
    void sinCuentaDeBancosConfiguradaNoSePagaYQuedaPendiente() {
        Planilla p = nuevaPlanilla();
        servicio.generarDetalles(p.getId());
        PlanillaService sinBancos = new Cableado(em, ConfigPruebas.con(Map.of("cuenta.bancos", "9999")))
                .obtener(PlanillaService.class);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> sinBancos.pagar(p.getId()));

        assertTrue(ex.getMessage().contains("cuenta.bancos"), ex.getMessage());
        assertEquals(EstadoPlanilla.Pendiente, p.getEstado());
        assertEquals(0, esc.contar(Partida.class));
    }
}
