package com.sistemacontable.control.service;

import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.TerceroDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.TerceroCmd;
import com.sistemacontable.entity.Tercero;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/** Terceros (contrapartes de partidas): nombre obligatorio; no se elimina uno referenciado por partidas. */
@Stateless
@LocalBean
public class TerceroService {

    @Inject
    private TerceroDAO terceroDAO;
    @Inject
    private PartidaDAO partidaDAO;

    public Tercero crear(TerceroCmd cmd) {
        Tercero tercero = new Tercero();
        aplicar(tercero, cmd);
        terceroDAO.crear(tercero);
        terceroDAO.flush();
        return tercero;
    }

    public Tercero modificar(Long id, TerceroCmd cmd) {
        Tercero tercero = buscarPorId(id);
        aplicar(tercero, cmd);
        terceroDAO.modificar(tercero);
        return tercero;
    }

    public void eliminar(Long id) {
        Tercero tercero = buscarPorId(id);
        if (partidaDAO.existePorReferencia("tercero", tercero)) {
            throw new ReglaNegocioException("No se puede eliminar el tercero " + id + " porque tiene partidas");
        }
        terceroDAO.eliminar(id);
    }

    public Tercero buscarPorId(Long id) {
        return terceroDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Tercero", id));
    }

    public List<Tercero> listar(int first, int max) {
        return terceroDAO.findRange(first, max);
    }

    private static void aplicar(Tercero tercero, TerceroCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del tercero son obligatorios");
        }
        tercero.setNombre(Validaciones.texto(cmd.nombre(), "nombre"));
        tercero.setDescripcion(Validaciones.textoOpcional(cmd.descripcion()));
    }
}
