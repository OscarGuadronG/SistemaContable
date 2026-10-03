package com.sistemacontable.control;

import com.sistemacontable.control.exception.NoEncontradoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.List;
import java.util.Optional;

/**
 * DAO generico con las operaciones CRUD y de paginacion comunes. No contiene reglas de negocio.
 *
 * <p>Convenciones: argumentos nulos o fuera de rango lanzan {@link IllegalArgumentException}; un
 * {@code EntityManager} ausente lanza {@link IllegalStateException}; un registro inexistente al modificar o
 * eliminar lanza {@link NoEncontradoException}. Las excepciones del proveedor JPA se propagan sin envolver
 * (no se tragan ni se convierten en valores centinela).</p>
 *
 * @param <T> tipo de la entidad
 * @param <ID> tipo de la clave primaria
 */
public abstract class AbstractDAO<T, ID> {

    /** Tope de registros por pagina en {@link #findRange(int, int)}. */
    public static final int MAX_POR_PAGINA = 100;

    private final Class<T> entityClass;

    protected AbstractDAO(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("La clase de la entidad es obligatoria");
        }
        this.entityClass = entityClass;
    }

    /** Devuelve el EntityManager de la unidad de persistencia; lo implementa cada DAO concreto. */
    protected abstract EntityManager getEntityManager();

    protected Class<T> getEntityClass() {
        return entityClass;
    }

    /** EntityManager obligatorio: falla con un mensaje claro si el contenedor no lo inyecto. */
    protected EntityManager em() {
        EntityManager em = getEntityManager();
        if (em == null) {
            throw new IllegalStateException("No hay EntityManager disponible para " + entityClass.getSimpleName());
        }
        return em;
    }

    /**
     * Persiste la entidad. Con claves IDENTITY el id se asigna al sincronizar con la BD: usar {@link #flush()}
     * si se necesita antes del commit.
     *
     * @return la misma instancia, ya gestionada
     */
    public T crear(T entidad) {
        if (entidad == null) {
            throw new IllegalArgumentException("La entidad a crear no puede ser nula");
        }
        em().persist(entidad);
        return entidad;
    }

    /**
     * Actualiza una entidad existente.
     *
     * @return la instancia gestionada resultante del merge
     * @throws NoEncontradoException si no existe un registro con ese id
     */
    public T modificar(T entidad) {
        if (entidad == null) {
            throw new IllegalArgumentException("La entidad a modificar no puede ser nula");
        }
        EntityManager em = em();
        Object id = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entidad);
        if (id == null) {
            throw new IllegalArgumentException(
                    "No se puede modificar " + entityClass.getSimpleName() + " sin id (use crear)");
        }
        if (em.find(entityClass, id) == null) {
            throw new NoEncontradoException(entityClass.getSimpleName(), id);
        }
        return em.merge(entidad);
    }

    /** Busca por clave primaria. */
    public Optional<T> buscarPorId(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser nulo");
        }
        return Optional.ofNullable(em().find(entityClass, id));
    }

    /**
     * Elimina por clave primaria.
     *
     * @throws NoEncontradoException si no existe
     */
    public void eliminar(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("El id no puede ser nulo");
        }
        EntityManager em = em();
        T entidad = em.find(entityClass, id);
        if (entidad == null) {
            throw new NoEncontradoException(entityClass.getSimpleName(), id);
        }
        em.remove(entidad);
    }

    /**
     * Pagina ordenando por el atributo {@code @Id} real de la entidad (obtenido del metamodelo JPA).
     *
     * @param first posicion del primer registro (&gt;= 0)
     * @param max cantidad de registros (1..{@value #MAX_POR_PAGINA})
     */
    public List<T> findRange(int first, int max) {
        if (first < 0) {
            throw new IllegalArgumentException("first debe ser >= 0");
        }
        if (max < 1 || max > MAX_POR_PAGINA) {
            throw new IllegalArgumentException("max debe estar entre 1 y " + MAX_POR_PAGINA);
        }
        EntityManager em = em();
        EntityType<T> tipo = em.getMetamodel().entity(entityClass);
        String jpql = "SELECT e FROM " + tipo.getName() + " e ORDER BY e." + nombreAtributoId(tipo) + " ASC";
        return em.createQuery(jpql, entityClass)
                .setFirstResult(first)
                .setMaxResults(max)
                .getResultList();
    }

    /** Cantidad total de registros. */
    public long count() {
        EntityManager em = em();
        String entidad = em.getMetamodel().entity(entityClass).getName();
        return em.createQuery("SELECT COUNT(e) FROM " + entidad + " e", Long.class).getSingleResult();
    }

    /** Sincroniza el contexto de persistencia con la BD (asigna los ids IDENTITY pendientes). */
    public void flush() {
        em().flush();
    }

    // ---------------------------------------------------------------- utilidades para los DAO concretos

    /** Valida que un argumento de una consulta no sea nulo. */
    protected static void requerir(Object valor, String nombre) {
        if (valor == null) {
            throw new IllegalArgumentException("El argumento '" + nombre + "' no puede ser nulo");
        }
    }

    /** Valida que un texto de busqueda no sea nulo ni este en blanco. */
    protected static void requerirTexto(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El argumento '" + nombre + "' no puede ser nulo ni vacio");
        }
    }

    /** Primer resultado de una consulta (para campos unicos), o vacio si no hay filas. */
    protected static <R> Optional<R> primero(TypedQuery<R> consulta) {
        return consulta.setMaxResults(1).getResultList().stream().findFirst();
    }

    /** Ejecuta un COUNT y devuelve si hay al menos una fila. */
    protected static boolean hayFilas(TypedQuery<Long> consultaConteo) {
        return consultaConteo.getSingleResult() > 0;
    }

    private String nombreAtributoId(EntityType<T> tipo) {
        return tipo.getSingularAttributes().stream()
                .filter(SingularAttribute::isId)
                .map(SingularAttribute::getName)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        entityClass.getSimpleName() + " no tiene un atributo @Id simple"));
    }
}
