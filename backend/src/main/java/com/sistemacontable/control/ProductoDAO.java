package com.sistemacontable.control;

import com.sistemacontable.entity.Producto;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Set;

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

    public List<Producto> buscarInventario(String texto, Set<Long> tipoIds, int first, int max) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM Producto p WHERE 1=1");
        boolean filtrarTexto = texto != null && !texto.isBlank();
        boolean filtrarTipo = tipoIds != null && !tipoIds.isEmpty();
        if (filtrarTexto) jpql.append(" AND (LOWER(p.nombre) LIKE :q OR LOWER(p.tipo.nombre) LIKE :q)");
        if (filtrarTipo) jpql.append(" AND p.tipo.id IN :tipos");
        jpql.append(" ORDER BY p.nombre,p.id");
        var q = em().createQuery(jpql.toString(), Producto.class);
        if (filtrarTexto) q.setParameter("q", "%" + texto.trim().toLowerCase() + "%");
        if (filtrarTipo) q.setParameter("tipos", tipoIds);
        return q.setFirstResult(first).setMaxResults(max).getResultList();
    }
    public long contarInventario(String texto, Set<Long> tipoIds) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(p) FROM Producto p WHERE 1=1");
        boolean filtrarTexto = texto != null && !texto.isBlank();
        boolean filtrarTipo = tipoIds != null && !tipoIds.isEmpty();
        if (filtrarTexto) jpql.append(" AND (LOWER(p.nombre) LIKE :q OR LOWER(p.tipo.nombre) LIKE :q)");
        if (filtrarTipo) jpql.append(" AND p.tipo.id IN :tipos");
        var q = em().createQuery(jpql.toString(), Long.class);
        if (filtrarTexto) q.setParameter("q", "%" + texto.trim().toLowerCase() + "%");
        if (filtrarTipo) q.setParameter("tipos", tipoIds);
        return q.getSingleResult();
    }
}
