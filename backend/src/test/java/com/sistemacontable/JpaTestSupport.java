package com.sistemacontable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Soporte para pruebas con EclipseLink + H2 en memoria (unidad {@code testPU}). Valida mapeos y JPQL reales;
 * no sustituye una prueba contra PostgreSQL (ver limitaciones en el informe de la tarea).
 */
public final class JpaTestSupport {

    private static EntityManagerFactory emf;

    private JpaTestSupport() {
    }

    /** Esquema real de la BD (fuente de verdad), relativo al directorio backend/ donde corre Maven. */
    static final Path ESQUEMA = Path.of("..", "docs", "esquema-bd.sql");

    static final String URL = "jdbc:h2:mem:contable;DB_CLOSE_DELAY=-1;MODE=LEGACY";

    public static synchronized EntityManagerFactory emf() {
        if (emf == null) {
            crearEsquema();
            emf = Persistence.createEntityManagerFactory("testPU");
        }
        return emf;
    }

    /**
     * Ejecuta docs/esquema-bd.sql sobre H2 con las adaptaciones minimas: los enums de Postgres pasan a varchar,
     * {@code numeric} a {@code decfloat} (sin escala fija, como numeric sin precision en Postgres) y se quita el
     * cast {@code ::numeric}.
     */
    static String adaptarEsquema(String sql) {
        return sql.replaceAll("(?m)^\\s*--.*$", "")
                .replace("::numeric", "")
                .replaceAll("\\bnumeric\\b", "decfloat")
                .replaceAll("\\b(estado_empleado|estado_planilla|naturaleza_cuenta|tipo_movimiento|tipo_partida)\\b",
                        "varchar")
                .replace("DEFAULT now()", "DEFAULT CURRENT_TIMESTAMP");
    }

    private static void crearEsquema() {
        try {
            String sql = adaptarEsquema(Files.readString(ESQUEMA, StandardCharsets.UTF_8));
            try (Connection con = DriverManager.getConnection(URL, "sa", "");
                    Statement st = con.createStatement()) {
                for (String sentencia : sql.split(";")) {
                    if (!sentencia.isBlank()) {
                        st.execute(sentencia);
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("No se pudo crear el esquema de pruebas desde " + ESQUEMA.toAbsolutePath(), e);
        }
    }

    /** Crea un EntityManager con una transaccion abierta; el llamador debe hacer rollback y cerrar. */
    public static EntityManager nuevoEmConTransaccion() {
        EntityManager em = emf().createEntityManager();
        em.getTransaction().begin();
        return em;
    }

    public static void cerrarConRollback(EntityManager em) {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
        em.close();
    }

    /** Inyecta el EntityManager en el campo privado {@code em} de un DAO (lo que haria el contenedor). */
    public static void inyectarEm(Object dao, EntityManager em) {
        try {
            Field campo = dao.getClass().getDeclaredField("em");
            campo.setAccessible(true);
            campo.set(dao, em);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo inyectar el EntityManager en " + dao.getClass(), e);
        }
    }
}
