package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reglas puras de la partida doble (sin mocks ni base de datos). */
class ReglasPartidaTest {

    private final Cuenta caja = Fabrica.cuenta(1, 1101);
    private final Cuenta ventas = Fabrica.cuenta(2, 4101, NaturalezaCuenta.Acreedora, true, true);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private ReglaNegocioException rechaza(List<LineaAsiento> lineas) {
        return assertThrows(ReglaNegocioException.class, () -> ReglasPartida.validar(lineas));
    }

    @Test
    void aceptaPartidaCuadradaDeDosLineas() {
        assertDoesNotThrow(() -> ReglasPartida.validar(
                List.of(LineaAsiento.cargo(caja, bd("100.00")), LineaAsiento.abono(ventas, bd("100.00")))));
    }

    @Test
    void aceptaPartidaCuadradaConVariasLineas() {
        assertDoesNotThrow(() -> ReglasPartida.validar(List.of(
                LineaAsiento.cargo(caja, bd("113.00")),
                LineaAsiento.abono(ventas, bd("100.00")),
                LineaAsiento.abono(ventas, bd("13.00")))));
    }

    @Test
    void comparaConCompareToYNoConEquals() {
        // 100.0 y 100.00 son iguales para compareTo aunque equals diga que no
        assertDoesNotThrow(() -> ReglasPartida.validar(
                List.of(LineaAsiento.cargo(caja, bd("100.0")), LineaAsiento.abono(ventas, bd("100.00")))));
    }

    @Test
    void rechazaListaNulaOConMenosDeDosLineas() {
        assertTrue(rechaza(null).getMessage().contains("al menos dos"));
        assertTrue(rechaza(List.of()).getMessage().contains("al menos dos"));
        assertTrue(rechaza(List.of(LineaAsiento.cargo(caja, bd("1")))).getMessage().contains("al menos dos"));
    }

    @Test
    void rechazaLineaConCargoYAbonoAlMismoTiempo() {
        ReglaNegocioException ex = rechaza(List.of(
                new LineaAsiento(caja, bd("5"), bd("5")), LineaAsiento.abono(ventas, bd("5"))));
        assertTrue(ex.getMessage().contains("linea 1") && ex.getMessage().contains("cargo y abono"), ex.getMessage());
    }

    @Test
    void rechazaLineaSinImporte() {
        ReglaNegocioException ex = rechaza(List.of(
                LineaAsiento.cargo(caja, bd("5")), new LineaAsiento(ventas, BigDecimal.ZERO, BigDecimal.ZERO)));
        assertTrue(ex.getMessage().contains("linea 2") && ex.getMessage().contains("mayor que cero"), ex.getMessage());
    }

    @Test
    void rechazaImportesNegativosONulos() {
        assertTrue(rechaza(List.of(new LineaAsiento(caja, bd("-5"), BigDecimal.ZERO),
                LineaAsiento.abono(ventas, bd("5")))).getMessage().contains("negativo"));
        assertTrue(rechaza(List.of(new LineaAsiento(caja, null, BigDecimal.ZERO),
                LineaAsiento.abono(ventas, bd("5")))).getMessage().contains("nulo"));
    }

    @Test
    void rechazaLineaSinCuentaOLineaNula() {
        assertTrue(rechaza(List.of(LineaAsiento.cargo(null, bd("5")), LineaAsiento.abono(ventas, bd("5"))))
                .getMessage().contains("cuenta"));
        java.util.ArrayList<LineaAsiento> conNula = new java.util.ArrayList<>();
        conNula.add(LineaAsiento.cargo(caja, bd("5")));
        conNula.add(null);
        assertTrue(rechaza(conNula).getMessage().contains("linea 2"));
    }

    @Test
    void rechazaCuentaInactiva() {
        Cuenta inactiva = Fabrica.cuenta(3, 1190, NaturalezaCuenta.Deudora, false, true);
        ReglaNegocioException ex = rechaza(List.of(
                LineaAsiento.cargo(inactiva, bd("5")), LineaAsiento.abono(ventas, bd("5"))));
        assertTrue(ex.getMessage().contains("1190") && ex.getMessage().contains("activa"), ex.getMessage());
    }

    @Test
    void rechazaCuentaNoImputable() {
        Cuenta grupo = Fabrica.cuenta(3, 1100, NaturalezaCuenta.Deudora, true, false);
        ReglaNegocioException ex = rechaza(List.of(
                LineaAsiento.cargo(grupo, bd("5")), LineaAsiento.abono(ventas, bd("5"))));
        assertTrue(ex.getMessage().contains("1100") && ex.getMessage().contains("imputable"), ex.getMessage());
    }

    @Test
    void rechazaPartidaDescuadradaIndicandoLasSumas() {
        ReglaNegocioException ex = rechaza(List.of(
                LineaAsiento.cargo(caja, bd("100.00")), LineaAsiento.abono(ventas, bd("99.99"))));
        assertTrue(ex.getMessage().contains("descuadrada") && ex.getMessage().contains("100.00")
                && ex.getMessage().contains("99.99"), ex.getMessage());
    }

    @Test
    void validarCuentaImputableNombraLaEtiquetaRecibida() {
        Cuenta inactiva = Fabrica.cuenta(3, 1190, NaturalezaCuenta.Deudora, false, true);
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> ReglasPartida.validarCuentaImputable(inactiva, "'cuenta.caja'"));
        assertTrue(ex.getMessage().contains("'cuenta.caja'"), ex.getMessage());
    }
}
