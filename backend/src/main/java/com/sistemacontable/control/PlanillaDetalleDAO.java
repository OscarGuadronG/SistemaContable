package com.sistemacontable.control;

import com.sistemacontable.entity.PlanillaDetalle;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/** Acceso a datos de {@link PlanillaDetalle}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class PlanillaDetalleDAO extends AbstractDAO<PlanillaDetalle, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public PlanillaDetalleDAO() {
        super(PlanillaDetalle.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Detalles de una planilla, en orden de insercion. */
    public List<PlanillaDetalle> buscarPorPlanilla(Long planillaId) {
        requerir(planillaId, "planillaId");
        return em().createQuery(
                "SELECT d FROM PlanillaDetalle d WHERE d.planilla.id = :planillaId ORDER BY d.id",
                PlanillaDetalle.class)
                .setParameter("planillaId", planillaId)
                .getResultList();
    }

    /** Indica si el empleado ya tiene detalle en la planilla (un detalle por empleado y planilla). */
    public boolean existePorPlanillaYEmpleado(Long planillaId, Long empleadoId) {
        requerir(planillaId, "planillaId");
        requerir(empleadoId, "empleadoId");
        return hayFilas(em().createQuery(
                "SELECT COUNT(d) FROM PlanillaDetalle d WHERE d.planilla.id = :planillaId "
                        + "AND d.empleado.id = :empleadoId", Long.class)
                .setParameter("planillaId", planillaId)
                .setParameter("empleadoId", empleadoId));
    }
}
