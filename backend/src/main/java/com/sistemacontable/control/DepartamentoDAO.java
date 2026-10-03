package com.sistemacontable.control;

import com.sistemacontable.entity.Departamento;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Acceso a datos de {@link Departamento}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class DepartamentoDAO extends AbstractDAO<Departamento, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public DepartamentoDAO() {
        super(Departamento.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
