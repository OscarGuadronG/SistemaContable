package com.sistemacontable.control;

import com.sistemacontable.entity.CompraDetalle;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de {@link CompraDetalle}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class CompraDetalleDAO extends AbstractDAO<CompraDetalle, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public CompraDetalleDAO() {
        super(CompraDetalle.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Lineas de una compra, en orden de insercion. */
    public List<CompraDetalle> buscarPorCompra(Long compraId) {
        requerir(compraId, "compraId");
        return em().createQuery(
                "SELECT d FROM CompraDetalle d WHERE d.compra.id = :compraId ORDER BY d.id", CompraDetalle.class)
                .setParameter("compraId", compraId)
                .getResultList();
    }

    /** Total de la compra (suma de cantidad * precio); cero si no tiene lineas. */
    public BigDecimal totalCompra(Long compraId) {
        requerir(compraId, "compraId");
        BigDecimal total = em().createQuery(
                "SELECT SUM(d.cantidad * d.precio) FROM CompraDetalle d WHERE d.compra.id = :compraId",
                BigDecimal.class)
                .setParameter("compraId", compraId)
                .getSingleResult();
        return total == null ? BigDecimal.ZERO : total;
    }

    /**
     * Costo promedio ponderado de un lote: SUM(cantidad * precio) / SUM(cantidad) sobre todas sus compras.
     * Vacio si el lote nunca se compro (o la cantidad total es cero). La division usa precision amplia
     * (DECIMAL128) para no introducir un redondeo arbitrario; el servicio decide como redondear al contabilizar.
     */
    public Optional<BigDecimal> costoPromedioPonderado(Long loteId) {
        requerir(loteId, "loteId");
        Object[] fila = em().createQuery(
                "SELECT SUM(d.cantidad * d.precio), SUM(d.cantidad) FROM CompraDetalle d WHERE d.lote.id = :loteId",
                Object[].class)
                .setParameter("loteId", loteId)
                .getSingleResult();
        BigDecimal importe = (BigDecimal) fila[0];
        BigDecimal cantidad = (BigDecimal) fila[1];
        if (importe == null || cantidad == null || cantidad.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(importe.divide(cantidad, MathContext.DECIMAL128));
    }
}
