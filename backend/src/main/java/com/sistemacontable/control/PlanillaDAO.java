package com.sistemacontable.control;

import com.sistemacontable.entity.Planilla;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

/** Acceso a datos de {@link Planilla}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class PlanillaDAO extends AbstractDAO<Planilla, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public PlanillaDAO() {
        super(Planilla.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /**
     * Busca la planilla y la bloquea con {@code PESSIMISTIC_WRITE} (SELECT ... FOR UPDATE) hasta el fin de la
     * transaccion, para que dos transacciones no paguen ni modifiquen la misma planilla a la vez. Debe llamarse
     * dentro de una transaccion.
     */
    public Optional<Planilla> buscarParaActualizar(Long id) {
        requerir(id, "id");
        return Optional.ofNullable(em().find(Planilla.class, id, LockModeType.PESSIMISTIC_WRITE));
    }
}
