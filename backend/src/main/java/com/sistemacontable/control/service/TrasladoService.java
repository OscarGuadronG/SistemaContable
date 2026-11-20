package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaTrasladoCmd;
import com.sistemacontable.control.service.cmd.TrasladoCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.enums.FormaPago;
import com.sistemacontable.entity.enums.TipoPartida;
import com.sistemacontable.entity.enums.TipoMovimiento;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.enums.TipoMovimiento;

/**
 * Traslado de stock entre dos almacenes (distintos y habilitados) en una sola transaccion JTA. No genera partida.
 *
 * <p>Primero valida y comprueba el stock acumulado del origen para todas las lineas (si falla no se escribe nada);
 * luego, por cada linea, resta en el origen y suma en el destino, y registra UN {@code kardex} {@code Traslado}
 * (origen, destino) con una fila por linea. El {@code precio_costo} del kardex es informativo: el costo promedio
 * ponderado del lote, o cero si el lote nunca se compro.</p>
 */
@Stateless
@LocalBean
public class TrasladoService {

    @Inject
    private AlmacenDAO almacenDAO;
    @Inject
    private LoteDAO loteDAO;
    @Inject
    private ExistenciasService existenciasService;
    @Inject
    private KardexService kardexService;
    @Inject private PartidaService partidaService;
    @Inject private KardexDAO kardexDAO;
    @Inject private PartidaDAO partidaDAO;
    @Inject private PartidaDetalleDAO partidaDetalleDAO;

    public ResultadoTraslado registrar(TrasladoCmd cmd) {
        // ---- Fase 1: validar sin escribir nada
        if (cmd == null) {
            throw new ReglaNegocioException("El traslado es obligatorio");
        }
        if (cmd.almacenOrigenId() == null || cmd.almacenDestinoId() == null) {
            throw new ReglaNegocioException("El traslado debe indicar almacen de origen y de destino");
        }
        if (cmd.almacenOrigenId().equals(cmd.almacenDestinoId())) {
            throw new ReglaNegocioException("El almacen de origen y el de destino deben ser distintos");
        }
        if (cmd.lineas().isEmpty()) {
            throw new ReglaNegocioException("El traslado debe tener al menos una linea");
        }
        Almacen origen = habilitado(cmd.almacenOrigenId(), "de origen");
        Almacen destino = habilitado(cmd.almacenDestinoId(), "de destino");

        List<Lote> lotes = new ArrayList<>();
        List<BigDecimal> cantidades = new ArrayList<>();
        Map<ClaveStock, BigDecimal> requeridos = new LinkedHashMap<>();
        int numero = 0;
        for (LineaTrasladoCmd l : cmd.lineas()) {
            numero++;
            if (l == null || l.loteId() == null) {
                throw new ReglaNegocioException("La linea " + numero + " del traslado debe indicar el lote");
            }
            if (l.cantidad() == null || l.cantidad().signum() <= 0) {
                throw new ReglaNegocioException("La linea " + numero + " del traslado debe tener cantidad mayor que cero");
            }
            Lote lote = loteDAO.buscarPorId(l.loteId()).orElseThrow(() -> new NoEncontradoException("Lote", l.loteId()));
            lotes.add(lote);
            cantidades.add(l.cantidad());
            requeridos.merge(new ClaveStock(lote.getId(), origen.getId()), l.cantidad(), BigDecimal::add);
        }
        existenciasService.verificarDisponible(requeridos);

        BigDecimal gastos = Dinero.redondear(cmd.gastosAdicionales() == null ? BigDecimal.ZERO : cmd.gastosAdicionales());
        if (gastos.signum() < 0) throw new ReglaNegocioException("Los gastos adicionales no pueden ser negativos");
        if (gastos.signum() > 0 && cmd.formaPagoGastos() == null) throw new ReglaNegocioException("Indique la forma de pago de los gastos");
        if (gastos.signum() > 0 && cmd.formaPagoGastos() == FormaPago.Credito) throw new ReglaNegocioException("Los gastos de traslado se pagan al contado o con cheque");
        List<BigDecimal> costos = new ArrayList<>();
        BigDecimal valorOrigen = BigDecimal.ZERO;
        for (int i = 0; i < lotes.size(); i++) {
            Lote lote = lotes.get(i);
            BigDecimal costo = existenciasService.costoEntradas(lote.getId(), origen.getId()).orElseThrow(() -> new ReglaNegocioException("No se conoce el costo del lote " + lote.getId() + " en el almacén de origen"));
            costos.add(costo); valorOrigen = valorOrigen.add(costo.multiply(cantidades.get(i)));
        }

        List<DetalleKardex> detalles = new ArrayList<>();
        BigDecimal gastoAplicado = BigDecimal.ZERO, costoTotal = BigDecimal.ZERO;
        for (int i = 0; i < lotes.size(); i++) {
            BigDecimal denominator = valorOrigen.signum() == 0 ? cantidades.stream().reduce(BigDecimal.ZERO, BigDecimal::add) : valorOrigen;
            BigDecimal peso = valorOrigen.signum() == 0 ? cantidades.get(i) : costos.get(i).multiply(cantidades.get(i));
            BigDecimal asignado = i == lotes.size() - 1 ? gastos.subtract(gastoAplicado)
                    : denominator.signum() == 0 ? BigDecimal.ZERO : Dinero.redondear(gastos.multiply(peso).divide(denominator, 10, java.math.RoundingMode.HALF_UP));
            gastoAplicado = gastoAplicado.add(asignado);
            BigDecimal costoDestino = Dinero.redondearCosto(costos.get(i).add(asignado.divide(cantidades.get(i), 10, java.math.RoundingMode.HALF_UP)));
            detalles.add(new DetalleKardex(lotes.get(i), cantidades.get(i), costoDestino));
            costoTotal = costoTotal.add(costoDestino.multiply(cantidades.get(i)));
        }

        // ---- Fase 2: escribir
        for (int i = 0; i < lotes.size(); i++) {
            existenciasService.disminuir(lotes.get(i), origen, cantidades.get(i));
            existenciasService.aumentar(lotes.get(i), destino, cantidades.get(i));
        }
        String descripcion = cmd.descripcion() != null && !cmd.descripcion().isBlank() ? cmd.descripcion().trim()
                : "Traslado entre almacenes";
        LocalDate fecha = cmd.fecha() == null ? LocalDate.now() : cmd.fecha();
        MovimientoKardex movimiento = kardexService.registrar(TipoMovimiento.Traslado, origen, destino, null, null, descripcion, fecha.atStartOfDay().atOffset(java.time.ZoneOffset.UTC), detalles);
        movimiento.kardex().setGastosAdicionales(gastos);
        Partida partida = null;
        if (gastos.signum() > 0) {
            Cuenta inventario = partidaService.cuentaConfigurada("cuenta.inventario");
            Cuenta salida = partidaService.cuentaConfigurada(cmd.formaPagoGastos() == FormaPago.Cheque ? "cuenta.bancos" : "cuenta.caja");
            List<LineaAsiento> asiento = List.of(LineaAsiento.cargo(inventario, gastos), LineaAsiento.abono(salida, gastos));
            partida = partidaService.registrar(TipoPartida.Diario, fecha, descripcion, null, null, null, asiento);
            partida.setIdKardex(movimiento.kardex().getId());
        }
        return new ResultadoTraslado(movimiento, partida, partida == null ? List.of() : partidaDetalleDAO.buscarPorPartida(partida.getId()), gastos, Dinero.redondear(costoTotal));
    }

