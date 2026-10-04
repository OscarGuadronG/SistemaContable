package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Totales con IVA y asientos automaticos de compra, venta y ajuste (puro, sin mocks). */
class AsientosTest {

    private final Cuenta inventario = Fabrica.cuenta(1, 1104);
    private final Cuenta ivaCredito = Fabrica.cuenta(2, 1105);
    private final Cuenta proveedores = Fabrica.cuenta(3, 2101, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta caja = Fabrica.cuenta(4, 1101);
    private final Cuenta ventas = Fabrica.cuenta(5, 4101, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta ivaDebito = Fabrica.cuenta(6, 2102, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta costoVentas = Fabrica.cuenta(7, 5101);
    private final Cuenta ajuste = Fabrica.cuenta(8, 5301);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal cargoDe(List<LineaAsiento> lineas, Cuenta c) {
        return lineas.stream().filter(l -> l.cuenta() == c).map(LineaAsiento::cargo).findFirst().orElseThrow();
    }

    private static BigDecimal abonoDe(List<LineaAsiento> lineas, Cuenta c) {
        return lineas.stream().filter(l -> l.cuenta() == c).map(LineaAsiento::abono).findFirst().orElseThrow();
    }

    // -------------------------------------------------------------------------------------- totales

    @Test
    void totalesCalculanIvaSobreElSubtotalRedondeado() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("100"), bd("0.13"));

        assertEquals(bd("100.00"), t.subtotal());
        assertEquals(bd("13.00"), t.iva());
        assertEquals(bd("113.00"), t.total());
    }

    @Test
    void totalesRedondeanUnaSolaVezCadaMagnitud() {
        // 10.005 -> 10.01 de subtotal; IVA = 10.01 x 0.13 = 1.3013 -> 1.30; total 11.31
        TotalesDocumento t = TotalesDocumento.calcular(bd("10.005"), bd("0.13"));

        assertEquals(bd("10.01"), t.subtotal());
        assertEquals(bd("1.30"), t.iva());
        assertEquals(bd("11.31"), t.total());
    }

    @Test
    void totalesConTasaCeroNoTienenIva() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("50"), BigDecimal.ZERO);

