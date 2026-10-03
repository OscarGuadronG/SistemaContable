package com.sistemacontable.control;

import com.sistemacontable.entity.UnidadMedida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Acceso a datos de {@link UnidadMedida}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class UnidadMedidaDAO extends AbstractDAO<UnidadMedida, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public UnidadMedidaDAO() {
        super(UnidadMedida.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
