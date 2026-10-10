package com.sistemacontable.control.service;

import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.UnidadMedidaDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.UnidadMedidaCmd;
import com.sistemacontable.entity.UnidadMedida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/** Unidades de medida: nombre y abreviatura obligatorios; no se elimina una usada por productos. */
@Stateless
@LocalBean
public class UnidadMedidaService {

    @Inject
    private UnidadMedidaDAO unidadMedidaDAO;
    @Inject
    private ProductoDAO productoDAO;

    public UnidadMedida crear(UnidadMedidaCmd cmd) {
        UnidadMedida unidad = new UnidadMedida();
        aplicar(unidad, cmd);
        unidadMedidaDAO.crear(unidad);
        unidadMedidaDAO.flush();
        return unidad;
    }

    public UnidadMedida modificar(Long id, UnidadMedidaCmd cmd) {
        UnidadMedida unidad = buscarPorId(id);
        aplicar(unidad, cmd);
        unidadMedidaDAO.modificar(unidad);
        return unidad;
    }

    public void eliminar(Long id) {
        UnidadMedida unidad = buscarPorId(id);
        if (productoDAO.existePorReferencia("unidadMedida", unidad)) {
            throw new ReglaNegocioException("No se puede eliminar la unidad de medida " + id
                    + " porque la usan productos");
        }
        unidadMedidaDAO.eliminar(id);
    }

    public UnidadMedida buscarPorId(Long id) {
        return unidadMedidaDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Unidad de medida", id));
    }

    public List<UnidadMedida> listar(int first, int max) {
        return unidadMedidaDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return unidadMedidaDAO.count();
    }

    private static void aplicar(UnidadMedida unidad, UnidadMedidaCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos de la unidad de medida son obligatorios");
        }
        String nombre = Validaciones.texto(cmd.nombre(), "nombre");
        String abrev = Validaciones.texto(cmd.abrev(), "abreviatura");
        unidad.setNombre(nombre);
        unidad.setAbrev(abrev);
    }
}
