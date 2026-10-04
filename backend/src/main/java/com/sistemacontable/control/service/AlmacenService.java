package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.ExistenciasDAO;
import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.VentaDetalleDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.AlmacenCmd;
import com.sistemacontable.entity.Almacen;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Almacenes: nombre obligatorio; se crean habilitados salvo que se indique lo contrario. No se elimina uno con
 * existencias, compras, ventas o movimientos de kardex (como origen o destino): se deshabilita.
 */
@Stateless
@LocalBean
public class AlmacenService {

    @Inject
    private AlmacenDAO almacenDAO;
    @Inject
    private ExistenciasDAO existenciasDAO;
    @Inject
    private CompraDetalleDAO compraDetalleDAO;
    @Inject
    private VentaDetalleDAO ventaDetalleDAO;
    @Inject
    private KardexDAO kardexDAO;

    public Almacen crear(AlmacenCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del almacen son obligatorios");
        }
        Almacen almacen = new Almacen();
        almacen.setNombre(Validaciones.texto(cmd.nombre(), "nombre"));
        almacen.setDescripcion(Validaciones.textoOpcional(cmd.descripcion()));
        almacen.setEstado(cmd.estado() == null ? Boolean.TRUE : cmd.estado());
        almacenDAO.crear(almacen);
        almacenDAO.flush();
        return almacen;
    }

    /** Modifica nombre y descripcion; un {@code estado} nulo conserva el actual. */
    public Almacen modificar(Long id, AlmacenCmd cmd) {
        Almacen almacen = buscarPorId(id);
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del almacen son obligatorios");
        }
        almacen.setNombre(Validaciones.texto(cmd.nombre(), "nombre"));
        almacen.setDescripcion(Validaciones.textoOpcional(cmd.descripcion()));
        if (cmd.estado() != null) {
            almacen.setEstado(cmd.estado());
        }
        almacenDAO.modificar(almacen);
        return almacen;
    }

    /** Habilita ({@code true}) o deshabilita ({@code false}) un almacen. */
    public Almacen cambiarEstado(Long id, Boolean habilitado) {
        Validaciones.requerir(habilitado, "estado");
        Almacen almacen = buscarPorId(id);
        almacen.setEstado(habilitado);
        almacenDAO.modificar(almacen);
        return almacen;
    }

    public void eliminar(Long id) {
        Almacen almacen = buscarPorId(id);
        if (existenciasDAO.existePorReferencia("almacen", almacen)
                || compraDetalleDAO.existePorReferencia("almacen", almacen)
                || ventaDetalleDAO.existePorReferencia("almacen", almacen)
                || kardexDAO.existePorReferencia("origen", almacen)
                || kardexDAO.existePorReferencia("destino", almacen)) {
            throw new ReglaNegocioException("No se puede eliminar el almacen " + id
                    + " porque tiene existencias o movimientos; deshabilitelo en su lugar");
        }
        almacenDAO.eliminar(id);
    }

    public Almacen buscarPorId(Long id) {
        return almacenDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Almacen", id));
    }

    public List<Almacen> activos() {
        return almacenDAO.findActivos();
    }

    public List<Almacen> listar(int first, int max) {
        return almacenDAO.findRange(first, max);
    }
}
