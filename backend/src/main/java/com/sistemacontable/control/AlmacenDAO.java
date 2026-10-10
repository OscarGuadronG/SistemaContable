package com.sistemacontable.control;

import com.sistemacontable.entity.Almacen;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/** Acceso a datos de {@link Almacen}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class AlmacenDAO extends AbstractDAO<Almacen, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public AlmacenDAO() {
        super(Almacen.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Almacenes habilitados (estado = true), ordenados por nombre. */
    public List<Almacen> findActivos() {
        return em().createQuery("SELECT a FROM Almacen a WHERE a.estado = true ORDER BY a.nombre", Almacen.class)
                .getResultList();
    }
}
