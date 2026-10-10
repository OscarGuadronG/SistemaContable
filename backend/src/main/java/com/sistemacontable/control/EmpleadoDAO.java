package com.sistemacontable.control;

import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de {@link Empleado}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class EmpleadoDAO extends AbstractDAO<Empleado, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public EmpleadoDAO() {
        super(Empleado.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Empleados en estado Activo, ordenados por apellido y nombre. */
    public List<Empleado> findActivos() {
        return em().createQuery(
                "SELECT e FROM Empleado e WHERE e.estado = :estado ORDER BY e.apellido, e.nombre", Empleado.class)
                .setParameter("estado", EstadoEmpleado.Activo)
                .getResultList();
    }

    public Optional<Empleado> findByDui(String dui) {
        requerirTexto(dui, "dui");
        return primero(em().createQuery("SELECT e FROM Empleado e WHERE e.dui = :dui", Empleado.class)
                .setParameter("dui", dui));
    }

    public Optional<Empleado> findByNumeroIsss(String numeroIsss) {
        requerirTexto(numeroIsss, "numeroIsss");
        return primero(em().createQuery(
                "SELECT e FROM Empleado e WHERE e.numeroIsss = :numeroIsss", Empleado.class)
                .setParameter("numeroIsss", numeroIsss));
    }
}
