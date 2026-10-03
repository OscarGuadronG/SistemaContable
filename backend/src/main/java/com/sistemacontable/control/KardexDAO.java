package com.sistemacontable.control;

import com.sistemacontable.entity.Kardex;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

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
}
