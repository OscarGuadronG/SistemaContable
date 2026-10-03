package com.sistemacontable.control;

import com.sistemacontable.entity.Planilla;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Acceso a datos de {@link Planilla}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class PlanillaDAO extends AbstractDAO<Planilla, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public PlanillaDAO() {
        super(Planilla.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
