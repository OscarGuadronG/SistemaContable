package com.sistemacontable.control;

import com.sistemacontable.entity.Venta;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import com.sistemacontable.entity.enums.FormaPago;

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
    public List<Venta> buscar(LocalDate desde, LocalDate hasta, Long clienteId, FormaPago formaPago, int first, int max) {
        StringBuilder jpql = new StringBuilder("SELECT v FROM Venta v WHERE 1=1");
        if (desde != null) jpql.append(" AND v.fecha>=:desde");
        if (hasta != null) jpql.append(" AND v.fecha<=:hasta");
        if (clienteId != null) jpql.append(" AND v.cliente.id=:cliente");
        if (formaPago != null) jpql.append(" AND v.formaPago=:forma");
        jpql.append(" ORDER BY v.fecha,v.id");
        var q = em().createQuery(jpql.toString(), Venta.class);
        if (desde != null) q.setParameter("desde", desde);
        if (hasta != null) q.setParameter("hasta", hasta);
        if (clienteId != null) q.setParameter("cliente", clienteId);
        if (formaPago != null) q.setParameter("forma", formaPago);
        return q.setFirstResult(first).setMaxResults(max).getResultList();
    }
    public long contarBuscar(LocalDate desde, LocalDate hasta, Long clienteId, FormaPago formaPago) {
        StringBuilder jpql = new StringBuilder("SELECT COUNT(v) FROM Venta v WHERE 1=1");
        if (desde != null) jpql.append(" AND v.fecha>=:desde");
        if (hasta != null) jpql.append(" AND v.fecha<=:hasta");
        if (clienteId != null) jpql.append(" AND v.cliente.id=:cliente");
        if (formaPago != null) jpql.append(" AND v.formaPago=:forma");
        var q = em().createQuery(jpql.toString(), Long.class);
        if (desde != null) q.setParameter("desde", desde);
        if (hasta != null) q.setParameter("hasta", hasta);
        if (clienteId != null) q.setParameter("cliente", clienteId);
        if (formaPago != null) q.setParameter("forma", formaPago);
        return q.getSingleResult();
    }
}
