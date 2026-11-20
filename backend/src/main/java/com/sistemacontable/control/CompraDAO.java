package com.sistemacontable.control;

import com.sistemacontable.entity.Compra;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import com.sistemacontable.entity.enums.FormaPago;

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
    public List<Compra> buscar(LocalDate desde, LocalDate hasta, Long proveedorId, FormaPago formaPago, int first, int max) {
        StringBuilder jpql = new StringBuilder("SELECT c FROM Compra c WHERE 1=1");
        if (desde != null) jpql.append(" AND c.fecha>=:desde");
        if (hasta != null) jpql.append(" AND c.fecha<=:hasta");
        if (proveedorId != null) jpql.append(" AND c.proveedor.id=:proveedor");
        if (formaPago != null) jpql.append(" AND c.formaPago=:forma");
        jpql.append(" ORDER BY c.fecha,c.id");
        var q = em().createQuery(jpql.toString(), Compra.class);
        if (desde != null) q.setParameter("desde", desde);
        if (hasta != null) q.setParameter("hasta", hasta);
        if (proveedorId != null) q.setParameter("proveedor", proveedorId);
        if (formaPago != null) q.setParameter("forma", formaPago);
        return q.setFirstResult(first).setMaxResults(max).getResultList();
    }
    public long contarBuscar(LocalDate desde, LocalDate hasta, Long proveedorId, FormaPago formaPago) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(c) FROM Compra c WHERE 1=1");
        if (desde != null) jpql.append(" AND c.fecha>=:desde");
        if (hasta != null) jpql.append(" AND c.fecha<=:hasta");
        if (proveedorId != null) jpql.append(" AND c.proveedor.id=:proveedor");
        if (formaPago != null) jpql.append(" AND c.formaPago=:forma");
        var q = em().createQuery(jpql.toString(), Long.class);
        if (desde != null) q.setParameter("desde", desde);
        if (hasta != null) q.setParameter("hasta", hasta);
        if (proveedorId != null) q.setParameter("proveedor", proveedorId);
        if (formaPago != null) q.setParameter("forma", formaPago);
        return q.getSingleResult();
    }
}
