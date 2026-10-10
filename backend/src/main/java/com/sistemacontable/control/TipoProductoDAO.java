package com.sistemacontable.control;

import com.sistemacontable.entity.TipoProducto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/** Acceso a datos de {@link TipoProducto}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class TipoProductoDAO extends AbstractDAO<TipoProducto, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public TipoProductoDAO() {
        super(TipoProducto.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Tipos de producto sin padre (raices del arbol). */
    public List<TipoProducto> findRaices() {
        return em().createQuery("SELECT t FROM TipoProducto t WHERE t.padre IS NULL ORDER BY t.nombre",
                TipoProducto.class)
                .getResultList();
    }

    /** Hijos directos de un tipo de producto. */
    public List<TipoProducto> findHijos(TipoProducto padre) {
        requerir(padre, "padre");
        return em().createQuery("SELECT t FROM TipoProducto t WHERE t.padre = :padre ORDER BY t.nombre",
                TipoProducto.class)
                .setParameter("padre", padre)
                .getResultList();
    }
}
