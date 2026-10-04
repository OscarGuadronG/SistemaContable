package com.sistemacontable.control.service;

import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.PlanillaDAO;
import com.sistemacontable.control.PlanillaDetalleDAO;
import com.sistemacontable.control.config.ConfigContable;
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
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Planillas de pago: creacion, detalle por empleado, calculo y pago con partida automatica.
 *
 * <p>Reglas: {@code fecha_inicio <= fecha_fin <= fecha_pago}; solo empleados {@code Activo} entran a la planilla
 * (al agregar o generar el detalle; despues, si el empleado cambia de estado, su detalle se conserva); un solo
 * detalle por empleado y planilla; dias 0..31 y horas extra &gt;= 0 (smallint); {@code sueldo_base} del detalle es
 * el salario del empleado al crearlo (congelado: un cambio posterior de salario no altera planillas existentes).
 * Una planilla {@code Pagado} es inmutable (ni ella ni sus detalles). Toda operacion que cambia una planilla la
 * bloquea con SELECT ... FOR UPDATE ({@link PlanillaDAO#buscarParaActualizar}), por eso dos pagos simultaneos no
 * pueden duplicar la partida.</p>
 *
 * <p>{@link #pagar}: {@code Pendiente -> Pagado} y partida {@code Egreso} (Sueldos al debe por el devengado total;
 * retenciones de ISSS, AFP y renta y Bancos por el liquido al haber). Decisiones conservadoras: el liquido se
 * paga desde la cuenta {@code cuenta.bancos}; no se paga una planilla sin detalles ni de devengado cero; no se
 * calculan aportes patronales. Las formulas estan en {@link CalculoPlanilla}.</p>
 *
 * <p>ATENCION: las tasas de ISSS/AFP, el techo, los recargos y la tabla de renta de {@code contabilidad.properties}
 * son valores de REFERENCIA (placeholders): deben verificarse contra la normativa vigente antes de usar este
 * servicio con planillas reales.</p>
 */
@Stateless
@LocalBean
public class PlanillaService {

    private static final int MAX_DIAS = 31;
    private static final int MAX_HORAS = Short.MAX_VALUE;

    @Inject
    private PlanillaDAO planillaDAO;
    @Inject
    private PlanillaDetalleDAO planillaDetalleDAO;
    @Inject
    private EmpleadoDAO empleadoDAO;
    @Inject
    private PartidaService partidaService;
    @Inject
    private ConfigContable config;

    // ------------------------------------------------------------------------------------ planilla

    /** Crea una planilla en estado Pendiente. */
    public Planilla crear(PlanillaCmd cmd) {
        validarFechas(cmd);
        Planilla planilla = new Planilla();
        aplicar(planilla, cmd);
        planilla.setEstado(EstadoPlanilla.Pendiente);
        planillaDAO.crear(planilla);
        planillaDAO.flush();
        return planilla;
    }

    /** Modifica fechas y descripcion de una planilla Pendiente. */
    public Planilla modificar(Long id, PlanillaCmd cmd) {
        Planilla planilla = bloquear(id);
        exigirPendiente(planilla);
        validarFechas(cmd);
        aplicar(planilla, cmd);
        planillaDAO.modificar(planilla);
        return planilla;
    }

    // ------------------------------------------------------------------------------------ detalles

    /** Agrega el detalle de un empleado Activo (una sola vez por planilla). */
    public PlanillaDetalle agregarDetalle(Long planillaId, DetallePlanillaCmd cmd) {
        Planilla planilla = bloquear(planillaId);
        exigirPendiente(planilla);
        if (cmd == null || cmd.empleadoId() == null) {
            throw new ReglaNegocioException("El detalle de planilla debe indicar el empleado");
        }
        int dias = entero(cmd.dias(), Math.min(config.getInt(ConfigContable.DIAS_MES), MAX_DIAS), "dias", MAX_DIAS);
        int horasDiurnas = entero(cmd.horasxDiur(), 0, "horas extra diurnas", MAX_HORAS);
        int horasNocturnas = entero(cmd.horasxNoct(), 0, "horas extra nocturnas", MAX_HORAS);
        Empleado empleado = empleadoDAO.buscarPorId(cmd.empleadoId())
                .orElseThrow(() -> new NoEncontradoException("Empleado", cmd.empleadoId()));
        return crearDetalle(planilla, empleado, dias, horasDiurnas, horasNocturnas);
    }

    /**
     * Crea el detalle por defecto (dias del mes, sin horas extra) para cada empleado Activo que aun no tiene detalle
     * en la planilla. Devuelve solo los detalles creados.
     */
    public List<PlanillaDetalle> generarDetalles(Long planillaId) {
        Planilla planilla = bloquear(planillaId);
        exigirPendiente(planilla);
        int dias = Math.min(config.getInt(ConfigContable.DIAS_MES), MAX_DIAS);
        List<PlanillaDetalle> creados = new ArrayList<>();
        for (Empleado empleado : empleadoDAO.findActivos()) {
            if (!planillaDetalleDAO.existePorPlanillaYEmpleado(planilla.getId(), empleado.getId())) {
                creados.add(crearDetalle(planilla, empleado, dias, 0, 0));
            }
        }
        return creados;
    }

    /** Cambia dias y horas de un detalle (valores nulos conservan el actual); el empleado no se puede cambiar. */
    public PlanillaDetalle modificarDetalle(Long detalleId, DetallePlanillaCmd cmd) {
        PlanillaDetalle detalle = buscarDetalle(detalleId);
        exigirPendiente(bloquear(detalle.getPlanilla().getId()));
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del detalle son obligatorios");
        }
        if (cmd.empleadoId() != null && !cmd.empleadoId().equals(detalle.getEmpleado().getId())) {
            throw new ReglaNegocioException("No se puede cambiar el empleado de un detalle de planilla");
        }
        int dias = entero(cmd.dias(), detalle.getDias(), "dias", MAX_DIAS);
        int horasDiurnas = entero(cmd.horasxDiur(), detalle.getHorasxDiur(), "horas extra diurnas", MAX_HORAS);
        int horasNocturnas = entero(cmd.horasxNoct(), detalle.getHorasxNoct(), "horas extra nocturnas", MAX_HORAS);
        detalle.setDias((short) dias);
        detalle.setHorasxDiur((short) horasDiurnas);
        detalle.setHorasxNoct((short) horasNocturnas);
        planillaDetalleDAO.modificar(detalle);
        return detalle;
    }

    /** Quita un detalle de una planilla Pendiente. */
    public void eliminarDetalle(Long detalleId) {
        PlanillaDetalle detalle = buscarDetalle(detalleId);
        exigirPendiente(bloquear(detalle.getPlanilla().getId()));
        planillaDetalleDAO.eliminar(detalleId);
    }

    // ------------------------------------------------------------------------------- calcular / pagar

    /** Calcula la planilla (sin escribir) con el sueldo base congelado de cada detalle. */
    public ResumenPlanilla calcular(Long planillaId) {
        return resumen(buscarPorId(planillaId));
    }

    /**
     * Paga la planilla: valida y calcula todo sin escribir; despues marca Pagado y registra la partida Egreso. Si
     * algo falla (planilla ya pagada, sin detalles, cuenta configurada invalida) la planilla sigue Pendiente.
     */
    public PagoPlanilla pagar(Long planillaId) {
        Planilla planilla = bloquear(planillaId);
        exigirPendiente(planilla);
        ResumenPlanilla resumen = resumen(planilla);
        if (resumen.lineas().isEmpty()) {
            throw new ReglaNegocioException("La planilla " + planillaId + " no tiene detalles que pagar");
        }
        if (resumen.totalDevengado().signum() <= 0) {
            throw new ReglaNegocioException("La planilla " + planillaId + " no tiene importe a pagar");
        }
        Cuenta sueldos = partidaService.cuentaConfigurada(ConfigContable.CUENTA_SUELDOS);
        Cuenta retIsss = partidaService.cuentaConfigurada(ConfigContable.CUENTA_RETENCIONES_ISSS);
        Cuenta retAfp = partidaService.cuentaConfigurada(ConfigContable.CUENTA_RETENCIONES_AFP);
        Cuenta retRenta = partidaService.cuentaConfigurada(ConfigContable.CUENTA_RETENCIONES_RENTA);
        Cuenta bancos = partidaService.cuentaConfigurada(ConfigContable.CUENTA_BANCOS);
        List<LineaAsiento> asiento = Asientos.planilla(sueldos, retIsss, retAfp, retRenta, bancos,
                resumen.totalDevengado(), resumen.totalIsss(), resumen.totalAfp(), resumen.totalRenta(),
                resumen.totalLiquido());
        ReglasPartida.validar(asiento);

        planilla.setEstado(EstadoPlanilla.Pagado);
        planillaDAO.modificar(planilla);
        Partida partida = partidaService.registrar(TipoPartida.Egreso, planilla.getFechaPago().toLocalDate(),
                "Pago de planilla #" + planilla.getId(), null, null, null, asiento);
        return new PagoPlanilla(planilla, partida, resumen);
    }

    // ------------------------------------------------------------------------------------ consultas

    public Planilla buscarPorId(Long id) {
        return planillaDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Planilla", id));
    }

    public List<Planilla> listar(int first, int max) {
        return planillaDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return planillaDAO.count();
    }

    public List<PlanillaDetalle> detalles(Long planillaId) {
        return planillaDetalleDAO.buscarPorPlanilla(planillaId);
    }

    // ------------------------------------------------------------------------------------- privados

    private ResumenPlanilla resumen(Planilla planilla) {
        ParametrosPlanilla parametros = ParametrosPlanilla.desde(config);
        List<LineaPlanilla> lineas = new ArrayList<>();
        BigDecimal devengado = BigDecimal.ZERO;
        BigDecimal isss = BigDecimal.ZERO;
        BigDecimal afp = BigDecimal.ZERO;
        BigDecimal renta = BigDecimal.ZERO;
        BigDecimal liquido = BigDecimal.ZERO;
        for (PlanillaDetalle d : planillaDetalleDAO.buscarPorPlanilla(planilla.getId())) {
            ResultadoPlanilla r = CalculoPlanilla.calcular(d.getSueldoBase(), d.getDias(), d.getHorasxDiur(),
                    d.getHorasxNoct(), parametros);
            lineas.add(new LineaPlanilla(d, r));
            devengado = devengado.add(r.devengado());
            isss = isss.add(r.isss());
            afp = afp.add(r.afp());
            renta = renta.add(r.renta());
            liquido = liquido.add(r.liquido());
        }
        return new ResumenPlanilla(planilla, lineas, Dinero.redondear(devengado), Dinero.redondear(isss),
                Dinero.redondear(afp), Dinero.redondear(renta), Dinero.redondear(isss.add(afp).add(renta)),
                Dinero.redondear(liquido));
    }

    private PlanillaDetalle crearDetalle(Planilla planilla, Empleado empleado, int dias, int horasDiurnas,
            int horasNocturnas) {
        if (empleado.getEstado() != EstadoEmpleado.Activo) {
            throw new ReglaNegocioException("Solo los empleados Activo entran a la planilla: el empleado "
                    + empleado.getId() + " esta " + empleado.getEstado());
        }
        if (planillaDetalleDAO.existePorPlanillaYEmpleado(planilla.getId(), empleado.getId())) {
            throw new ReglaNegocioException("El empleado " + empleado.getId() + " ya tiene un detalle en la planilla "
                    + planilla.getId());
        }
        PlanillaDetalle detalle = new PlanillaDetalle();
        detalle.setPlanilla(planilla);
        detalle.setEmpleado(empleado);
        detalle.setDias((short) dias);
        detalle.setHorasxDiur((short) horasDiurnas);
        detalle.setHorasxNoct((short) horasNocturnas);
        detalle.setSueldoBase(empleado.getSalario());
        planillaDetalleDAO.crear(detalle);
        return detalle;
    }

    private Planilla bloquear(Long id) {
        return planillaDAO.buscarParaActualizar(id).orElseThrow(() -> new NoEncontradoException("Planilla", id));
    }

    private PlanillaDetalle buscarDetalle(Long id) {
        return planillaDetalleDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Detalle de planilla", id));
    }

    private static void exigirPendiente(Planilla planilla) {
        if (planilla.getEstado() != EstadoPlanilla.Pendiente) {
            throw new ReglaNegocioException("La planilla " + planilla.getId()
                    + " ya fue pagada y no se puede modificar");
        }
    }

    private static void validarFechas(PlanillaCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos de la planilla son obligatorios");
        }
        if (cmd.fechaInicio() == null || cmd.fechaFin() == null || cmd.fechaPago() == null) {
            throw new ReglaNegocioException("Las fechas de inicio, fin y pago de la planilla son obligatorias");
        }
        OffsetDateTime fin = cmd.fechaFin();
        if (fin.isBefore(cmd.fechaInicio())) {
            throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la de inicio");
        }
        if (cmd.fechaPago().isBefore(fin)) {
            throw new ReglaNegocioException("La fecha de pago no puede ser anterior a la de fin");
        }
    }

    private static void aplicar(Planilla planilla, PlanillaCmd cmd) {
        planilla.setFechaInicio(cmd.fechaInicio());
        planilla.setFechaFin(cmd.fechaFin());
        planilla.setFechaPago(cmd.fechaPago());
        planilla.setDescripcion(cmd.descripcion() == null ? null : cmd.descripcion().trim());
    }

    /** Valor indicado (o {@code porDefecto} si es nulo) validado en 0..{@code maximo}. */
    private static int entero(Integer valor, int porDefecto, String que, int maximo) {
        int v = valor == null ? porDefecto : valor;
        if (v < 0 || v > maximo) {
            throw new ReglaNegocioException("El valor de " + que + " debe estar entre 0 y " + maximo + ", no " + v);
        }
        return v;
    }
}
