package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.control.PartidaDetalleDAO.SumaCuenta;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Agregacion jerarquica y saldos de la balanza de comprobacion (puro, sin mocks). */
class CalculoBalanzaTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 1, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 1, 31);

    private final Cuenta activo = cuenta(1, 1, NaturalezaCuenta.Deudora, null);
    private final Cuenta caja = cuenta(2, 11, NaturalezaCuenta.Deudora, activo);
    private final Cuenta bancos = cuenta(3, 12, NaturalezaCuenta.Deudora, activo);
    private final Cuenta ingresos = cuenta(4, 4, NaturalezaCuenta.Acreedora, null);
    private final Cuenta ventas = cuenta(5, 41, NaturalezaCuenta.Acreedora, ingresos);
    private final List<Cuenta> catalogo = List.of(activo, ingresos, caja, bancos, ventas);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static Cuenta cuenta(long id, int codigo, NaturalezaCuenta naturaleza, Cuenta padre) {
        Cuenta c = Fabrica.cuenta(id, codigo, naturaleza, true, padre != null);
        c.setPadre(padre);
        return c;
    }

    private static SumaCuenta suma(Cuenta c, String cargo, String abono) {
        return new SumaCuenta(c.getId(), bd(cargo), bd(abono));
    }

    private FilaBalanza fila(Balanza b, int codigo) {
        return b.filas().stream().filter(f -> f.codigo() == codigo).findFirst().orElseThrow();
    }

    @Test
    void agregaLasHijasALaCuentaPadreYCalculaSaldoSegunNaturaleza() {
        Balanza b = CalculoBalanza.calcular(DESDE, HASTA, catalogo, List.of(
                suma(caja, "150", "20"), suma(bancos, "50", "0"), suma(ventas, "0", "180")));

        FilaBalanza padre = fila(b, 1);
        assertEquals(bd("200.00"), padre.cargos());
        assertEquals(bd("20.00"), padre.abonos());
        assertEquals(bd("180.00"), padre.saldo());
        assertFalse(padre.imputable());

        FilaBalanza hoja = fila(b, 11);
        assertEquals(bd("130.00"), hoja.saldo());
        assertTrue(hoja.imputable());

        FilaBalanza acreedora = fila(b, 41);
        assertEquals(bd("180.00"), acreedora.saldo());
        assertEquals(bd("180.00"), fila(b, 4).saldo());
    }

    @Test
    void losTotalesSumanSoloLosMovimientosDirectosSinDobleConteo() {
        Balanza b = CalculoBalanza.calcular(DESDE, HASTA, catalogo, List.of(
                suma(caja, "150", "20"), suma(bancos, "50", "0"), suma(ventas, "0", "180")));

        assertEquals(bd("200.00"), b.totalCargos());
        assertEquals(bd("200.00"), b.totalAbonos());
        assertTrue(b.cuadra());
        assertEquals(0, BigDecimal.ZERO.compareTo(b.diferencia()));
    }

    @Test
    void filasOrdenadasPorCodigoYSoloConMovimientos() {
        Balanza b = CalculoBalanza.calcular(DESDE, HASTA, catalogo, List.of(suma(caja, "10", "0"), suma(ventas, "0", "10")));

        assertEquals(List.of(1, 4, 11, 41), b.filas().stream().map(FilaBalanza::codigo).toList());
    }

    @Test
    void siLosTotalesNoCoincidenSeMarcaDescuadradaConLaDiferencia() {
        Balanza b = CalculoBalanza.calcular(DESDE, HASTA, catalogo, List.of(suma(caja, "100", "0"), suma(ventas, "0", "90")));

        assertFalse(b.cuadra());
        assertEquals(bd("10.00"), b.diferencia());
    }

    @Test
    void unaCuentaConSaldoContrarioALaNaturalezaMuestraSaldoNegativo() {
        Balanza b = CalculoBalanza.calcular(DESDE, HASTA, catalogo, List.of(suma(ventas, "30", "10"), suma(caja, "0", "20")));

        assertEquals(bd("-20.00"), fila(b, 41).saldo());
        assertEquals(bd("-20.00"), fila(b, 11).saldo());
    }

    @Test
    void sinMovimientosNoHayFilasYCuadra() {
        Balanza b = CalculoBalanza.calcular(DESDE, HASTA, catalogo, List.of());

        assertTrue(b.filas().isEmpty());
        assertEquals(bd("0.00"), b.totalCargos());
        assertTrue(b.cuadra());
    }

    @Test
    void unaCuentaConMovimientosQueSeCancelanSigueApareciendo() {
        Balanza b = CalculoBalanza.calcular(DESDE, HASTA, catalogo, List.of(suma(caja, "10", "10"), suma(ventas, "10", "10")));

        assertEquals(bd("0.00"), fila(b, 11).saldo());
    }

    @Test
    void cuentaConMovimientosAusenteDelCatalogoEsError() {
        assertThrows(NoEncontradoException.class, () -> CalculoBalanza.calcular(DESDE, HASTA, catalogo,
                List.of(new SumaCuenta(999L, bd("1"), bd("1")))));
    }
}
