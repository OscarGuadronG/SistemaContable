package com.sistemacontable.control.service;

import com.sistemacontable.control.DepartamentoDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.DepartamentoCmd;
import com.sistemacontable.entity.Departamento;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/** Departamentos: nombre obligatorio; no se elimina uno que tenga empleados. */
@Stateless
@LocalBean
public class DepartamentoService {

    @Inject
    private DepartamentoDAO departamentoDAO;
    @Inject
    private EmpleadoDAO empleadoDAO;

    public Departamento crear(DepartamentoCmd cmd) {
        Departamento departamento = new Departamento();
        aplicar(departamento, cmd);
        departamentoDAO.crear(departamento);
        departamentoDAO.flush();
        return departamento;
    }

    public Departamento modificar(Long id, DepartamentoCmd cmd) {
        Departamento departamento = buscarPorId(id);
        aplicar(departamento, cmd);
        departamentoDAO.modificar(departamento);
        return departamento;
    }

    public void eliminar(Long id) {
        Departamento departamento = buscarPorId(id);
        if (empleadoDAO.existePorReferencia("departamento", departamento)) {
            throw new ReglaNegocioException("No se puede eliminar el departamento " + id + " porque tiene empleados");
        }
        departamentoDAO.eliminar(id);
    }

    public Departamento buscarPorId(Long id) {
        return departamentoDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Departamento", id));
    }

    public List<Departamento> listar(int first, int max) {
        return departamentoDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return departamentoDAO.count();
    }

    private static void aplicar(Departamento departamento, DepartamentoCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del departamento son obligatorios");
        }
        departamento.setNombre(Validaciones.texto(cmd.nombre(), "nombre"));
        departamento.setDescripcion(Validaciones.textoOpcional(cmd.descripcion()));
    }
}
