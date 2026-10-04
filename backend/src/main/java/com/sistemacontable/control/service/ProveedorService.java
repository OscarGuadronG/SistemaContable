package com.sistemacontable.control.service;

import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.ProveedorDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.ProveedorCmd;
import com.sistemacontable.entity.Proveedor;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/** Proveedores: nombre y NIT obligatorios, NIT unico (validado antes de la constraint); no se elimina con compras. */
@Stateless
@LocalBean
public class ProveedorService {

    @Inject
    private ProveedorDAO proveedorDAO;
    @Inject
    private CompraDAO compraDAO;

    public Proveedor crear(ProveedorCmd cmd) {
        Proveedor proveedor = new Proveedor();
        aplicar(proveedor, cmd);
        proveedorDAO.crear(proveedor);
        proveedorDAO.flush();
        return proveedor;
    }

    public Proveedor modificar(Long id, ProveedorCmd cmd) {
        Proveedor proveedor = buscarPorId(id);
        aplicar(proveedor, cmd);
        proveedorDAO.modificar(proveedor);
        return proveedor;
    }

    public void eliminar(Long id) {
        Proveedor proveedor = buscarPorId(id);
        if (compraDAO.existePorReferencia("proveedor", proveedor)) {
            throw new ReglaNegocioException("No se puede eliminar el proveedor " + id + " porque tiene compras");
        }
        proveedorDAO.eliminar(id);
    }

    public Proveedor buscarPorId(Long id) {
        return proveedorDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Proveedor", id));
    }

    public Proveedor buscarPorNit(String nit) {
        return proveedorDAO.findByNit(nit).orElseThrow(() -> new NoEncontradoException("Proveedor con NIT", nit));
    }

    public List<Proveedor> listar(int first, int max) {
        return proveedorDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return proveedorDAO.count();
    }

    private void aplicar(Proveedor proveedor, ProveedorCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del proveedor son obligatorios");
        }
        String nombre = Validaciones.texto(cmd.nombre(), "nombre");
        String nit = Validaciones.texto(cmd.nit(), "NIT");
        proveedorDAO.findByNit(nit).ifPresent(otro -> {
            if (!otro.equals(proveedor)) {
                throw new ReglaNegocioException("Ya existe un proveedor con el NIT " + nit);
            }
        });
        proveedor.setNombre(nombre);
        proveedor.setDescripcion(Validaciones.textoOpcional(cmd.descripcion()));
        proveedor.setNit(nit);
    }
}
