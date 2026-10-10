package com.sistemacontable.control;

import com.sistemacontable.entity.KardexDetalle;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/** Acceso a datos de {@link KardexDetalle}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class KardexDetalleDAO extends AbstractDAO<KardexDetalle, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public KardexDetalleDAO() {
        super(KardexDetalle.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Lineas de un movimiento de kardex, en orden de insercion. */
    public List<KardexDetalle> buscarPorKardex(Long kardexId) {
        requerir(kardexId, "kardexId");
        return em().createQuery(
                "SELECT d FROM KardexDetalle d WHERE d.kardex.id = :kardexId ORDER BY d.id", KardexDetalle.class)
                .setParameter("kardexId", kardexId)
                .getResultList();
    }
}
