package com.sistemacontable.control;

import com.sistemacontable.entity.Venta;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Acceso a datos de {@link Venta}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class VentaDAO extends AbstractDAO<Venta, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public VentaDAO() {
        super(Venta.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
