package com.sistemacontable.control;

import com.sistemacontable.entity.Cuenta;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de {@link Cuenta}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class CuentaDAO extends AbstractDAO<Cuenta, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public CuentaDAO() {
        super(Cuenta.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Busca una cuenta por su codigo contable (unico). */
    public Optional<Cuenta> findByCodigo(Integer codigo) {
        requerir(codigo, "codigo");
        return primero(em().createQuery("SELECT c FROM Cuenta c WHERE c.codigo = :codigo", Cuenta.class)
                .setParameter("codigo", codigo));
    }

    /** Cuentas hijas directas de {@code padre}, ordenadas por codigo. */
    public List<Cuenta> findHijos(Cuenta padre) {
        requerir(padre, "padre");
        return em().createQuery("SELECT c FROM Cuenta c WHERE c.padre = :padre ORDER BY c.codigo", Cuenta.class)
                .setParameter("padre", padre)
                .getResultList();
    }

    /** Indica si {@code padre} tiene al menos una cuenta hija activa. */
    public boolean existenHijosActivos(Cuenta padre) {
        requerir(padre, "padre");
        return hayFilas(em().createQuery(
                "SELECT COUNT(c) FROM Cuenta c WHERE c.padre = :padre AND c.activo = true", Long.class)
                .setParameter("padre", padre));
    }

    /** Indica si la cuenta ya tiene lineas de partida (movimientos). */
    public boolean tieneMovimientos(Cuenta cuenta) {
        requerir(cuenta, "cuenta");
        return hayFilas(em().createQuery(
                "SELECT COUNT(d) FROM PartidaDetalle d WHERE d.cuenta = :cuenta", Long.class)
                .setParameter("cuenta", cuenta));
    }
}
