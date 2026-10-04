package com.sistemacontable.control.service;

import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.ExistenciasDAO;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Existencias;
import com.sistemacontable.entity.Lote;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Movimientos de stock y valoracion, usados por compra, venta, traslado y ajuste. Toda lectura previa a una
 * modificacion usa {@link ExistenciasDAO#findForUpdate} (SELECT ... FOR UPDATE), de modo que dos transacciones no
 * pisan la misma fila, y el stock nunca queda negativo.
 *
 * <p>Los servicios que consumen stock deben llamar primero a {@link #verificarDisponible(Map)} con TODAS sus
 * lineas acumuladas (si falla no se ha escrito nada) y solo despues a {@link #disminuir}/{@link #aumentar}. Debe
 * invocarse dentro de la transaccion JTA del servicio de negocio que lo usa.</p>
 */
@Stateless
@LocalBean
public class ExistenciasService {

    @Inject
    private ExistenciasDAO existenciasDAO;
    @Inject
    private CompraDetalleDAO compraDetalleDAO;

    /** Suma {@code cantidad} al stock del lote en el almacen, creando la fila si no existe. */
    public Existencias aumentar(Lote lote, Almacen almacen, BigDecimal cantidad) {
        exigirPositiva(cantidad);
        Optional<Existencias> actual = existenciasDAO.findForUpdate(lote.getId(), almacen.getId());
        if (actual.isEmpty()) {
            Existencias nueva = new Existencias();
            nueva.setLote(lote);
            nueva.setAlmacen(almacen);
            nueva.setCantidad(cantidad);
            existenciasDAO.crear(nueva);
            return nueva;
        }
        Existencias fila = actual.get();
        fila.setCantidad(fila.getCantidad().add(cantidad));
        existenciasDAO.modificar(fila);
        return fila;
    }

    /**
     * Resta {@code cantidad} del stock.
     *
     * @throws ReglaNegocioException si no hay fila o el stock no alcanza (el stock nunca queda negativo)
     */
    public Existencias disminuir(Lote lote, Almacen almacen, BigDecimal cantidad) {
        exigirPositiva(cantidad);
        Optional<Existencias> actual = existenciasDAO.findForUpdate(lote.getId(), almacen.getId());
        BigDecimal disponible = actual.map(Existencias::getCantidad).orElse(BigDecimal.ZERO);
        if (disponible.compareTo(cantidad) < 0) {
            throw stockInsuficiente(lote.getId(), almacen.getId(), disponible, cantidad);
        }
        Existencias fila = actual.get();
        fila.setCantidad(disponible.subtract(cantidad));
        existenciasDAO.modificar(fila);
        return fila;
    }

    /**
     * Comprueba, sin modificar nada, que hay stock para todas las claves (las cantidades ya vienen acumuladas por
     * clave). Las filas se bloquean en orden de clave. Una clave sin fila equivale a stock cero.
     *
     * @throws ReglaNegocioException ante la primera clave cuyo stock no alcanza
     */
    public void verificarDisponible(Map<ClaveStock, BigDecimal> requeridos) {
        for (Map.Entry<ClaveStock, BigDecimal> e : new TreeMap<>(requeridos).entrySet()) {
            ClaveStock clave = e.getKey();
            BigDecimal disponible = existenciasDAO.findForUpdate(clave.loteId(), clave.almacenId())
                    .map(Existencias::getCantidad).orElse(BigDecimal.ZERO);
            if (disponible.compareTo(e.getValue()) < 0) {
                throw stockInsuficiente(clave.loteId(), clave.almacenId(), disponible, e.getValue());
            }
        }
    }

    /**
     * Costo promedio ponderado del lote segun sus compras ({@code SUM(cantidad*precio)/SUM(cantidad)} de
     * {@code compra_detalle}), con 4 decimales; vacio si el lote nunca se compro.
     */
    public Optional<BigDecimal> costoPromedio(Long loteId) {
        return compraDetalleDAO.costoPromedioPonderado(loteId).map(Dinero::redondearCosto);
    }

    private static void exigirPositiva(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaNegocioException("La cantidad de un movimiento de stock debe ser mayor que cero");
        }
    }

    private static ReglaNegocioException stockInsuficiente(Long loteId, Long almacenId, BigDecimal disponible,
            BigDecimal requerido) {
        return new ReglaNegocioException("Stock insuficiente del lote " + loteId + " en el almacen " + almacenId
                + ": disponible " + disponible.toPlainString() + ", requerido " + requerido.toPlainString());
    }
}
