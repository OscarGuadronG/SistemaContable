package com.sistemacontable.control;

import com.sistemacontable.entity.Parametro;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;

@Stateless @LocalBean
public class ParametroDAO extends AbstractDAO<Parametro, String> {
    @PersistenceContext(unitName = "contablePU") private EntityManager em;
    public ParametroDAO() { super(Parametro.class); }
    @Override protected EntityManager getEntityManager() { return em; }
    public Optional<Parametro> buscar(String clave) { return Optional.ofNullable(em().find(Parametro.class, clave)); }
    public List<Parametro> listar() { return em().createQuery("SELECT p FROM Parametro p ORDER BY p.clave", Parametro.class).getResultList(); }
}
