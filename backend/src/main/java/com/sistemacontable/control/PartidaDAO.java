package com.sistemacontable.control;

import com.sistemacontable.entity.Partida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.sistemacontable.entity.enums.TipoPartida;

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

    public Optional<com.sistemacontable.entity.Partida> findByKardex(Long kardexId) {
        return em().createQuery("SELECT p FROM Partida p WHERE p.idKardex = :id ORDER BY p.id", com.sistemacontable.entity.Partida.class)
                .setParameter("id", kardexId).setMaxResults(1).getResultList().stream().findFirst();
    }
    public Optional<Partida> findByCompra(Long compraId) {
        return em().createQuery("SELECT p FROM Partida p WHERE p.compra.id=:id ORDER BY p.id", Partida.class).setParameter("id",compraId).setMaxResults(1).getResultList().stream().findFirst();
    }
    public Optional<Partida> findByVenta(Long ventaId) {
        return em().createQuery("SELECT p FROM Partida p WHERE p.venta.id=:id ORDER BY p.id", Partida.class).setParameter("id",ventaId).setMaxResults(1).getResultList().stream().findFirst();
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

    public List<Partida> buscar(LocalDate desde, LocalDate hasta, TipoPartida tipo, Long terceroId, Long periodoId,
            String origen, int first, int max) {
        String where=filtros(desde,hasta,tipo,terceroId,periodoId,origen);
        TypedQuery<Partida> q=em().createQuery("SELECT p FROM Partida p"+where+" ORDER BY p.fecha,p.id",Partida.class);
        bindFiltros(q,desde,hasta,tipo,terceroId,periodoId,origen);
        return q.setFirstResult(first).setMaxResults(max).getResultList();
    }

    public long contarBuscar(LocalDate desde,LocalDate hasta,TipoPartida tipo,Long terceroId,Long periodoId,String origen) {
        String where=filtros(desde,hasta,tipo,terceroId,periodoId,origen);
        TypedQuery<Long> q=em().createQuery("SELECT COUNT(p) FROM Partida p"+where,Long.class);
        bindFiltros(q,desde,hasta,tipo,terceroId,periodoId,origen);
        return q.getSingleResult();
    }

    private static String filtros(LocalDate desde,LocalDate hasta,TipoPartida tipo,Long terceroId,Long periodoId,String origen) {
        StringBuilder w=new StringBuilder(" WHERE 1=1");
        if(desde!=null) w.append(" AND p.fecha>=:desde");
        if(hasta!=null) w.append(" AND p.fecha<=:hasta");
        if(tipo!=null) w.append(" AND p.tipo=:tipo");
        if(terceroId!=null) w.append(" AND p.tercero.id=:tercero");
        if(periodoId!=null) w.append(" AND p.periodo.id=:periodo");
        if(origen!=null) switch(origen) {
            case "compra" -> w.append(" AND p.compra IS NOT NULL AND p.idPagoCredito IS NULL");
            case "venta" -> w.append(" AND p.venta IS NOT NULL AND p.idPagoCredito IS NULL");
            case "planilla" -> w.append(" AND p.idPlanilla IS NOT NULL");
            case "pago" -> w.append(" AND p.idPagoCredito IS NOT NULL");
            case "devolucion" -> w.append(" AND (p.idDevolucionCompra IS NOT NULL OR p.idDevolucionVenta IS NOT NULL)");
            case "traslado" -> w.append(" AND p.idKardex IS NOT NULL AND p.tipo<>:ajuste");
            case "ajuste" -> w.append(" AND p.tipo=:ajuste");
            case "cierre" -> w.append(" AND p.tipo=:cierre");
            case "apertura" -> w.append(" AND p.tipo=:apertura");
            case "manual" -> w.append(" AND p.compra IS NULL AND p.venta IS NULL AND p.idPlanilla IS NULL AND p.idPagoCredito IS NULL "
                    + "AND p.idDevolucionCompra IS NULL AND p.idDevolucionVenta IS NULL AND p.idKardex IS NULL AND p.tipo NOT IN (:ajuste,:cierre,:apertura)");
            default -> w.append(" AND 1=0");
        }
        return w.toString();
    }

    private static void bindFiltros(TypedQuery<?> q,LocalDate desde,LocalDate hasta,TipoPartida tipo,Long terceroId,Long periodoId,String origen) {
        if(desde!=null) q.setParameter("desde",desde); if(hasta!=null) q.setParameter("hasta",hasta);
        if(tipo!=null) q.setParameter("tipo",tipo); if(terceroId!=null) q.setParameter("tercero",terceroId);
        if(periodoId!=null) q.setParameter("periodo",periodoId);
        if("ajuste".equals(origen)||"traslado".equals(origen)||"manual".equals(origen)) q.setParameter("ajuste",TipoPartida.Ajuste);
        if("cierre".equals(origen)||"manual".equals(origen)) q.setParameter("cierre",TipoPartida.Cierre);
        if("apertura".equals(origen)||"manual".equals(origen)) q.setParameter("apertura",TipoPartida.Apertura);
    }
}