        assertEquals(bd("0.00"), t.iva());
        assertEquals(bd("50.00"), t.total());
    }

    // ---------------------------------------------------------------------------------------- compra

    @Test
    void compraDebitaInventarioEIvaYAcreditaProveedores() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("152"), bd("0.13"));

        List<LineaAsiento> lineas = Asientos.compra(inventario, ivaCredito, proveedores, t);

        assertEquals(3, lineas.size());
        assertEquals(bd("152.00"), cargoDe(lineas, inventario));
        assertEquals(bd("19.76"), cargoDe(lineas, ivaCredito));
        assertEquals(bd("171.76"), abonoDe(lineas, proveedores));
        ReglasPartida.validar(lineas);
    }

    @Test
    void compraSinIvaOmiteLaLineaDeIva() {
        List<LineaAsiento> lineas = Asientos.compra(inventario, ivaCredito, proveedores,
                TotalesDocumento.calcular(bd("100"), BigDecimal.ZERO));

        assertEquals(2, lineas.size());
        assertTrue(lineas.stream().noneMatch(l -> l.cuenta() == ivaCredito));
    }

    @Test
    void compraDeImporteCeroNoGeneraAsiento() {
        assertTrue(Asientos.compra(inventario, ivaCredito, proveedores,
                TotalesDocumento.calcular(BigDecimal.ZERO, bd("0.13"))).isEmpty());
    }

    // ----------------------------------------------------------------------------------------- venta

    @Test
    void ventaRegistraIngresoConIvaYCostoDeVentas() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("100"), bd("0.13"));

        List<LineaAsiento> lineas = Asientos.venta(caja, ventas, ivaDebito, costoVentas, inventario, t, bd("60.00"));

        assertEquals(5, lineas.size());
        assertEquals(bd("113.00"), cargoDe(lineas, caja));
        assertEquals(bd("100.00"), abonoDe(lineas, ventas));
        assertEquals(bd("13.00"), abonoDe(lineas, ivaDebito));
        assertEquals(bd("60.00"), cargoDe(lineas, costoVentas));
        assertEquals(bd("60.00"), abonoDe(lineas, inventario));
        ReglasPartida.validar(lineas);
    }

    @Test
    void ventaConCostoCeroOmiteLasLineasDeCosto() {
        List<LineaAsiento> lineas = Asientos.venta(caja, ventas, ivaDebito, costoVentas, inventario,
                TotalesDocumento.calcular(bd("100"), bd("0.13")), BigDecimal.ZERO);

        assertEquals(3, lineas.size());
        ReglasPartida.validar(lineas);
    }

    @Test
    void ventaGratisConCostoSoloRegistraElCosto() {
        List<LineaAsiento> lineas = Asientos.venta(caja, ventas, ivaDebito, costoVentas, inventario,
                TotalesDocumento.calcular(BigDecimal.ZERO, bd("0.13")), bd("5"));

        assertEquals(2, lineas.size());
        assertEquals(bd("5.00"), cargoDe(lineas, costoVentas));
        ReglasPartida.validar(lineas);
    }

    @Test
    void ventaSinImporteNiCostoNoGeneraAsiento() {
        assertTrue(Asientos.venta(caja, ventas, ivaDebito, costoVentas, inventario,
                TotalesDocumento.calcular(BigDecimal.ZERO, bd("0.13")), BigDecimal.ZERO).isEmpty());
    }

    // ---------------------------------------------------------------------------------------- ajuste

    @Test
    void ajusteDeEntradaDebitaInventarioYAcreditaLaCuentaDeAjuste() {
        List<LineaAsiento> lineas = Asientos.ajuste(inventario, ajuste, bd("25"), true);

        assertEquals(bd("25.00"), cargoDe(lineas, inventario));
        assertEquals(bd("25.00"), abonoDe(lineas, ajuste));
    }

    @Test
    void ajusteDeSalidaDebitaLaCuentaDeAjusteYAcreditaInventario() {
        List<LineaAsiento> lineas = Asientos.ajuste(inventario, ajuste, bd("25"), false);

        assertEquals(bd("25.00"), cargoDe(lineas, ajuste));
        assertEquals(bd("25.00"), abonoDe(lineas, inventario));
    }

    @Test
    void ajusteDeValorCeroNoGeneraAsiento() {
        assertTrue(Asientos.ajuste(inventario, ajuste, BigDecimal.ZERO, true).isEmpty());
    }

    // --------------------------------------------------------------------------------------- planilla

    private final Cuenta sueldos = Fabrica.cuenta(9, 5201);
    private final Cuenta retIsss = Fabrica.cuenta(10, 2103, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta retAfp = Fabrica.cuenta(11, 2104, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta retRenta = Fabrica.cuenta(12, 2105, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta bancos = Fabrica.cuenta(13, 1102);

    @Test
    void planillaDebitaSueldosYAcreditaRetencionesYBancosPorElLiquido() {
        List<LineaAsiento> lineas = Asientos.planilla(sueldos, retIsss, retAfp, retRenta, bancos,
                bd("1600"), bd("48"), bd("116"), bd("84.77"), bd("1351.23"));

        assertEquals(5, lineas.size());
        assertEquals(bd("1600.00"), cargoDe(lineas, sueldos));
        assertEquals(bd("48.00"), abonoDe(lineas, retIsss));
        assertEquals(bd("116.00"), abonoDe(lineas, retAfp));
        assertEquals(bd("84.77"), abonoDe(lineas, retRenta));
        assertEquals(bd("1351.23"), abonoDe(lineas, bancos));
        ReglasPartida.validar(lineas);
    }

    @Test
    void planillaSinRentaOmiteLaLineaDeRenta() {
        List<LineaAsiento> lineas = Asientos.planilla(sueldos, retIsss, retAfp, retRenta, bancos,
                bd("400"), bd("12"), bd("29"), BigDecimal.ZERO, bd("359"));

        assertEquals(4, lineas.size());
        assertTrue(lineas.stream().noneMatch(l -> l.cuenta() == retRenta));
        ReglasPartida.validar(lineas);
    }

    @Test
    void planillaSinDevengadoNoGeneraAsiento() {
        assertTrue(Asientos.planilla(sueldos, retIsss, retAfp, retRenta, bancos, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO).isEmpty());
    }
}
