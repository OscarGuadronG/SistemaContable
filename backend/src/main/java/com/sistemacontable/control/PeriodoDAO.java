package com.sistemacontable.control;

import com.sistemacontable.entity.Periodo;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Stateless @LocalBean
public class PeriodoDAO extends AbstractDAO<Periodo, Long> {
    @PersistenceContext(unitName = "contablePU") private EntityManager em;
    public PeriodoDAO() { super(Periodo.class); }
    @Override protected EntityManager getEntityManager() { return em; }

    public Optional<Periodo> findByFecha(LocalDate fecha) {
        return em().createQuery("SELECT p FROM Periodo p WHERE :fecha BETWEEN p.fechaInicio AND p.fechaFin", Periodo.class)
                .setParameter("fecha", fecha).setMaxResults(1).getResultList().stream().findFirst();
    }
    public Optional<Periodo> findByFechaParaActualizar(LocalDate fecha) {
        return em().createQuery("SELECT p FROM Periodo p WHERE :fecha BETWEEN p.fechaInicio AND p.fechaFin", Periodo.class)
                .setParameter("fecha", fecha).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().stream().findFirst();
    }
    public Optional<Periodo> buscarParaActualizar(Long id) {
        requerir(id,"id");
        return Optional.ofNullable(em().find(Periodo.class,id,LockModeType.PESSIMISTIC_WRITE));
    }
    public List<Periodo> findAllOrdenados() {
        return em().createQuery("SELECT p FROM Periodo p ORDER BY p.fechaInicio, p.id", Periodo.class).getResultList();
    }
    public Optional<Periodo> findSiguiente(LocalDate despuesDe) {
        return em().createQuery("SELECT p FROM Periodo p WHERE p.fechaInicio > :fecha ORDER BY p.fechaInicio", Periodo.class)
                .setParameter("fecha", despuesDe).setMaxResults(1).getResultList().stream().findFirst();
    }
    public boolean existenAnterioresAbiertos(Periodo periodo) {
        return em().createQuery("SELECT COUNT(p) FROM Periodo p WHERE p.fechaInicio < :inicio AND p.estado <> :cerrado", Long.class)
                .setParameter("inicio", periodo.getFechaInicio()).setParameter("cerrado", com.sistemacontable.entity.enums.EstadoPeriodo.Cerrado)
                .getSingleResult() > 0;
    }
}
