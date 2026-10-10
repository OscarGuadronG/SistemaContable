package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Estructura de la capa de servicios: EJB sin estado con dependencias inyectadas (sin dependencias muertas ni
 * EntityManager propio), comandos inmutables sin entidades JPA y DAOs sin conocimiento de los servicios.
 */
class EstructuraServiciosTest {

    private static final Path FUENTES = Path.of("src", "main", "java", "com", "sistemacontable", "control");

    static List<Class<?>> servicios() {
        return List.of(PartidaService.class, CuentaService.class, BalanzaService.class, ExistenciasService.class,
                KardexService.class, CompraService.class, VentaService.class, TrasladoService.class,
                AjusteService.class, PlanillaService.class, EmpleadoService.class, DepartamentoService.class,
                ClienteService.class, ProveedorService.class, ProductoService.class, LoteService.class,
                AlmacenService.class, TipoProductoService.class, TerceroService.class, UnidadMedidaService.class);
    }

    @ParameterizedTest
    @MethodSource("servicios")
    void esUnEjbSinEstadoConVistaSinInterfaz(Class<?> servicio) {
        assertTrue(servicio.isAnnotationPresent(Stateless.class), servicio + " debe ser @Stateless");
        assertTrue(servicio.isAnnotationPresent(LocalBean.class), servicio + " debe ser @LocalBean");
        assertTrue(Modifier.isPublic(servicio.getModifiers()));
        assertDoesNotThrowNoArgConstructor(servicio);
    }

    private static void assertDoesNotThrowNoArgConstructor(Class<?> servicio) {
        try {
            assertNotNull(servicio.getDeclaredConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(servicio + " necesita un constructor sin argumentos", e);
        }
    }

    @ParameterizedTest
    @MethodSource("servicios")
    void todasSusDependenciasSonCamposInjectPrivadosYNoManejanElEntityManager(Class<?> servicio) {
        for (Field campo : servicio.getDeclaredFields()) {
            if (Modifier.isStatic(campo.getModifiers())) {
                continue;
            }
            assertTrue(campo.isAnnotationPresent(Inject.class), servicio.getSimpleName() + "." + campo.getName()
                    + " debe inyectarse con @Inject (los servicios no guardan estado)");
            assertTrue(Modifier.isPrivate(campo.getModifiers()), campo.getName() + " debe ser privado");
            assertFalse(Modifier.isFinal(campo.getModifiers()), campo.getName() + " no puede ser final");
            assertFalse(campo.isAnnotationPresent(PersistenceContext.class));
            assertFalse(EntityManager.class.isAssignableFrom(campo.getType()),
                    "los servicios acceden a datos solo mediante DAOs");
        }
    }

    @ParameterizedTest
    @MethodSource("servicios")
    void ningunaDependenciaInyectadaQuedaSinUsar(Class<?> servicio) throws IOException {
        String fuente = Files.readString(FUENTES.resolve("service").resolve(servicio.getSimpleName() + ".java"),
                StandardCharsets.UTF_8);
        for (Field campo : servicio.getDeclaredFields()) {
            if (campo.isAnnotationPresent(Inject.class)) {
                // una aparicion es la declaracion del campo; debe haber al menos otra (llamada o paso como argumento)
                int apariciones = fuente.split("\\b" + campo.getName() + "\\b", -1).length - 1;
                assertTrue(apariciones > 1, servicio.getSimpleName() + "." + campo.getName()
                        + " esta inyectado y no se usa");
            }
        }
    }

    // ------------------------------------------------------------------------------------ comandos

    static Stream<Class<?>> comandos() throws IOException, ClassNotFoundException {
        Path carpeta = Path.of("target", "classes", "com", "sistemacontable", "control", "service", "cmd");
        List<Class<?>> clases = new java.util.ArrayList<>();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            for (String nombre : archivos.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".class"))
                    .map(n -> n.substring(0, n.length() - ".class".length())).sorted().toList()) {
                clases.add(Class.forName("com.sistemacontable.control.service.cmd." + nombre));
            }
        }
        return clases.stream();
    }

    @Test
    void hayComandosParaCadaServicioDeEscritura() throws Exception {
        List<String> nombres = comandos().map(Class::getSimpleName).toList();

        for (String esperado : List.of("PartidaCmd", "CuentaCmd", "CompraCmd", "VentaCmd", "TrasladoCmd", "AjusteCmd",
                "PlanillaCmd", "DetallePlanillaCmd", "EmpleadoCmd", "ClienteCmd", "ProveedorCmd", "LoteCmd")) {
            assertTrue(nombres.contains(esperado), "falta el comando " + esperado);
        }
    }

    @ParameterizedTest
    @MethodSource("comandos")
    void losComandosSonRecordsSinEntidadesJpa(Class<?> comando) {
        assertTrue(comando.isRecord(), comando + " debe ser un record inmutable");
        for (RecordComponent c : comando.getRecordComponents()) {
            for (Class<?> tipo : tiposDe(c.getGenericType())) {
                assertFalse(tipo.isAnnotationPresent(Entity.class),
                        comando.getSimpleName() + "." + c.getName() + " no debe usar la entidad JPA " + tipo);
            }
        }
    }

    private static List<Class<?>> tiposDe(Type tipo) {
        if (tipo instanceof Class<?> c) {
            return List.of(c);
        }
        if (tipo instanceof ParameterizedType p) {
            return Stream.concat(Stream.of((Class<?>) p.getRawType()),
                    Stream.of(p.getActualTypeArguments()).flatMap(t -> tiposDe(t).stream())).toList();
        }
        return List.of();
    }

    // ----------------------------------------------------------------------------------------- DAOs

    @Test
    void losDaoNoConocenALosServicios() throws IOException {
        try (Stream<Path> archivos = Files.list(FUENTES)) {
            List<Path> daos = archivos.filter(p -> p.getFileName().toString().endsWith(".java")).toList();
            assertFalse(daos.isEmpty());
            for (Path dao : daos) {
                String fuente = Files.readString(dao, StandardCharsets.UTF_8);
                assertFalse(fuente.contains("control.service"), dao.getFileName() + " no debe depender de los servicios");
                assertEquals(-1, fuente.indexOf("ReglaNegocioException("), dao.getFileName()
                        + " no debe lanzar excepciones de negocio");
            }
        }
    }
}
