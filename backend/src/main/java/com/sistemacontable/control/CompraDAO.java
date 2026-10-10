package com.sistemacontable.control;

import com.sistemacontable.entity.Compra;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Acceso a datos de {@link Compra}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class CompraDAO extends AbstractDAO<Compra, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public CompraDAO() {
        super(Compra.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
