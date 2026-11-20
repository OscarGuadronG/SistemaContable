package com.sistemacontable.control;
import com.sistemacontable.entity.DevolucionCompraDetalle;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
/** Acceso CRUD de {@link DevolucionCompraDetalle}; las reglas se ejecutan en el servicio. */
@Stateless
@LocalBean
public class DevolucionCompraDetalleDAO extends AbstractDAO<DevolucionCompraDetalle, Long> {
    @PersistenceContext(unitName = "contablePU") private EntityManager em;
    public DevolucionCompraDetalleDAO() { super(DevolucionCompraDetalle.class); }
    @Override protected EntityManager getEntityManager() { return em; }
}
