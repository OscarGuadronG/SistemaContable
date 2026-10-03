package com.sistemacontable.control;

import com.sistemacontable.entity.VentaDetalle;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;

/** Acceso a datos de {@link VentaDetalle}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class VentaDetalleDAO extends AbstractDAO<VentaDetalle, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public VentaDetalleDAO() {
        super(VentaDetalle.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Lineas de una venta, en orden de insercion. */
    public List<VentaDetalle> buscarPorVenta(Long ventaId) {
        requerir(ventaId, "ventaId");
        return em().createQuery(
                "SELECT d FROM VentaDetalle d WHERE d.venta.id = :ventaId ORDER BY d.id", VentaDetalle.class)
                .setParameter("ventaId", ventaId)
                .getResultList();
    }

    /** Total de la venta (suma de cantidad * precio); cero si no tiene lineas. */
    public BigDecimal totalVenta(Long ventaId) {
        requerir(ventaId, "ventaId");
        BigDecimal total = em().createQuery(
                "SELECT SUM(d.cantidad * d.precio) FROM VentaDetalle d WHERE d.venta.id = :ventaId",
                BigDecimal.class)
                .setParameter("ventaId", ventaId)
                .getSingleResult();
        return total == null ? BigDecimal.ZERO : total;
    }
}
