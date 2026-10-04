package com.sistemacontable.control.service;

import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.DepartamentoDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.PlanillaDetalleDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.EmpleadoCmd;
import com.sistemacontable.entity.Departamento;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.List;

/**
 * Empleados: nombre, apellido, DUI, numero de ISSS y departamento obligatorios; salario obligatorio y &gt;= 0;
 * DUI y numero de ISSS unicos (se valida antes de que falle la constraint, con un mensaje claro). No se elimina un
 * empleado con compras, ventas o planillas: se le cambia el estado (Inactivo/Suspendido).
 */
@Stateless
@LocalBean
public class EmpleadoService {

    @Inject
    private EmpleadoDAO empleadoDAO;
    @Inject
    private DepartamentoDAO departamentoDAO;
    @Inject
    private CompraDAO compraDAO;
    @Inject
    private VentaDAO ventaDAO;
    @Inject
    private PlanillaDetalleDAO planillaDetalleDAO;

    public Empleado crear(EmpleadoCmd cmd) {
        Empleado empleado = new Empleado();
        aplicar(empleado, cmd, EstadoEmpleado.Activo);
        empleadoDAO.crear(empleado);
        empleadoDAO.flush();
        return empleado;
    }

    /** Modifica los datos; un {@code estado} nulo conserva el actual. */
    public Empleado modificar(Long id, EmpleadoCmd cmd) {
        Empleado empleado = buscarPorId(id);
        aplicar(empleado, cmd, empleado.getEstado());
        empleadoDAO.modificar(empleado);
        return empleado;
    }

    public Empleado cambiarEstado(Long id, EstadoEmpleado estado) {
        Validaciones.requerir(estado, "estado");
        Empleado empleado = buscarPorId(id);
        empleado.setEstado(estado);
        empleadoDAO.modificar(empleado);
        return empleado;
    }

    public void eliminar(Long id) {
        Empleado empleado = buscarPorId(id);
        if (compraDAO.existePorReferencia("empleado", empleado)
                || ventaDAO.existePorReferencia("empleado", empleado)
                || planillaDetalleDAO.existePorReferencia("empleado", empleado)) {
            throw new ReglaNegocioException("No se puede eliminar el empleado " + id
                    + " porque tiene compras, ventas o planillas; cambie su estado a Inactivo");
        }
        empleadoDAO.eliminar(id);
    }

    public Empleado buscarPorId(Long id) {
        return empleadoDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Empleado", id));
    }

    public List<Empleado> activos() {
        return empleadoDAO.findActivos();
    }

    public List<Empleado> listar(int first, int max) {
        return empleadoDAO.findRange(first, max);
    }

    private void aplicar(Empleado empleado, EmpleadoCmd cmd, EstadoEmpleado estadoPorDefecto) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del empleado son obligatorios");
        }
        String nombre = Validaciones.texto(cmd.nombre(), "nombre");
        String apellido = Validaciones.texto(cmd.apellido(), "apellido");
        String dui = Validaciones.texto(cmd.dui(), "DUI");
        String isss = Validaciones.texto(cmd.numeroIsss(), "numero de ISSS");
        Long departamentoId = Validaciones.requerir(cmd.departamentoId(), "departamento");
        Departamento departamento = departamentoDAO.buscarPorId(departamentoId)
                .orElseThrow(() -> new NoEncontradoException("Departamento", departamentoId));
        empleadoDAO.findByDui(dui).ifPresent(otro -> {
            if (!otro.equals(empleado)) {
                throw new ReglaNegocioException("Ya existe un empleado con el DUI " + dui);
            }
        });
        empleadoDAO.findByNumeroIsss(isss).ifPresent(otro -> {
            if (!otro.equals(empleado)) {
                throw new ReglaNegocioException("Ya existe un empleado con el numero de ISSS " + isss);
            }
        });
        BigDecimal salario = Validaciones.noNegativo(cmd.salario(), "salario");
        empleado.setNombre(nombre);
        empleado.setApellido(apellido);
        empleado.setFechNac(cmd.fechNac());
        empleado.setDepartamento(departamento);
        empleado.setSalario(salario);
        empleado.setEstado(cmd.estado() != null ? cmd.estado() : estadoPorDefecto);
        empleado.setNumeroIsss(isss);
        empleado.setDui(dui);
    }
}
