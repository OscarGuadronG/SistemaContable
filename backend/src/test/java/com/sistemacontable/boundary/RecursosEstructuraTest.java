package com.sistemacontable.boundary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.RestApplication;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Reglas de arquitectura de la capa REST: los recursos solo hablan con servicios (nunca con DAOs ni con el
 * EntityManager), no devuelven entidades JPA, validan los cuerpos y abarcan la peticion en una transaccion para que el
 * mapeo lea las relaciones perezosas dentro de ella.
 */
class RecursosEstructuraTest {

    private static final java.nio.file.Path FUENTES = Paths.get("src", "main", "java", "com", "sistemacontable",
            "boundary");

    static List<Class<?>> recursos() {
        return ServidorEnMemoria.RECURSOS;
    }

    @Test
    void laAplicacionSigueEnApi() {
        ApplicationPath ruta = RestApplication.class.getAnnotation(ApplicationPath.class);

        assertNotNull(ruta);
        assertEquals("/api", ruta.value());
    }

    @Test
    void todoRecursoDeLaAplicacionEstaEnElServidorDePruebas() throws IOException {
        Set<String> declarados = new TreeSet<>();
        recursos().forEach(c -> declarados.add(c.getSimpleName()));
        declarados.add("HelloResource");
        Set<String> enDisco = new TreeSet<>();
        try (Stream<java.nio.file.Path> archivos = Files.list(FUENTES)) {
            archivos.map(p -> p.getFileName().toString()).filter(n -> n.endsWith("Resource.java"))
                    .forEach(n -> enDisco.add(n.substring(0, n.length() - ".java".length())));
        }

        assertEquals(enDisco, declarados, "un recurso nuevo debe anadirse a ServidorEnMemoria.RECURSOS");
    }

    @ParameterizedTest
    @MethodSource("recursos")
    void esUnRecursoJsonTransaccionalPorPeticion(Class<?> recurso) {
        assertTrue(recurso.isAnnotationPresent(Path.class), recurso + " necesita @Path");
        assertTrue(recurso.isAnnotationPresent(RequestScoped.class), recurso + " necesita @RequestScoped");
        assertTrue(recurso.isAnnotationPresent(Transactional.class),
                recurso + " necesita @Transactional para mapear a DTO dentro de la transaccion");
        assertEquals("application/json", recurso.getAnnotation(Produces.class).value()[0]);
    }

    @ParameterizedTest
    @MethodSource("recursos")
    void soloDependeDeServiciosYNuncaDeDaosNiDelEntityManager(Class<?> recurso) {
        for (Field campo : recurso.getDeclaredFields()) {
            assertTrue(campo.isAnnotationPresent(Inject.class), campo + " debe inyectarse");
            assertTrue(Modifier.isPrivate(campo.getModifiers()));
            assertEquals("com.sistemacontable.control.service", campo.getType().getPackageName(),
                    recurso.getSimpleName() + "." + campo.getName() + " debe ser un servicio");
            assertFalse(EntityManager.class.isAssignableFrom(campo.getType()));
        }
        assertFalse(recurso.getDeclaredFields().length == 0, recurso + " debe inyectar su servicio");
    }

    @ParameterizedTest
    @MethodSource("recursos")
    void elCodigoFuenteNoImportaDaosNiPersistencia(Class<?> recurso) throws IOException {
        String fuente = Files.readString(FUENTES.resolve(recurso.getSimpleName() + ".java"), StandardCharsets.UTF_8);

        assertFalse(fuente.matches("(?s).*import com\\.sistemacontable\\.control\\.[A-Za-z]*DAO;.*"),
                recurso.getSimpleName() + " no debe usar DAOs");
        assertFalse(fuente.contains("import jakarta.persistence"), recurso.getSimpleName() + " no debe usar JPA");
        assertFalse(fuente.contains("BigDecimal"), recurso.getSimpleName() + " no debe calcular importes");
        assertFalse(fuente.matches("(?s).*\\bem\\..*"), recurso.getSimpleName() + " no debe tocar el EntityManager");
    }

    @ParameterizedTest
    @MethodSource("recursos")
    void losMetodosDevuelvenResponseYNuncaEntidadesNiRecibenlas(Class<?> recurso) {
        for (Method m : recurso.getDeclaredMethods()) {
            if (!esEndpoint(m)) {
                continue;
            }
            assertEquals(Response.class, m.getReturnType(), m + " debe devolver Response");
            for (Parameter p : m.getParameters()) {
                assertFalse(p.getType().getName().startsWith("com.sistemacontable.entity."),
                        m + " no debe recibir entidades JPA");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("recursos")
    void losCuerposDeEscrituraSeValidanConValid(Class<?> recurso) {
        List<String> sinValid = new ArrayList<>();
        for (Method m : recurso.getDeclaredMethods()) {
            if (!(m.isAnnotationPresent(POST.class) || m.isAnnotationPresent(PUT.class))) {
                continue;
            }
            for (Parameter p : m.getParameters()) {
                boolean esCuerpo = p.getType().getName().startsWith("com.sistemacontable.boundary.dto.");
                if (esCuerpo && !p.isAnnotationPresent(Valid.class)) {
                    sinValid.add(m.getName() + "(" + p.getType().getSimpleName() + ")");
                }
            }
        }

        assertTrue(sinValid.isEmpty(), recurso.getSimpleName() + ": cuerpos sin @Valid: " + sinValid);
    }

    @Test
    void soloLosRecursosEsperadosEscribenYLosDeConsultaNoTienenPutNiDelete() {
        for (Class<?> consulta : List.of(KardexResource.class, ExistenciasResource.class, BalanzaResource.class)) {
            for (Method m : consulta.getDeclaredMethods()) {
                assertFalse(m.isAnnotationPresent(POST.class) || m.isAnnotationPresent(PUT.class)
                        || m.isAnnotationPresent(DELETE.class), consulta.getSimpleName() + " es de solo lectura");
            }
        }
        for (Class<?> inmutable : List.of(PartidaResource.class, CompraResource.class, VentaResource.class)) {
            for (Method m : inmutable.getDeclaredMethods()) {
                assertFalse(m.isAnnotationPresent(PUT.class) || m.isAnnotationPresent(DELETE.class),
                        inmutable.getSimpleName() + " es inmutable: sin PUT ni DELETE");
            }
        }
    }

    private static boolean esEndpoint(Method m) {
        return m.isAnnotationPresent(GET.class) || m.isAnnotationPresent(POST.class)
                || m.isAnnotationPresent(PUT.class) || m.isAnnotationPresent(DELETE.class);
    }
}
