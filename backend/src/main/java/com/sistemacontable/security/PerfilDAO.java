package com.sistemacontable.security;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.sistemacontable.entity.Empleado;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless
@LocalBean
public class PerfilDAO {
    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public Optional<Perfil> buscar(UUID id) {
        return Optional.ofNullable(em.find(Perfil.class, id));
    }

    public Perfil requerido(UUID id) {
        return buscar(id).orElse(null);
    }

    public List<Perfil> listar() {
        return em.createQuery("SELECT p FROM Perfil p ORDER BY p.nombre, p.id", Perfil.class).getResultList();
    }

    public void guardar(Perfil perfil) {
        em.persist(perfil);
        em.flush(); // Permite compensar Auth antes de devolver una creación si falla una restricción.
    }

    public boolean existeEmpleado(Long id) {
        return id != null && em.find(Empleado.class, id) != null;
    }

    public Perfil modificar(Perfil perfil) {
        return em.merge(perfil);
    }
}
