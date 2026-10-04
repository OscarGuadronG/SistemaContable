package com.sistemacontable.control.service;

import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.KardexDetalleDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Unico camino para escribir {@code kardex} y {@code kardex_detalle}. Valida las invariantes del tipo de movimiento
 * ({@link ReglasKardex}) antes de escribir. No toca el stock: eso es de {@link ExistenciasService}.
 *
 * <p>Convencion de {@code Ajuste}: la cantidad se guarda siempre positiva; entrada de ajuste = {@code destino} es
 * el almacen, salida de ajuste = {@code origen} es el almacen.</p>
 */
@Stateless
@LocalBean
public class KardexService {

    @Inject
    private KardexDAO kardexDAO;
    @Inject
    private KardexDetalleDAO kardexDetalleDAO;

    /** Registra un movimiento (cabecera + una fila por detalle) con fecha y hora actuales. */
    public MovimientoKardex registrar(TipoMovimiento tipo, Almacen origen, Almacen destino, Compra compra,
            Venta venta, String descripcion, List<DetalleKardex> detalles) {
        ReglasKardex.validar(tipo, origen, destino, compra, venta, detalles);

        Kardex kardex = new Kardex();
        kardex.setTipoMovimiento(tipo);
        kardex.setOrigen(origen);
        kardex.setDestino(destino);
        kardex.setCompra(compra);
        kardex.setVenta(venta);
        kardex.setDescripcion(descripcion);
        kardex.setFecha(OffsetDateTime.now());
        kardexDAO.crear(kardex);

        List<KardexDetalle> filas = new ArrayList<>();
        for (DetalleKardex d : detalles) {
            KardexDetalle fila = new KardexDetalle();
            fila.setKardex(kardex);
            fila.setLote(d.lote());
            fila.setCantidad(d.cantidad());
            fila.setPrecioCosto(d.precioCosto());
            kardexDetalleDAO.crear(fila);
            filas.add(fila);
        }
        kardexDAO.flush();
        return new MovimientoKardex(kardex, filas);
    }

    public Kardex buscarPorId(Long id) {
        return kardexDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Kardex", id));
    }

    /** Lineas de un movimiento existente. */
    public List<KardexDetalle> detalles(Long kardexId) {
        buscarPorId(kardexId);
        return kardexDetalleDAO.buscarPorKardex(kardexId);
    }

    public List<Kardex> listar(int first, int max) {
        return kardexDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return kardexDAO.count();
    }
}
