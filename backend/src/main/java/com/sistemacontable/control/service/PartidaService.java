package com.sistemacontable.control.service;

import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.CuentaDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.TerceroDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaPartidaCmd;
import com.sistemacontable.control.service.cmd.PartidaCmd;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Tercero;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro y consulta de partidas contables. Es el unico camino para escribir partidas: los demas servicios
 * (compra, venta, planilla, ajuste) lo usan para contabilizar.
 *
 * <p>Reglas: ver {@link ReglasPartida}; ademas {@code id_compra} e {@code id_venta} no pueden ir a la vez. Una
 * partida <b>no se edita ni se borra</b>: solo se crea y se consulta (una correccion se hace con otra partida).
 * Los importes se redondean a 2 decimales ({@link Dinero}) antes de validar el cuadre. Todas las validaciones
 * ocurren antes de escribir nada. Los errores de validacion son {@link ReglaNegocioException} (incluye campos
 * obligatorios vacios); un id inexistente es {@link NoEncontradoException}.</p>
 *
 * <p>Los codigos de cuenta de los asientos automaticos salen solo de {@link ConfigContable}
 * ({@link #cuentaConfigurada(String)}).</p>
 */
@Stateless
@LocalBean
public class PartidaService {

    @Inject
    private PartidaDAO partidaDAO;
    @Inject
    private PartidaDetalleDAO partidaDetalleDAO;
    @Inject
    private CuentaDAO cuentaDAO;
    @Inject
    private CompraDAO compraDAO;
    @Inject
    private VentaDAO ventaDAO;
    @Inject
    private TerceroDAO terceroDAO;
    @Inject
    private ConfigContable config;

    /** Crea una partida a partir de una orden externa (todo por ids). */
    public Partida crear(PartidaCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("La partida es obligatoria");
        }
        if (cmd.compraId() != null && cmd.ventaId() != null) {
            throw new ReglaNegocioException("Una partida no puede estar ligada a una compra y a una venta a la vez");
        }
        requerirDatos(cmd.fecha(), cmd.concepto(), cmd.tipo());
        Compra compra = cmd.compraId() == null ? null : compraDAO.buscarPorId(cmd.compraId())
                .orElseThrow(() -> new NoEncontradoException("Compra", cmd.compraId()));
        Venta venta = cmd.ventaId() == null ? null : ventaDAO.buscarPorId(cmd.ventaId())
                .orElseThrow(() -> new NoEncontradoException("Venta", cmd.ventaId()));
        Tercero tercero = cmd.terceroId() == null ? null : terceroDAO.buscarPorId(cmd.terceroId())
                .orElseThrow(() -> new NoEncontradoException("Tercero", cmd.terceroId()));
        List<LineaAsiento> lineas = new ArrayList<>();
        for (LineaPartidaCmd l : cmd.lineas()) {
            if (l == null || l.cuentaId() == null) {
                throw new ReglaNegocioException("Cada linea de la partida debe indicar su cuenta");
            }
            Cuenta cuenta = cuentaDAO.buscarPorId(l.cuentaId())
                    .orElseThrow(() -> new NoEncontradoException("Cuenta", l.cuentaId()));
            lineas.add(new LineaAsiento(cuenta, aImporte(l.cargo()), aImporte(l.abono())));
        }
        return registrar(cmd.tipo(), cmd.fecha(), cmd.concepto(), compra, venta, tercero, lineas);
    }

    /**
     * Registra una partida con las entidades ya resueltas (uso entre servicios). Valida todo antes de escribir.
     */
    public Partida registrar(TipoPartida tipo, LocalDate fecha, String concepto, Compra compra, Venta venta,
            Tercero tercero, List<LineaAsiento> lineas) {
        requerirDatos(fecha, concepto, tipo);
        if (compra != null && venta != null) {
            throw new ReglaNegocioException("Una partida no puede estar ligada a una compra y a una venta a la vez");
        }
        List<LineaAsiento> normalizadas = normalizar(lineas);
        ReglasPartida.validar(normalizadas);

        Partida partida = new Partida();
        partida.setFecha(fecha);
        partida.setConcepto(concepto.trim());
        partida.setTipo(tipo);
        partida.setCompra(compra);
        partida.setVenta(venta);
        partida.setTercero(tercero);
        partidaDAO.crear(partida);
        for (LineaAsiento l : normalizadas) {
            PartidaDetalle detalle = new PartidaDetalle();
            detalle.setPartida(partida);
            detalle.setCuenta(l.cuenta());
            detalle.setCargo(l.cargo());
            detalle.setAbono(l.abono());
            partidaDetalleDAO.crear(detalle);
        }
        partidaDAO.flush();
        return partida;
    }

    /**
     * Cuenta cuyo codigo esta configurado bajo {@code claveConfig}. Debe existir y estar activa e imputable; si no,
     * falla con {@link ReglaNegocioException} que nombra la clave de configuracion.
     */
    public Cuenta cuentaConfigurada(String claveConfig) {
        int codigo = config.getInt(claveConfig);
        Cuenta cuenta = cuentaDAO.findByCodigo(codigo).orElseThrow(() -> new ReglaNegocioException(
                "La cuenta configurada en '" + claveConfig + "' (codigo " + codigo + ") no existe en el catalogo"));
        ReglasPartida.validarCuentaImputable(cuenta, "configurada en '" + claveConfig + "'");
        return cuenta;
    }

    public Partida buscarPorId(Long id) {
        return partidaDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Partida", id));
    }

    public List<Partida> listar(int first, int max) {
        return partidaDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return partidaDAO.count();
    }

    /** Lineas de una partida existente. */
    public List<PartidaDetalle> lineas(Long partidaId) {
        buscarPorId(partidaId);
        return partidaDetalleDAO.buscarPorPartida(partidaId);
    }

    /** Partidas con fecha en [desde, hasta], ambos incluidos. */
    public List<Partida> buscarPorRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("El rango de fechas es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final");
        }
        return partidaDAO.findByRango(desde, hasta);
    }

    private static void requerirDatos(LocalDate fecha, String concepto, TipoPartida tipo) {
        if (fecha == null) {
            throw new ReglaNegocioException("La fecha de la partida es obligatoria");
        }
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaNegocioException("El concepto de la partida es obligatorio");
        }
        if (tipo == null) {
            throw new ReglaNegocioException("El tipo de la partida es obligatorio");
        }
    }

    private static BigDecimal aImporte(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    /** Redondea los importes a 2 decimales; deja pasar lineas invalidas para que {@link ReglasPartida} las rechace. */
    private static List<LineaAsiento> normalizar(List<LineaAsiento> lineas) {
        if (lineas == null) {
            return null;
        }
        List<LineaAsiento> resultado = new ArrayList<>();
        for (LineaAsiento l : lineas) {
            if (l == null || l.cargo() == null || l.abono() == null) {
                resultado.add(l);
            } else {
                resultado.add(new LineaAsiento(l.cuenta(), Dinero.redondear(l.cargo()), Dinero.redondear(l.abono())));
            }
        }
        return resultado;
    }
}
