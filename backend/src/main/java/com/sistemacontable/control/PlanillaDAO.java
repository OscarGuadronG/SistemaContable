package com.sistemacontable.control;

import com.sistemacontable.entity.Planilla;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.Optional;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import com.sistemacontable.entity.enums.EstadoPlanilla;

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

    public List<Planilla> buscar(EstadoPlanilla estado,LocalDate desde,LocalDate hasta,int first,int max) {
        OffsetDateTime d=desde==null?null:desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime h=hasta==null?null:hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        StringBuilder jpql=new StringBuilder("SELECT p FROM Planilla p WHERE 1=1");
        if(estado!=null) jpql.append(" AND p.estado=:estado"); if(d!=null) jpql.append(" AND p.fechaInicio>=:desde"); if(h!=null) jpql.append(" AND p.fechaFin<:hasta");
        jpql.append(" ORDER BY p.fechaInicio,p.id"); TypedQuery<Planilla> q=em().createQuery(jpql.toString(),Planilla.class);
        if(estado!=null) q.setParameter("estado",estado); if(d!=null) q.setParameter("desde",d); if(h!=null) q.setParameter("hasta",h);
        return q.setFirstResult(first).setMaxResults(max).getResultList();
    }

    public long contar(EstadoPlanilla estado,LocalDate desde,LocalDate hasta) {
        OffsetDateTime d=desde==null?null:desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime h=hasta==null?null:hasta.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        StringBuilder jpql=new StringBuilder("SELECT COUNT(p) FROM Planilla p WHERE 1=1");
        if(estado!=null) jpql.append(" AND p.estado=:estado"); if(d!=null) jpql.append(" AND p.fechaInicio>=:desde"); if(h!=null) jpql.append(" AND p.fechaFin<:hasta");
        TypedQuery<Long> q=em().createQuery(jpql.toString(),Long.class);
        if(estado!=null) q.setParameter("estado",estado); if(d!=null) q.setParameter("desde",d); if(h!=null) q.setParameter("hasta",h);
        return q.getSingleResult();
    }
}
