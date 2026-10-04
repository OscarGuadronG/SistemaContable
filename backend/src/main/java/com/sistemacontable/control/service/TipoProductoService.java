package com.sistemacontable.control.service;

import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.TipoProductoDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.TipoProductoCmd;
import com.sistemacontable.entity.TipoProducto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Tipos de producto en arbol: nombre obligatorio, padre opcional, sin ciclos. No se elimina un tipo con hijos ni con
 * productos.
 */
@Stateless
@LocalBean
public class TipoProductoService {

    @Inject
    private TipoProductoDAO tipoProductoDAO;
    @Inject
    private ProductoDAO productoDAO;

    public TipoProducto crear(TipoProductoCmd cmd) {
        TipoProducto tipo = new TipoProducto();
        aplicar(tipo, cmd);
        tipoProductoDAO.crear(tipo);
        tipoProductoDAO.flush();
        return tipo;
    }

    public TipoProducto modificar(Long id, TipoProductoCmd cmd) {
        TipoProducto tipo = buscarPorId(id);
        aplicar(tipo, cmd);
        tipoProductoDAO.modificar(tipo);
        return tipo;
    }

    public void eliminar(Long id) {
        TipoProducto tipo = buscarPorId(id);
        if (!tipoProductoDAO.findHijos(tipo).isEmpty()) {
            throw new ReglaNegocioException("No se puede eliminar el tipo de producto " + id + " porque tiene hijos");
        }
        if (productoDAO.existePorReferencia("tipo", tipo)) {
            throw new ReglaNegocioException("No se puede eliminar el tipo de producto " + id
                    + " porque tiene productos");
        }
        tipoProductoDAO.eliminar(id);
    }

    public TipoProducto buscarPorId(Long id) {
        return tipoProductoDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Tipo de producto", id));
    }

    public List<TipoProducto> raices() {
        return tipoProductoDAO.findRaices();
    }

    public List<TipoProducto> hijos(Long id) {
        return tipoProductoDAO.findHijos(buscarPorId(id));
    }

    public List<TipoProducto> listar(int first, int max) {
        return tipoProductoDAO.findRange(first, max);
    }

    private void aplicar(TipoProducto tipo, TipoProductoCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del tipo de producto son obligatorios");
        }
        String nombre = Validaciones.texto(cmd.nombre(), "nombre");
        TipoProducto padre = cmd.padreId() == null ? null : buscarPorId(cmd.padreId());
        if (padre != null && Jerarquia.hayCiclo(tipo, padre, TipoProducto::getPadre)) {
            throw new ReglaNegocioException("El tipo de producto no puede colgar de " + padre.getId()
                    + ": se formaria un ciclo");
        }
        tipo.setNombre(nombre);
        tipo.setPadre(padre);
    }
}