    public List<ResultadoTraslado> listar(LocalDate desde, LocalDate hasta, Long almacenId, int first, int max) {
        validarRango(desde,hasta);
        return kardexDAO.findTraslados(desde,hasta,almacenId,first,max).stream().map(this::resultado).toList();
    }
    public long contar(LocalDate desde, LocalDate hasta, Long almacenId) { validarRango(desde,hasta); return kardexDAO.contarTraslados(desde,hasta,almacenId); }
    public ResultadoTraslado buscar(Long id) {
        Kardex k = kardexDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Kardex", id));
        if (k.getTipoMovimiento() != TipoMovimiento.Traslado) throw new NoEncontradoException("Traslado", id);
        return resultado(k);
    }
    private ResultadoTraslado resultado(Kardex k) {
        var lineas = kardexService.detalles(k.getId());
        var movimiento = new MovimientoKardex(k, lineas);
        BigDecimal valor = lineas.stream().map(d -> d.getCantidad().multiply(d.getPrecioCosto())).reduce(BigDecimal.ZERO, BigDecimal::add);
        Partida partida = partidaDAO.findByKardex(k.getId()).orElse(null);
        return new ResultadoTraslado(movimiento, partida, partida == null ? List.of() : partidaDetalleDAO.buscarPorPartida(partida.getId()), k.getGastosAdicionales(), Dinero.redondear(valor));
    }
    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
    }

    private Almacen habilitado(Long id, String rol) {
        Almacen almacen = almacenDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Almacen", id));
        if (!Boolean.TRUE.equals(almacen.getEstado())) {
            throw new ReglaNegocioException("El almacen " + rol + " " + id + " esta deshabilitado");
        }
        return almacen;
    }
}
