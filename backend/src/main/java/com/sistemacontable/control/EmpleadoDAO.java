package com.sistemacontable.control;

import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

/** Acceso a datos de {@link Empleado}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class EmpleadoDAO extends AbstractDAO<Empleado, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public EmpleadoDAO() {
        super(Empleado.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Empleados en estado Activo, ordenados por apellido y nombre. */
    public List<Empleado> findActivos() {
        return em().createQuery(
                "SELECT e FROM Empleado e WHERE e.estado = :estado ORDER BY e.apellido, e.nombre", Empleado.class)
                .setParameter("estado", EstadoEmpleado.Activo)
                .getResultList();
    }

    public List<Empleado> buscar(EstadoEmpleado estado, Long departamentoId, String buscar, int first, int max) {
        StringBuilder jpql=new StringBuilder("SELECT e FROM Empleado e WHERE 1=1");
        if(estado!=null) jpql.append(" AND e.estado=:estado");
        if(departamentoId!=null) jpql.append(" AND e.departamento.id=:departamentoId");
        String patron=patron(buscar); if(patron!=null) jpql.append(" AND LOWER(CONCAT(e.nombre,' ',e.apellido)) LIKE :buscar");
        jpql.append(" ORDER BY e.apellido,e.nombre,e.id");
        TypedQuery<Empleado> q=em().createQuery(jpql.toString(),Empleado.class);
        if(estado!=null) q.setParameter("estado",estado); if(departamentoId!=null) q.setParameter("departamentoId",departamentoId);
        if(patron!=null) q.setParameter("buscar",patron);
        return q.setFirstResult(first).setMaxResults(max).getResultList();
    }

    public long contar(EstadoEmpleado estado, Long departamentoId, String buscar) {
        StringBuilder jpql=new StringBuilder("SELECT COUNT(e) FROM Empleado e WHERE 1=1");
        if(estado!=null) jpql.append(" AND e.estado=:estado");
        if(departamentoId!=null) jpql.append(" AND e.departamento.id=:departamentoId");
        String patron=patron(buscar); if(patron!=null) jpql.append(" AND LOWER(CONCAT(e.nombre,' ',e.apellido)) LIKE :buscar");
        TypedQuery<Long> q=em().createQuery(jpql.toString(),Long.class);
        if(estado!=null) q.setParameter("estado",estado); if(departamentoId!=null) q.setParameter("departamentoId",departamentoId);
        if(patron!=null) q.setParameter("buscar",patron);
        return q.getSingleResult();
    }

    private static String patron(String buscar) { return buscar==null||buscar.isBlank()?null:"%"+buscar.trim().toLowerCase()+"%"; }

    public Optional<Empleado> findByDui(String dui) {
        requerirTexto(dui, "dui");
        return primero(em().createQuery("SELECT e FROM Empleado e WHERE e.dui = :dui", Empleado.class)
                .setParameter("dui", dui));
    }

    public Optional<Empleado> findByNumeroIsss(String numeroIsss) {
        requerirTexto(numeroIsss, "numeroIsss");
        return primero(em().createQuery(
                "SELECT e FROM Empleado e WHERE e.numeroIsss = :numeroIsss", Empleado.class)
                .setParameter("numeroIsss", numeroIsss));
    }
}
