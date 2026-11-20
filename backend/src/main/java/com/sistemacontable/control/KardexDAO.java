package com.sistemacontable.control;

import com.sistemacontable.entity.Kardex;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Optional;
import java.time.LocalDate;
import com.sistemacontable.entity.enums.TipoMovimiento;

/** Acceso a datos de {@link Kardex}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class KardexDAO extends AbstractDAO<Kardex, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public KardexDAO() {
        super(Kardex.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /**
     * Movimientos que tocan un almacen, ya sea como origen o como destino. Usa LEFT JOIN porque en una
     * entrada el origen es nulo y en una salida el destino es nulo.
     */
    public List<Kardex> findByAlmacen(Long almacenId) {
        requerir(almacenId, "almacenId");
        return em().createQuery(
                "SELECT k FROM Kardex k LEFT JOIN k.origen o LEFT JOIN k.destino d "
                        + "WHERE o.id = :almacenId OR d.id = :almacenId ORDER BY k.fecha, k.id", Kardex.class)
                .setParameter("almacenId", almacenId)
                .getResultList();
    }
    public List<Kardex> findByCompra(Long compraId) {
        return em().createQuery("SELECT k FROM Kardex k WHERE k.compra.id=:id ORDER BY k.fecha,k.id", Kardex.class).setParameter("id",compraId).getResultList();
    }
    public List<Kardex> findByVenta(Long ventaId) {
        return em().createQuery("SELECT k FROM Kardex k WHERE k.venta.id=:id ORDER BY k.fecha,k.id", Kardex.class).setParameter("id",ventaId).getResultList();
    }

    /** Coste histórico ponderado de entrada del lote al almacén, basado exclusivamente en kardex entrante. */
    public Optional<BigDecimal> costoEntradas(Long loteId, Long almacenId) {
        Object[] row = em().createQuery("SELECT SUM(d.cantidad * d.precioCosto), SUM(d.cantidad) FROM KardexDetalle d "
                + "WHERE d.lote.id = :lote AND d.kardex.destino.id = :almacen", Object[].class)
                .setParameter("lote", loteId).setParameter("almacen", almacenId).getSingleResult();
        BigDecimal valor = (BigDecimal) row[0], cantidad = (BigDecimal) row[1];
        return valor == null || cantidad == null || cantidad.signum() == 0 ? Optional.empty()
                : Optional.of(valor.divide(cantidad, MathContext.DECIMAL128));
    }

    /** Primera fecha de entrada a ese almacén; es la clave primaria de FIFO. */
    public Optional<java.time.OffsetDateTime> primeraEntrada(Long loteId, Long almacenId) {
        return Optional.ofNullable(em().createQuery("SELECT MIN(k.fecha) FROM KardexDetalle d JOIN d.kardex k "
                + "WHERE d.lote.id = :lote AND k.destino.id = :almacen", java.time.OffsetDateTime.class)
                .setParameter("lote", loteId).setParameter("almacen", almacenId).getSingleResult());
    }

    public List<Kardex> findTraslados(LocalDate desde, LocalDate hasta, Long almacenId, int first, int max) {
        StringBuilder jpql = trasladosWhere(desde, hasta, almacenId, false);
        jpql.append(" ORDER BY k.fecha,k.id");
        var q = em().createQuery(jpql.toString(), Kardex.class);
        bindTraslados(q, desde, hasta, almacenId);
        return q.setFirstResult(first).setMaxResults(max).getResultList();
    }
    public long contarTraslados(LocalDate desde, LocalDate hasta, Long almacenId) {
        StringBuilder jpql = trasladosWhere(desde, hasta, almacenId, true);
        var q = em().createQuery(jpql.toString(), Long.class);
        bindTraslados(q, desde, hasta, almacenId);
        return q.getSingleResult();
    }

    private static StringBuilder trasladosWhere(LocalDate desde, LocalDate hasta, Long almacenId, boolean count) {
        StringBuilder jpql = new StringBuilder(count ? "SELECT COUNT(k) FROM Kardex k WHERE k.tipoMovimiento=:tipo" : "SELECT k FROM Kardex k WHERE k.tipoMovimiento=:tipo");
        if (desde != null) jpql.append(" AND k.fecha>=:desde");
        if (hasta != null) jpql.append(" AND k.fecha<:hasta");
        if (almacenId != null) jpql.append(" AND (k.origen.id=:almacen OR k.destino.id=:almacen)");
        return jpql;
    }
    private static void bindTraslados(jakarta.persistence.TypedQuery<?> query, LocalDate desde, LocalDate hasta, Long almacenId) {
        query.setParameter("tipo", TipoMovimiento.Traslado);
        if (desde != null) query.setParameter("desde", desde.atStartOfDay().atOffset(java.time.ZoneOffset.UTC));
        if (hasta != null) query.setParameter("hasta", hasta.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC));
        if (almacenId != null) query.setParameter("almacen", almacenId);
    }
}
