package com.sistemacontable.control.service;

import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.ExistenciasDAO;
import com.sistemacontable.control.KardexDetalleDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.VentaDetalleDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LoteCmd;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Producto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Lotes: producto (existente) y numero de lote obligatorios. Un lote no cambia de producto al modificarlo. No se
 * elimina un lote con existencias, compras, ventas o movimientos de kardex.
 */
@Stateless
@LocalBean
public class LoteService {

    @Inject
    private LoteDAO loteDAO;
    @Inject
    private ProductoDAO productoDAO;
    @Inject
    private ExistenciasDAO existenciasDAO;
    @Inject
    private CompraDetalleDAO compraDetalleDAO;
    @Inject
    private VentaDetalleDAO ventaDetalleDAO;
    @Inject
    private KardexDetalleDAO kardexDetalleDAO;

    public Lote crear(LoteCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del lote son obligatorios");
        }
        Long productoId = Validaciones.requerir(cmd.productoId(), "producto");
        String numLote = Validaciones.texto(cmd.numLote(), "numero de lote");
        Producto producto = productoDAO.buscarPorId(productoId)
                .orElseThrow(() -> new NoEncontradoException("Producto", productoId));
        Lote lote = new Lote();
        lote.setProducto(producto);
        lote.setNumLote(numLote);
        lote.setFechCaducidad(cmd.fechCaducidad());
        loteDAO.crear(lote);
        loteDAO.flush();
        return lote;
    }

    /** Modifica numero y caducidad; {@code productoId} debe ser nulo o el del lote. */
    public Lote modificar(Long id, LoteCmd cmd) {
        Lote lote = buscarPorId(id);
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del lote son obligatorios");
        }
        if (cmd.productoId() != null && !cmd.productoId().equals(lote.getProducto().getId())) {
            throw new ReglaNegocioException("Un lote no puede cambiar de producto");
        }
        lote.setNumLote(Validaciones.texto(cmd.numLote(), "numero de lote"));
        lote.setFechCaducidad(cmd.fechCaducidad());
        loteDAO.modificar(lote);
        return lote;
    }

    public void eliminar(Long id) {
        Lote lote = buscarPorId(id);
        if (existenciasDAO.existePorReferencia("lote", lote)
                || compraDetalleDAO.existePorReferencia("lote", lote)
                || ventaDetalleDAO.existePorReferencia("lote", lote)
                || kardexDetalleDAO.existePorReferencia("lote", lote)) {
            throw new ReglaNegocioException("No se puede eliminar el lote " + id
                    + " porque tiene existencias, compras, ventas o movimientos de kardex");
        }
        loteDAO.eliminar(id);
    }

    public Lote buscarPorId(Long id) {
        return loteDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Lote", id));
    }

    /** Lotes de un producto, ordenados por numero de lote. */
    public List<Lote> porProducto(Long productoId) {
        return loteDAO.findByProducto(productoId);
    }
}
