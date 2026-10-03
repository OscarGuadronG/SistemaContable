package com.sistemacontable.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.entity.enums.EstadoEmpleado;
import com.sistemacontable.entity.enums.EstadoPlanilla;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** Verificaciones estructurales de las 22 entidades y los 5 enums contra el esquema de la BD. */
class MapeoEntidadesTest {

    static final List<Class<? extends EntidadBase>> ENTIDADES = List.of(
            Almacen.class, Cliente.class, Compra.class, CompraDetalle.class, Cuenta.class, Departamento.class,
            Empleado.class, Existencias.class, Kardex.class, KardexDetalle.class, Lote.class, Partida.class,
            PartidaDetalle.class, Planilla.class, PlanillaDetalle.class, Producto.class, Proveedor.class,
            Tercero.class, TipoProducto.class, UnidadMedida.class, Venta.class, VentaDetalle.class);

    static List<Class<? extends EntidadBase>> entidades() {
        return ENTIDADES;
    }

    private static Field campoId(Class<?> tipo) {
        for (Class<?> c = tipo; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(Id.class)) {
                    return f;
                }
            }
        }
        return null;
    }

    @Test
    void sonVeintidosEntidades() {
        assertEquals(22, ENTIDADES.size());
    }

    @ParameterizedTest
    @MethodSource("entidades")
    void cadaEntidadTieneEntityTableEIdLongIdentity(Class<? extends EntidadBase> tipo) {
        assertTrue(tipo.isAnnotationPresent(Entity.class), tipo + " sin @Entity");
        Table tabla = tipo.getAnnotation(Table.class);
        assertNotNull(tabla, tipo + " sin @Table");
        assertEquals("public", tabla.schema());
        assertTrue(!tabla.name().isBlank());

        Field id = campoId(tipo);
        assertNotNull(id, tipo + " sin @Id");
        assertEquals(Long.class, id.getType());
        GeneratedValue gv = id.getAnnotation(GeneratedValue.class);
        assertNotNull(gv);
        assertEquals(GenerationType.IDENTITY, gv.strategy());
    }

    @ParameterizedTest
    @MethodSource("entidades")
    void todasLasRelacionesSonLazy(Class<? extends EntidadBase> tipo) {
        for (Field f : tipo.getDeclaredFields()) {
            ManyToOne rel = f.getAnnotation(ManyToOne.class);
            if (rel != null) {
                assertEquals(FetchType.LAZY, rel.fetch(), tipo.getSimpleName() + "." + f.getName());
            }
        }
    }

    @Test
    void nombresDeTablaCoincidenConElEsquema() {
        Set<String> esperadas = new TreeSet<>(Set.of("departamento", "empleado", "cuentas", "tercero", "cliente",
                "proveedor", "unidad_medida", "tipo_producto", "producto", "lote", "almacen", "existencias",
                "compra", "compra_detalle", "ventas", "ventas_detalle", "kardex", "kardex_detalle", "partida",
                "partida_detalle", "planilla", "planilla_detalle"));
        Set<String> reales = ENTIDADES.stream().map(c -> c.getAnnotation(Table.class).name())
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(esperadas, reales);
    }

    @Test
    void persistenceXmlListaExactamenteLasEntidades() throws Exception {
        for (String unidad : new String[] {"contablePU", "consolePU", "testPU"}) {
            Set<String> listadas = clasesDeUnidad(unidad);
            Set<String> esperadas = ENTIDADES.stream().map(Class::getName).collect(Collectors.toCollection(TreeSet::new));
            esperadas.add(EntidadBase.class.getName());
            assertEquals(esperadas, listadas, unidad);
        }
    }

    private static Set<String> clasesDeUnidad(String unidad) throws Exception {
        // En el classpath de pruebas hay varios persistence.xml (el principal y el de testPU): se revisan todos.
        var recursos = MapeoEntidadesTest.class.getClassLoader().getResources("META-INF/persistence.xml");
        while (recursos.hasMoreElements()) {
            try (InputStream in = recursos.nextElement().openStream()) {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
                NodeList unidades = doc.getElementsByTagName("persistence-unit");
                for (int i = 0; i < unidades.getLength(); i++) {
                    org.w3c.dom.Element u = (org.w3c.dom.Element) unidades.item(i);
                    if (unidad.equals(u.getAttribute("name"))) {
                        NodeList clases = u.getElementsByTagName("class");
                        Set<String> resultado = new TreeSet<>();
                        for (int j = 0; j < clases.getLength(); j++) {
                            resultado.add(clases.item(j).getTextContent().trim());
                        }
                        return resultado;
                    }
                }
            }
        }
        throw new AssertionError("No existe la unidad " + unidad);
    }

    @Test
    void enumsCoincidenConLasEtiquetasDePostgres() {
        assertEquals(List.of("Activo", "Inactivo", "Suspendido"), nombres(EstadoEmpleado.values()));
        assertEquals(List.of("Pendiente", "Pagado"), nombres(EstadoPlanilla.values()));
        assertEquals(List.of("Acreedora", "Deudora"), nombres(NaturalezaCuenta.values()));
        assertEquals(List.of("Entrada", "Salida", "Traslado", "Ajuste"), nombres(TipoMovimiento.values()));
        assertEquals(List.of("Diario", "Ingreso", "Egreso", "Ajuste", "Cierre"), nombres(TipoPartida.values()));
    }

    private static List<String> nombres(Enum<?>[] valores) {
        return Arrays.stream(valores).map(Enum::name).toList();
    }

    // ---- equals / hashCode ----

    @Test
    void entidadesSinIdSoloSonIgualesASiMismas() {
        Departamento a = new Departamento();
        Departamento b = new Departamento();

        assertEquals(a, a);
        assertTrue(!a.equals(b));
        assertTrue(!a.equals(null));
    }

    @Test
    void entidadesConElMismoIdSonIgualesYTienenElMismoHashCode() {
        Departamento a = new Departamento();
        a.setId(5L);
        Departamento b = new Departamento();
        b.setId(5L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void idsDistintosNoSonIguales() {
        Departamento a = new Departamento();
        a.setId(1L);
        Departamento b = new Departamento();
        b.setId(2L);

        assertTrue(!a.equals(b));
    }

    @Test
    void entidadesDeDistintaClaseConElMismoIdNoSonIguales() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        Proveedor proveedor = new Proveedor();
        proveedor.setId(1L);

        assertTrue(!cliente.equals(proveedor));
    }

    @Test
    void hashCodeNoCambiaAlAsignarElId() {
        Cuenta cuenta = new Cuenta();
        int antes = cuenta.hashCode();
        cuenta.setId(99L);

        assertEquals(antes, cuenta.hashCode());
    }

    @Test
    void toStringNoTocaRelacionesNiFallaConNulos() {
        Empleado e = new Empleado();
        e.setNombre("Ana");

        String texto = e.toString();
        assertTrue(texto.startsWith("Empleado[id=null"), texto);
        assertTrue(texto.contains("nombre=Ana"), texto);
    }

    @Test
    void valoresPorDefectoReflejanLosDefaultsDeLaBd() {
        assertEquals(EstadoEmpleado.Activo, new Empleado().getEstado());
        assertEquals(0, new Empleado().getSalario().signum());
        assertEquals(EstadoPlanilla.Pendiente, new Planilla().getEstado());
        assertEquals((short) 30, new PlanillaDetalle().getDias());
        assertEquals((short) 0, new PlanillaDetalle().getHorasxDiur());
        assertEquals(Boolean.TRUE, new Cuenta().getActivo());
        assertEquals(Boolean.FALSE, new Cuenta().getImputable());
        assertNotNull(new Compra().getFecha());
        assertNotNull(new Kardex().getFecha());
        assertEquals(0, new PartidaDetalle().getCargo().signum());
        assertEquals(0, new PartidaDetalle().getAbono().signum());
    }
}
