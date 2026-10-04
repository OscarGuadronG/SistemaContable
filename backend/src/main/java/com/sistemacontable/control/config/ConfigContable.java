package com.sistemacontable.control.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;

/**
 * Parametros contables (codigos de cuenta y tasas) leidos de {@code contabilidad.properties}.
 *
 * <p>Precedencia, de mayor a menor: propiedad de sistema Java con el nombre de la clave, variable de entorno
 * (clave en mayusculas con los puntos cambiados por guion bajo; {@code cuenta.iva.credito} ->
 * {@code CUENTA_IVA_CREDITO}) y, por ultimo, el archivo del classpath.</p>
 *
 * <p>Los valores del archivo son de referencia y deben verificarse (ver comentarios del archivo).</p>
 */
@ApplicationScoped
public class ConfigContable {

    public static final String RECURSO = "contabilidad.properties";

    // Claves usadas por los servicios (evita errores de tipeo).
    public static final String CUENTA_INVENTARIO = "cuenta.inventario";
    public static final String CUENTA_IVA_CREDITO = "cuenta.iva.credito";
    public static final String CUENTA_PROVEEDORES = "cuenta.proveedores";
    public static final String CUENTA_CAJA = "cuenta.caja";
    public static final String CUENTA_CLIENTES = "cuenta.clientes";
    public static final String CUENTA_VENTAS = "cuenta.ventas";
    public static final String CUENTA_IVA_DEBITO = "cuenta.iva.debito";
    public static final String CUENTA_COSTO_VENTAS = "cuenta.costo.ventas";
    public static final String CUENTA_SUELDOS = "cuenta.sueldos";
    public static final String CUENTA_RETENCIONES_ISSS = "cuenta.retenciones.isss";
    public static final String CUENTA_RETENCIONES_AFP = "cuenta.retenciones.afp";
    public static final String CUENTA_RETENCIONES_RENTA = "cuenta.retenciones.renta";
    public static final String CUENTA_BANCOS = "cuenta.bancos";
    public static final String CUENTA_AJUSTE_INVENTARIO = "cuenta.ajuste.inventario";
    public static final String VENTA_CONTADO = "venta.contado";
    public static final String AJUSTE_GENERA_PARTIDA = "ajuste.genera.partida";
    public static final String IVA_TASA = "iva.tasa";
    public static final String ISSS_TASA_EMPLEADO = "isss.tasa.empleado";
    public static final String ISSS_TECHO = "isss.techo";
    public static final String AFP_TASA_EMPLEADO = "afp.tasa.empleado";
    public static final String HORA_EXTRA_DIURNA_RECARGO = "hora.extra.diurna.recargo";
    public static final String HORA_EXTRA_NOCTURNA_RECARGO = "hora.extra.nocturna.recargo";
    public static final String HORAS_MES = "horas.mes";
    public static final String DIAS_MES = "dias.mes";

    private Properties base = new Properties();
    private Function<String, String> lectorEntorno = System::getenv;
    private Function<String, String> lectorSistema = System::getProperty;

    /** Constructor requerido por CDI; los valores se cargan en {@link #cargar()}. */
    public ConfigContable() {
    }

    /** Constructor para pruebas: permite inyectar el contenido base y las fuentes de sobreescritura. */
    ConfigContable(Properties base, Function<String, String> lectorEntorno, Function<String, String> lectorSistema) {
        this.base = base;
        this.lectorEntorno = lectorEntorno;
        this.lectorSistema = lectorSistema;
    }

    @PostConstruct
    void cargar() {
        Properties cargadas = new Properties();
        try (InputStream in = ConfigContable.class.getClassLoader().getResourceAsStream(RECURSO)) {
            if (in == null) {
                throw new IllegalStateException("No se encontro " + RECURSO + " en el classpath");
            }
            cargadas.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + RECURSO, e);
        }
        this.base = cargadas;
    }

    /** Indica si la clave tiene un valor no vacio en alguna fuente (sistema, entorno o archivo). */
    public boolean tiene(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave de configuracion es obligatoria");
        }
        String valor = valorSobreescrito(clave);
        if (valor == null) {
            valor = base.getProperty(clave);
        }
        return valor != null && !valor.isBlank();
    }

    public String getString(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new IllegalArgumentException("La clave de configuracion es obligatoria");
        }
        String valor = valorSobreescrito(clave);
        if (valor == null) {
            valor = base.getProperty(clave);
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta la configuracion contable '" + clave + "' (en " + RECURSO
                    + ", variable de entorno " + nombreEntorno(clave) + " o propiedad de sistema '" + clave + "')");
        }
        return valor.trim();
    }

    public int getInt(String clave) {
        String valor = getString(clave);
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw valorInvalido(clave, valor, "entero", e);
        }
    }

    public long getLong(String clave) {
        String valor = getString(clave);
        try {
            return Long.parseLong(valor);
        } catch (NumberFormatException e) {
            throw valorInvalido(clave, valor, "entero largo", e);
        }
    }

    public BigDecimal getBigDecimal(String clave) {
        String valor = getString(clave);
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            throw valorInvalido(clave, valor, "decimal", e);
        }
    }

    public boolean getBoolean(String clave) {
        String valor = getString(clave);
        if ("true".equalsIgnoreCase(valor)) {
            return true;
        }
        if ("false".equalsIgnoreCase(valor)) {
            return false;
        }
        throw valorInvalido(clave, valor, "booleano (true/false)", null);
    }

    private String valorSobreescrito(String clave) {
        String valor = lectorSistema.apply(clave);
        if (valor == null || valor.isBlank()) {
            valor = lectorEntorno.apply(nombreEntorno(clave));
        }
        return (valor == null || valor.isBlank()) ? null : valor;
    }

    static String nombreEntorno(String clave) {
        return clave.replace('.', '_').toUpperCase(Locale.ROOT);
    }

    private static IllegalStateException valorInvalido(String clave, String valor, String tipo, Throwable causa) {
        return new IllegalStateException(
                "La configuracion contable '" + clave + "' tiene el valor '" + valor + "', que no es " + tipo, causa);
    }
}
