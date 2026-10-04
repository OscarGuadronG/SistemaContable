package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.AjusteCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ajuste de inventario de un lote en un almacen habilitado, en una sola transaccion JTA.
 *
 * <p>La cantidad del comando lleva el signo: positiva suma (entrada de ajuste) y negativa resta (salida de ajuste,
 * con verificacion de stock previa a escribir). El {@code kardex} es de tipo {@code Ajuste} y guarda la cantidad
 * SIEMPRE positiva; el signo lo da la posicion del almacen: entrada de ajuste -&gt; {@code destino} = almacen, salida
 * de ajuste -&gt; {@code origen} = almacen. El motivo (obligatorio) va en la descripcion del kardex.</p>
 *
 * <p>Si {@code ajuste.genera.partida=true}, genera ademas una partida {@code Ajuste}: la entrada debita Inventario y
 * acredita la cuenta de ajuste ({@code cuenta.ajuste.inventario}); la salida hace lo contrario; el valor es
 * {@code cantidad x costo promedio ponderado}. Con la partida activa el lote debe tener costo conocido (si no, se
 * rechaza); sin partida, un lote sin compras se ajusta con costo cero en el kardex.</p>
 */
@Stateless
@LocalBean
public class AjusteService {

    @Inject
    private AlmacenDAO almacenDAO;
    @Inject
    private LoteDAO loteDAO;
    @Inject
    private ExistenciasService existenciasService;
    @Inject
    private KardexService kardexService;
    @Inject
    private PartidaService partidaService;
    @Inject
    private ConfigContable config;

    public ResultadoAjuste registrar(AjusteCmd cmd) {
        // ---- Fase 1: validar y calcular sin escribir nada
        if (cmd == null) {
            throw new ReglaNegocioException("El ajuste es obligatorio");
        }
        if (cmd.almacenId() == null || cmd.loteId() == null) {
            throw new ReglaNegocioException("El ajuste debe indicar almacen y lote");
        }
        if (cmd.cantidad() == null || cmd.cantidad().signum() == 0) {
            throw new ReglaNegocioException("La cantidad del ajuste debe ser distinta de cero (con signo)");
        }
        if (cmd.motivo() == null || cmd.motivo().isBlank()) {
            throw new ReglaNegocioException("El motivo del ajuste es obligatorio");
        }
        Almacen almacen = almacenDAO.buscarPorId(cmd.almacenId())
                .orElseThrow(() -> new NoEncontradoException("Almacen", cmd.almacenId()));
        if (!Boolean.TRUE.equals(almacen.getEstado())) {
            throw new ReglaNegocioException("El almacen " + cmd.almacenId() + " esta deshabilitado");
        }
        Lote lote = loteDAO.buscarPorId(cmd.loteId()).orElseThrow(() -> new NoEncontradoException("Lote", cmd.loteId()));

        boolean entrada = cmd.cantidad().signum() > 0;
        BigDecimal cantidad = cmd.cantidad().abs();
        if (!entrada) {
            existenciasService.verificarDisponible(Map.of(new ClaveStock(lote.getId(), almacen.getId()), cantidad));
        }
        Optional<BigDecimal> costo = existenciasService.costoPromedio(lote.getId());

        List<LineaAsiento> asiento = List.of();
        if (config.getBoolean(ConfigContable.AJUSTE_GENERA_PARTIDA)) {
            BigDecimal costoConocido = costo.orElseThrow(() -> new ReglaNegocioException(
                    "No se puede contabilizar el ajuste: el lote " + lote.getId() + " no tiene costo (sin compras)"));
            Cuenta inventario = partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO);
            Cuenta cuentaAjuste = partidaService.cuentaConfigurada(ConfigContable.CUENTA_AJUSTE_INVENTARIO);
            asiento = Asientos.ajuste(inventario, cuentaAjuste, Dinero.multiplicar(cantidad, costoConocido), entrada);
            if (!asiento.isEmpty()) {
                ReglasPartida.validar(asiento);
            }
        }

        // ---- Fase 2: escribir
        String motivo = cmd.motivo().trim();
        if (entrada) {
            existenciasService.aumentar(lote, almacen, cantidad);
        } else {
            existenciasService.disminuir(lote, almacen, cantidad);
        }
        MovimientoKardex movimiento = kardexService.registrar(TipoMovimiento.Ajuste, entrada ? null : almacen,
                entrada ? almacen : null, null, null, motivo,
                List.of(new DetalleKardex(lote, cantidad, costo.orElse(BigDecimal.ZERO))));
        Partida partida = asiento.isEmpty() ? null : partidaService.registrar(TipoPartida.Ajuste, LocalDate.now(),
                "Ajuste de inventario: " + motivo, null, null, null, asiento);
        return new ResultadoAjuste(movimiento, partida);
    }
}
