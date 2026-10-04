package com.sistemacontable.control.service;

import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.TipoProductoDAO;
import com.sistemacontable.control.UnidadMedidaDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.ProductoCmd;
import com.sistemacontable.entity.Producto;
import com.sistemacontable.entity.TipoProducto;
import com.sistemacontable.entity.UnidadMedida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/** Productos: nombre, unidad de medida y tipo obligatorios (deben existir); no se elimina uno con lotes. */
@Stateless
@LocalBean
public class ProductoService {

    @Inject
    private ProductoDAO productoDAO;
    @Inject
    private UnidadMedidaDAO unidadMedidaDAO;
    @Inject
    private TipoProductoDAO tipoProductoDAO;
    @Inject
    private LoteDAO loteDAO;

    public Producto crear(ProductoCmd cmd) {
        Producto producto = new Producto();
        aplicar(producto, cmd);
        productoDAO.crear(producto);
        productoDAO.flush();
        return producto;
    }

    public Producto modificar(Long id, ProductoCmd cmd) {
        Producto producto = buscarPorId(id);
        aplicar(producto, cmd);
        productoDAO.modificar(producto);
        return producto;
    }

    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        if (loteDAO.existePorReferencia("producto", producto)) {
            throw new ReglaNegocioException("No se puede eliminar el producto " + id + " porque tiene lotes");
        }
        productoDAO.eliminar(id);
    }

    public Producto buscarPorId(Long id) {
        return productoDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Producto", id));
    }

    public List<Producto> listar(int first, int max) {
        return productoDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return productoDAO.count();
    }

    private void aplicar(Producto producto, ProductoCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del producto son obligatorios");
        }
        String nombre = Validaciones.texto(cmd.nombre(), "nombre");
        Long unidadId = Validaciones.requerir(cmd.unidadMedidaId(), "unidad de medida");
        Long tipoId = Validaciones.requerir(cmd.tipoId(), "tipo de producto");
        UnidadMedida unidad = unidadMedidaDAO.buscarPorId(unidadId)
                .orElseThrow(() -> new NoEncontradoException("Unidad de medida", unidadId));
        TipoProducto tipo = tipoProductoDAO.buscarPorId(tipoId)
                .orElseThrow(() -> new NoEncontradoException("Tipo de producto", tipoId));
        producto.setNombre(nombre);
        producto.setUnidadMedida(unidad);
        producto.setTipo(tipo);
        producto.setDescripcion(Validaciones.textoOpcional(cmd.descripcion()));
    }
}
