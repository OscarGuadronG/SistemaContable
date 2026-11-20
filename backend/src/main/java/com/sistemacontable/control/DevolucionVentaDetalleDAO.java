package com.sistemacontable.control;
import com.sistemacontable.entity.DevolucionVentaDetalle;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
/** Acceso CRUD de {@link DevolucionVentaDetalle}; las reglas se ejecutan en el servicio. */
@Stateless
@LocalBean
public class DevolucionVentaDetalleDAO extends AbstractDAO<DevolucionVentaDetalle, Long> {
    @PersistenceContext(unitName = "contablePU") private EntityManager em;
    public DevolucionVentaDetalleDAO() { super(DevolucionVentaDetalle.class); }
    @Override protected EntityManager getEntityManager() { return em; }
}
