package com.sistemacontable.control;

import com.sistemacontable.entity.Proveedor;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

/** Acceso a datos de {@link Proveedor}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class ProveedorDAO extends AbstractDAO<Proveedor, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public ProveedorDAO() {
        super(Proveedor.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Busca por NIT (unico). */
    public Optional<Proveedor> findByNit(String nit) {
        requerirTexto(nit, "nit");
        return primero(em().createQuery("SELECT x FROM Proveedor x WHERE x.nit = :nit", Proveedor.class)
                .setParameter("nit", nit));
    }
}
