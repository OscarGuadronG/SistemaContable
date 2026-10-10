package com.sistemacontable.control.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Properties;

/**
 * Fabrica de {@link ConfigContable} para pruebas: carga el {@code contabilidad.properties} real del classpath
 * pero ignora variables de entorno y propiedades de sistema, de modo que los resultados no dependen de la
 * maquina donde corren. Vive en el mismo paquete que {@link ConfigContable} para usar su constructor de pruebas.
 */
public final class ConfigPruebas {

    private ConfigPruebas() {
    }

    /** Configuracion con los valores por defecto del archivo. */
    public static ConfigContable porDefecto() {
        return con(Map.of());
    }

    /** Configuracion por defecto con algunas claves sobreescritas. */
    public static ConfigContable con(Map<String, String> sobreescrituras) {
        Properties base = new Properties();
        try (InputStream in = ConfigContable.class.getClassLoader().getResourceAsStream(ConfigContable.RECURSO)) {
            base.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        base.putAll(sobreescrituras);
        return new ConfigContable(base, clave -> null, clave -> null);
    }
}
