package com.sistemacontable.control;

import com.sistemacontable.entity.Lote;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/** Acceso a datos de {@link Lote}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class LoteDAO extends AbstractDAO<Lote, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public LoteDAO() {
        super(Lote.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Lotes de un producto, ordenados por numero de lote. */
    public List<Lote> findByProducto(Long productoId) {
        requerir(productoId, "productoId");
        return em().createQuery(
                "SELECT l FROM Lote l WHERE l.producto.id = :productoId ORDER BY l.numLote, l.id", Lote.class)
                .setParameter("productoId", productoId)
                .getResultList();
    }
}
