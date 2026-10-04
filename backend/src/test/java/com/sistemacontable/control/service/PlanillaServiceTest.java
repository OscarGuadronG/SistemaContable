package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.PlanillaDAO;
import com.sistemacontable.control.PlanillaDetalleDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.config.ConfigPruebas;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.DetallePlanillaCmd;
import com.sistemacontable.control.service.cmd.PlanillaCmd;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Planilla;
import com.sistemacontable.entity.PlanillaDetalle;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import com.sistemacontable.entity.enums.EstadoPlanilla;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoPartida;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanillaServiceTest {

    @Mock
    PlanillaDAO planillaDAO;
    @Mock
    PlanillaDetalleDAO detalleDAO;
    @Mock
    EmpleadoDAO empleadoDAO;
    @Mock
    PartidaService partidaService;
    @Spy
    ConfigContable config = ConfigPruebas.porDefecto();
    @InjectMocks
    PlanillaService servicio;

    private final OffsetDateTime inicio = OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime fin = OffsetDateTime.of(2026, 6, 30, 23, 59, 0, 0, ZoneOffset.UTC);
    private final OffsetDateTime pago = OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.UTC);

    private final Planilla pendiente = planilla(1L, EstadoPlanilla.Pendiente);
    private final Planilla pagada = planilla(2L, EstadoPlanilla.Pagado);
    private final Empleado ana = empleado(10L, "1000", EstadoEmpleado.Activo);
    private final Empleado luis = empleado(11L, "600", EstadoEmpleado.Activo);
    private final Empleado inactivo = empleado(12L, "500", EstadoEmpleado.Inactivo);
    private final Empleado suspendido = empleado(13L, "500", EstadoEmpleado.Suspendido);

    private final Cuenta sueldos = Fabrica.cuenta(1, 5201);
    private final Cuenta retIsss = Fabrica.cuenta(2, 2103, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta retAfp = Fabrica.cuenta(3, 2104, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta retRenta = Fabrica.cuenta(4, 2105, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta bancos = Fabrica.cuenta(5, 1102);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private Planilla planilla(long id, EstadoPlanilla estado) {
        Planilla p = new Planilla();
        p.setId(id);
        p.setFechaInicio(inicio);
        p.setFechaFin(fin);
        p.setFechaPago(pago);
        p.setEstado(estado);
        return p;
    }

    private static Empleado empleado(long id, String salario, EstadoEmpleado estado) {
        Empleado e = new Empleado();
        e.setId(id);
        e.setSalario(bd(salario));
        e.setEstado(estado);
        e.setNombre("Emp" + id);
        e.setApellido("Ape");
        return e;
    }

    private PlanillaDetalle detalle(long id, Planilla p, Empleado e, int dias, int diur, int noct) {
        PlanillaDetalle d = new PlanillaDetalle();
        d.setId(id);
        d.setPlanilla(p);
        d.setEmpleado(e);
        d.setDias((short) dias);
        d.setHorasxDiur((short) diur);
        d.setHorasxNoct((short) noct);
        d.setSueldoBase(e.getSalario());
        return d;
    }

    @BeforeEach
    void preparar() {
        for (Planilla p : List.of(pendiente, pagada)) {
            when(planillaDAO.buscarParaActualizar(p.getId())).thenReturn(Optional.of(p));
            when(planillaDAO.buscarPorId(p.getId())).thenReturn(Optional.of(p));
        }
        for (Empleado e : List.of(ana, luis, inactivo, suspendido)) {
            when(empleadoDAO.buscarPorId(e.getId())).thenReturn(Optional.of(e));
        }
        when(planillaDAO.crear(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detalleDAO.crear(any())).thenAnswer(inv -> inv.getArgument(0));
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_SUELDOS)).thenReturn(sueldos);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_RETENCIONES_ISSS)).thenReturn(retIsss);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_RETENCIONES_AFP)).thenReturn(retAfp);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_RETENCIONES_RENTA)).thenReturn(retRenta);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_BANCOS)).thenReturn(bancos);
        when(partidaService.registrar(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Partida p = new Partida();
            p.setId(77L);
            return p;
        });
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<LineaAsiento>> lineasCaptor() {
        return ArgumentCaptor.forClass((Class<List<LineaAsiento>>) (Class<?>) List.class);
    }

    // ------------------------------------------------------------------------------ crear / modificar

    @Test
    void crearPlanillaPendienteConFechasValidas() {
        Planilla p = servicio.crear(new PlanillaCmd(inicio, fin, pago, " Junio "));

        verify(planillaDAO).crear(p);
        assertEquals(EstadoPlanilla.Pendiente, p.getEstado());
        assertEquals(inicio, p.getFechaInicio());
        assertEquals(fin, p.getFechaFin());
        assertEquals(pago, p.getFechaPago());
        assertEquals("Junio", p.getDescripcion());
    }

    @Test
    void crearConFechasIncoherentesOFaltantesSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(new PlanillaCmd(fin, inicio, pago, null)));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new PlanillaCmd(inicio, fin, fin.minusSeconds(1), null)));
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(new PlanillaCmd(null, fin, pago, null)));
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(new PlanillaCmd(inicio, null, pago, null)));
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(new PlanillaCmd(inicio, fin, null, null)));
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(null));
        verify(planillaDAO, never()).crear(any());
    }

    @Test
    void fechaPagoIgualALaDeFinEsValida() {
        assertNotNull(servicio.crear(new PlanillaCmd(inicio, fin, fin, null)));
        assertNotNull(servicio.crear(new PlanillaCmd(inicio, inicio, inicio, null)));
    }

    @Test
    void modificarSoloUnaPlanillaPendiente() {
        Planilla p = servicio.modificar(1L, new PlanillaCmd(inicio, fin, pago.plusDays(1), "Ajustada"));

        assertEquals("Ajustada", p.getDescripcion());
        assertEquals(pago.plusDays(1), p.getFechaPago());
        verify(planillaDAO).modificar(pendiente);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(2L, new PlanillaCmd(inicio, fin, pago, "x")));
        assertTrue(ex.getMessage().contains("pagada"), ex.getMessage());
    }

    @Test
    void modificarConFechasIncoherentesOInexistenteSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> servicio.modificar(1L, new PlanillaCmd(fin, inicio, pago, null)));
        when(planillaDAO.buscarParaActualizar(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.modificar(99L, new PlanillaCmd(inicio, fin, pago, null)));
    }

    // ------------------------------------------------------------------------------------- detalles

    @Test
    void agregarDetalleCongelaElSueldoBaseDelEmpleadoYUsaLosValoresPorDefecto() {
        PlanillaDetalle d = servicio.agregarDetalle(1L, new DetallePlanillaCmd(10L, null, null, null));

        verify(detalleDAO).crear(d);
        assertSame(pendiente, d.getPlanilla());
        assertSame(ana, d.getEmpleado());
        assertEquals(bd("1000"), d.getSueldoBase());
        assertEquals((short) 30, d.getDias());
        assertEquals((short) 0, d.getHorasxDiur());
        assertEquals((short) 0, d.getHorasxNoct());
    }

    @Test
    void agregarDetalleConDiasYHorasExplicitos() {
        PlanillaDetalle d = servicio.agregarDetalle(1L, new DetallePlanillaCmd(11L, 15, 4, 2));

        assertEquals((short) 15, d.getDias());
        assertEquals((short) 4, d.getHorasxDiur());
        assertEquals((short) 2, d.getHorasxNoct());
    }

    @Test
    void soloEmpleadosActivosEntranALaPlanilla() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(1L, new DetallePlanillaCmd(12L, null, null, null)));
        assertTrue(ex.getMessage().contains("Activo"), ex.getMessage());
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(1L, new DetallePlanillaCmd(13L, null, null, null)));
        verify(detalleDAO, never()).crear(any());
    }

    @Test
    void unEmpleadoNoPuedeTenerDosDetallesEnLaMismaPlanilla() {
        when(detalleDAO.existePorPlanillaYEmpleado(1L, 10L)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(1L, new DetallePlanillaCmd(10L, null, null, null)));

        assertTrue(ex.getMessage().contains("ya tiene"), ex.getMessage());
        verify(detalleDAO, never()).crear(any());
    }

    @Test
    void diasFueraDeRangoYHorasNegativasSeRechazan() {
        for (DetallePlanillaCmd cmd : List.of(
                new DetallePlanillaCmd(10L, -1, null, null), new DetallePlanillaCmd(10L, 32, null, null),
                new DetallePlanillaCmd(10L, 30, -1, null), new DetallePlanillaCmd(10L, 30, null, -1),
                new DetallePlanillaCmd(10L, 30, 40000, null), new DetallePlanillaCmd(10L, 30, null, 40000),
                new DetallePlanillaCmd(null, 30, 0, 0))) {
            assertThrows(ReglaNegocioException.class, () -> servicio.agregarDetalle(1L, cmd));
        }
        verify(detalleDAO, never()).crear(any());
    }

    @Test
    void losLimitesDeDiasYHorasSonValidos() {
        assertNotNull(servicio.agregarDetalle(1L, new DetallePlanillaCmd(10L, 0, 0, 0)));
        assertNotNull(servicio.agregarDetalle(1L, new DetallePlanillaCmd(11L, 31, 32767, 32767)));
    }

    @Test
    void noSeAgregaDetalleAUnaPlanillaPagadaNiInexistenteNiAUnEmpleadoInexistente() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(2L, new DetallePlanillaCmd(10L, null, null, null)));
        when(planillaDAO.buscarParaActualizar(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class,
                () -> servicio.agregarDetalle(99L, new DetallePlanillaCmd(10L, null, null, null)));
        when(empleadoDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class,
                () -> servicio.agregarDetalle(1L, new DetallePlanillaCmd(99L, null, null, null)));
        verify(detalleDAO, never()).crear(any());
    }

    @Test
    void generarDetallesCreaUnoPorCadaEmpleadoActivoQueAunNoEstaEnLaPlanilla() {
        when(empleadoDAO.findActivos()).thenReturn(List.of(ana, luis));
        when(detalleDAO.existePorPlanillaYEmpleado(1L, 10L)).thenReturn(true);

        List<PlanillaDetalle> creados = servicio.generarDetalles(1L);

        assertEquals(1, creados.size());
        assertSame(luis, creados.get(0).getEmpleado());
        assertEquals(bd("600"), creados.get(0).getSueldoBase());
        assertEquals((short) 30, creados.get(0).getDias());
        verify(detalleDAO, times(1)).crear(any());
    }

    @Test
    void generarDetallesEnUnaPlanillaPagadaSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> servicio.generarDetalles(2L));
        verify(detalleDAO, never()).crear(any());
    }

    @Test
    void modificarYEliminarDetalleSoloEnPlanillaPendiente() {
        PlanillaDetalle d = detalle(5L, pendiente, ana, 30, 0, 0);
        PlanillaDetalle dPagado = detalle(6L, pagada, ana, 30, 0, 0);
        when(detalleDAO.buscarPorId(5L)).thenReturn(Optional.of(d));
        when(detalleDAO.buscarPorId(6L)).thenReturn(Optional.of(dPagado));

        PlanillaDetalle m = servicio.modificarDetalle(5L, new DetallePlanillaCmd(null, 20, 3, 1));
        assertEquals((short) 20, m.getDias());
        assertEquals((short) 3, m.getHorasxDiur());
        assertEquals((short) 1, m.getHorasxNoct());
        verify(detalleDAO).modificar(d);
        servicio.eliminarDetalle(5L);
        verify(detalleDAO).eliminar(5L);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificarDetalle(6L, new DetallePlanillaCmd(null, 10, 0, 0)));
        assertThrows(ReglaNegocioException.class, () -> servicio.eliminarDetalle(6L));
        verify(detalleDAO, never()).eliminar(6L);
    }

    @Test
    void modificarDetalleNoPuedeCambiarDeEmpleadoYValidaRangos() {
        PlanillaDetalle d = detalle(5L, pendiente, ana, 30, 0, 0);
        when(detalleDAO.buscarPorId(5L)).thenReturn(Optional.of(d));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificarDetalle(5L, new DetallePlanillaCmd(11L, 20, 0, 0)));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificarDetalle(5L, new DetallePlanillaCmd(null, 40, 0, 0)));
        assertEquals((short) 30, d.getDias());
        when(detalleDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class,
                () -> servicio.modificarDetalle(99L, new DetallePlanillaCmd(null, 20, 0, 0)));
    }

    // -------------------------------------------------------------------------------------- calcular

    @Test
    void calcularUsaElSueldoCongeladoDelDetalleNoElActualDelEmpleado() {
        PlanillaDetalle d = detalle(5L, pendiente, ana, 30, 0, 0);
        d.setSueldoBase(bd("600"));
        ana.setSalario(bd("5000")); // el salario cambio despues de crear el detalle
        when(detalleDAO.buscarPorPlanilla(1L)).thenReturn(List.of(d));

        ResumenPlanilla r = servicio.calcular(1L);

        assertEquals(bd("600.00"), r.lineas().get(0).resultado().devengado());
        assertEquals(bd("514.18"), r.lineas().get(0).resultado().liquido());
    }

    @Test
    void calcularTotalizaLasLineas() {
        when(detalleDAO.buscarPorPlanilla(1L)).thenReturn(List.of(
                detalle(5L, pendiente, ana, 30, 0, 0), detalle(6L, pendiente, luis, 30, 0, 0)));

        ResumenPlanilla r = servicio.calcular(1L);

        assertEquals(2, r.lineas().size());
        // ana 1000: isss 30, afp 72.50, renta 60.45, liquido 837.05 ; luis 600: 18, 43.50, 24.32, 514.18
        assertEquals(bd("1600.00"), r.totalDevengado());
        assertEquals(bd("48.00"), r.totalIsss());
        assertEquals(bd("116.00"), r.totalAfp());
        assertEquals(bd("84.77"), r.totalRenta());
        assertEquals(bd("248.77"), r.totalDeducciones());
        assertEquals(bd("1351.23"), r.totalLiquido());
    }

    // ----------------------------------------------------------------------------------------- pagar

    private void dosDetalles() {
        when(detalleDAO.buscarPorPlanilla(1L)).thenReturn(List.of(
                detalle(5L, pendiente, ana, 30, 0, 0), detalle(6L, pendiente, luis, 30, 0, 0)));
    }

    @Test
    void pagarPasaAPagadoYGeneraLaPartidaDeEgresoCuadrada() {
        dosDetalles();

        PagoPlanilla r = servicio.pagar(1L);

        assertEquals(EstadoPlanilla.Pagado, pendiente.getEstado());
        verify(planillaDAO).modificar(pendiente);
        ArgumentCaptor<List<LineaAsiento>> lineas = lineasCaptor();
        verify(partidaService).registrar(eq(TipoPartida.Egreso), eq(pago.toLocalDate()), eq("Pago de planilla #1"),
                isNull(), isNull(), isNull(), lineas.capture());
        assertEquals(List.of(
                LineaAsiento.cargo(sueldos, bd("1600.00")),
                LineaAsiento.abono(retIsss, bd("48.00")),
                LineaAsiento.abono(retAfp, bd("116.00")),
                LineaAsiento.abono(retRenta, bd("84.77")),
                LineaAsiento.abono(bancos, bd("1351.23"))), lineas.getValue());
        assertEquals(77L, r.partida().getId());
        assertSame(pendiente, r.planilla());
        assertEquals(bd("1351.23"), r.resumen().totalLiquido());
    }

    @Test
    void noSePagaDosVecesLaMismaPlanilla() {
        dosDetalles();
        servicio.pagar(1L);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.pagar(1L));

        assertTrue(ex.getMessage().contains("pagada"), ex.getMessage());
        verify(partidaService, times(1)).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void unaPlanillaYaPagadaNoGeneraOtraPartida() {
        assertThrows(ReglaNegocioException.class, () -> servicio.pagar(2L));

        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
        verify(planillaDAO, never()).modificar(any());
    }

    @Test
    void pagarSinDetallesOSinImporteSeRechazaYDejaLaPlanillaPendiente() {
        when(detalleDAO.buscarPorPlanilla(1L)).thenReturn(List.of());
        assertThrows(ReglaNegocioException.class, () -> servicio.pagar(1L));

        when(detalleDAO.buscarPorPlanilla(1L)).thenReturn(List.of(detalle(5L, pendiente, ana, 0, 0, 0)));
        assertThrows(ReglaNegocioException.class, () -> servicio.pagar(1L));

        assertEquals(EstadoPlanilla.Pendiente, pendiente.getEstado());
        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
        verify(planillaDAO, never()).modificar(any());
    }

    @Test
    void siUnaCuentaConfiguradaFallaLaPlanillaSigueSiendoPendiente() {
        dosDetalles();
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_BANCOS))
                .thenThrow(new ReglaNegocioException("La cuenta configurada en 'cuenta.bancos' no existe"));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.pagar(1L));

        assertTrue(ex.getMessage().contains("cuenta.bancos"), ex.getMessage());
        assertEquals(EstadoPlanilla.Pendiente, pendiente.getEstado());
        verify(planillaDAO, never()).modificar(any());
        verify(partidaService, never()).registrar(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void pagarBloqueaLaFilaDeLaPlanillaParaEvitarDobleCobro() {
        dosDetalles();

        servicio.pagar(1L);

        verify(planillaDAO).buscarParaActualizar(1L);
    }

    // ------------------------------------------------------------------------------------- consultas

    @Test
    void consultasDelegan() {
        when(planillaDAO.findRange(0, 10)).thenReturn(List.of(pendiente));
        when(detalleDAO.buscarPorPlanilla(1L)).thenReturn(List.of());

        assertSame(pendiente, servicio.buscarPorId(1L));
        assertEquals(List.of(pendiente), servicio.listar(0, 10));
        assertEquals(List.of(), servicio.detalles(1L));
        when(planillaDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.buscarPorId(99L));
    }
}
