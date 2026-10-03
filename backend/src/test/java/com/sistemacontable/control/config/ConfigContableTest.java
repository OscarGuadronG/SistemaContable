package com.sistemacontable.control.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ConfigContableTest {

    private final Map<String, String> entorno = new HashMap<>();
    private final Map<String, String> sistema = new HashMap<>();

    private ConfigContable crear(String... paresClaveValor) {
        Properties base = new Properties();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            base.setProperty(paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return new ConfigContable(base, entorno::get, sistema::get);
    }

    @Test
    void leeValorDelArchivoConTipos() {
        ConfigContable c = crear("cuenta.caja", "1101", "iva.tasa", "0.13", "venta.contado", "true", "x", "abc");

        assertEquals(1101, c.getInt("cuenta.caja"));
        assertEquals(1101L, c.getLong("cuenta.caja"));
        assertEquals(0, new BigDecimal("0.13").compareTo(c.getBigDecimal("iva.tasa")));
        assertTrue(c.getBoolean("venta.contado"));
        assertEquals("abc", c.getString("x"));
    }

    @Test
    void variableDeEntornoSobreescribeElArchivo() {
        ConfigContable c = crear("cuenta.caja", "1101");
        entorno.put("CUENTA_CAJA", "9999");

        assertEquals(9999, c.getInt("cuenta.caja"));
    }

    @Test
    void propiedadDeSistemaTienePrecedenciaSobreEntornoYArchivo() {
        ConfigContable c = crear("cuenta.caja", "1101");
        entorno.put("CUENTA_CAJA", "9999");
        sistema.put("cuenta.caja", "7777");

        assertEquals(7777, c.getInt("cuenta.caja"));
    }

    @Test
    void sobreescrituraVaciaSeIgnora() {
        ConfigContable c = crear("cuenta.caja", "1101");
        entorno.put("CUENTA_CAJA", "  ");

        assertEquals(1101, c.getInt("cuenta.caja"));
    }

    @Test
    void claveInexistenteLanzaExcepcionClara() {
        ConfigContable c = crear();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> c.getString("cuenta.no.existe"));
        assertTrue(ex.getMessage().contains("cuenta.no.existe"), ex.getMessage());
    }

    @Test
    void valorNoNumericoLanzaExcepcionConLaClave() {
        ConfigContable c = crear("cuenta.caja", "abc");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> c.getInt("cuenta.caja"));
        assertTrue(ex.getMessage().contains("cuenta.caja"), ex.getMessage());
        assertThrows(IllegalStateException.class, () -> c.getBigDecimal("cuenta.caja"));
    }

    @Test
    void booleanoInvalidoLanzaExcepcion() {
        ConfigContable c = crear("venta.contado", "quizas");

        assertThrows(IllegalStateException.class, () -> c.getBoolean("venta.contado"));
    }

    @Test
    void claveNulaOVaciaEsArgumentoInvalido() {
        ConfigContable c = crear();

        assertThrows(IllegalArgumentException.class, () -> c.getString(null));
        assertThrows(IllegalArgumentException.class, () -> c.getString(" "));
    }

    @Test
    void archivoRealDelClasspathTrae_losValoresPorDefecto() {
        ConfigContable c = new ConfigContable();
        c.cargar();

        assertEquals(0, new BigDecimal("0.13").compareTo(c.getBigDecimal("iva.tasa")));
        assertEquals(0, new BigDecimal("0.03").compareTo(c.getBigDecimal("isss.tasa.empleado")));
        assertEquals(240, c.getInt("horas.mes"));
        assertTrue(c.getBoolean("venta.contado"));
        assertFalse(c.getBoolean("ajuste.genera.partida"));
        for (String clave : new String[] {"cuenta.inventario", "cuenta.iva.credito", "cuenta.proveedores",
            "cuenta.caja", "cuenta.clientes", "cuenta.ventas", "cuenta.iva.debito", "cuenta.costo.ventas",
            "cuenta.sueldos", "cuenta.retenciones.isss", "cuenta.retenciones.afp", "cuenta.retenciones.renta",
            "cuenta.bancos", "cuenta.ajuste.inventario"}) {
            assertTrue(c.getInt(clave) > 0, clave);
        }
    }
}
