package com.sistemacontable.control;
import com.sistemacontable.entity.DevolucionCompra;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
/** Acceso CRUD de {@link DevolucionCompra}; las reglas se ejecutan en el servicio. */
@Stateless
@LocalBean
public class DevolucionCompraDAO extends AbstractDAO<DevolucionCompra, Long> {
    @PersistenceContext(unitName = "contablePU") private EntityManager em;
    public DevolucionCompraDAO() { super(DevolucionCompra.class); }
    @Override protected EntityManager getEntityManager() { return em; }
}
