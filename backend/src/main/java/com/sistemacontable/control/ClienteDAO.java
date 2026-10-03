package com.sistemacontable.control;

import com.sistemacontable.entity.Cliente;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;

/** Acceso a datos de {@link Cliente}. Sin reglas de negocio: estas viven en los servicios. */
@Stateless
@LocalBean
public class ClienteDAO extends AbstractDAO<Cliente, Long> {

    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public ClienteDAO() {
        super(Cliente.class);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    /** Busca por NIT (unico). */
    public Optional<Cliente> findByNit(String nit) {
        requerirTexto(nit, "nit");
        return primero(em().createQuery("SELECT x FROM Cliente x WHERE x.nit = :nit", Cliente.class)
                .setParameter("nit", nit));
    }
}
