package com.sistemacontable.control;

import com.sistemacontable.entity.Existencias;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de {@link Existencias}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class ExistenciasDAO extends AbstractDAO<Existencias, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public ExistenciasDAO() {
        super(Existencias.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /**
     * Busca la fila de existencias de un lote en un almacen y la bloquea con {@code PESSIMISTIC_WRITE}
     * (SELECT ... FOR UPDATE) para que dos transacciones no actualicen el mismo stock a la vez. Debe llamarse
     * dentro de una transaccion.
     *
     * <p>La BD no tiene UNIQUE(id_lote, id_almacen) (ver docs/recomendaciones-bd.md): si hubiera filas
     * duplicadas se falla en lugar de elegir una al azar.</p>
     *
     * @throws IllegalStateException si existe mas de una fila para el par lote/almacen
     */
    public Optional<Existencias> findForUpdate(Long loteId, Long almacenId) {
        requerir(loteId, "loteId");
        requerir(almacenId, "almacenId");
        List<Existencias> filas = em().createQuery(
                "SELECT x FROM Existencias x WHERE x.lote.id = :loteId AND x.almacen.id = :almacenId ORDER BY x.id",
                Existencias.class)
                .setParameter("loteId", loteId)
                .setParameter("almacenId", almacenId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        if (filas.size() > 1) {
            throw new IllegalStateException("Existencias duplicadas para lote " + loteId + " y almacen " + almacenId
                    + ": falta el UNIQUE(id_lote, id_almacen) en la BD");
        }
        return filas.stream().findFirst();
    }

    /** Stock total de un lote sumando todos los almacenes; cero si no hay filas. */
    public BigDecimal stockTotalPorLote(Long loteId) {
        requerir(loteId, "loteId");
        BigDecimal total = em().createQuery(
                "SELECT SUM(x.cantidad) FROM Existencias x WHERE x.lote.id = :loteId", BigDecimal.class)
                .setParameter("loteId", loteId)
                .getSingleResult();
        return total == null ? BigDecimal.ZERO : total;
    }
}
