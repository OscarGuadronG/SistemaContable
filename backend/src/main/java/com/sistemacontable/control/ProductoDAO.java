package com.sistemacontable.control;

import com.sistemacontable.entity.Producto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/** Acceso a datos de {@link Producto}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class ProductoDAO extends AbstractDAO<Producto, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public ProductoDAO() {
        super(Producto.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
}
