package com.sistemacontable.control;

import com.sistemacontable.entity.Tercero;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Acceso a datos de {@link Tercero}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class TerceroDAO extends AbstractDAO<Tercero, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public TerceroDAO() {
        super(Tercero.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
