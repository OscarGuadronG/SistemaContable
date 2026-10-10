package com.sistemacontable.control;

import com.sistemacontable.entity.PartidaDetalle;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Acceso a datos de {@link PartidaDetalle}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class PartidaDetalleDAO extends AbstractDAO<PartidaDetalle, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public PartidaDetalleDAO() {
        super(PartidaDetalle.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Totales de cargo y abono de una cuenta en un periodo. */
    public record SumaCuenta(Long cuentaId, BigDecimal totalCargo, BigDecimal totalAbono) {
    }

    /** Lineas de una partida, en orden de insercion. */
    public List<PartidaDetalle> buscarPorPartida(Long partidaId) {
        requerir(partidaId, "partidaId");
        return em().createQuery(
                "SELECT d FROM PartidaDetalle d WHERE d.partida.id = :partidaId ORDER BY d.id", PartidaDetalle.class)
                .setParameter("partidaId", partidaId)
                .getResultList();
    }

    /**
     * Suma de cargos y abonos por cuenta para las partidas con fecha en [desde, hasta]. Solo aparecen las
     * cuentas con movimientos; agregar las hijas por jerarquia es responsabilidad del servicio.
     */
    public List<SumaCuenta> sumasPorCuenta(LocalDate desde, LocalDate hasta) {
        requerir(desde, "desde");
        requerir(hasta, "hasta");
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("desde no puede ser posterior a hasta");
        }
        List<Object[]> filas = em().createQuery(
                "SELECT d.cuenta.id, SUM(d.cargo), SUM(d.abono) FROM PartidaDetalle d "
                        + "WHERE d.partida.fecha BETWEEN :desde AND :hasta "
                        + "GROUP BY d.cuenta.id ORDER BY d.cuenta.id", Object[].class)
                .setParameter("desde", desde)
                .setParameter("hasta", hasta)
                .getResultList();
        return filas.stream()
                .map(f -> new SumaCuenta(((Number) f[0]).longValue(), (BigDecimal) f[1], (BigDecimal) f[2]))
                .toList();
    }
}
