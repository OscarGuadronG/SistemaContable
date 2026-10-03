package com.sistemacontable.control;

import com.sistemacontable.entity.Partida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;

/** Acceso a datos de {@link Partida}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class PartidaDAO extends AbstractDAO<Partida, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public PartidaDAO() {
        super(Partida.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Partidas con fecha entre {@code desde} y {@code hasta}, ambos extremos incluidos. */
    public List<Partida> findByRango(LocalDate desde, LocalDate hasta) {
        requerir(desde, "desde");
        requerir(hasta, "hasta");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("desde no puede ser posterior a hasta");
        }
        return em().createQuery(
                "SELECT p FROM Partida p WHERE p.fecha BETWEEN :desde AND :hasta ORDER BY p.fecha, p.id",
                Partida.class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
    }
}
