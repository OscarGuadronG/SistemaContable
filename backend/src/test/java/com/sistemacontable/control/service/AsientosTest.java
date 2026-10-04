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
    private final Cuenta clientes = Fabrica.cuenta(14, 1103);
    private final Cuenta ventas = Fabrica.cuenta(5, 4101, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta ivaDebito = Fabrica.cuenta(6, 2102, NaturalezaCuenta.Acreedora, true, true);
    private final Cuenta costoVentas = Fabrica.cuenta(7, 5101);
    private final Cuenta ajuste = Fabrica.cuenta(8, 5301);

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static PagoDocumento pago(TotalesDocumento t, String porcentajeContado) {
        return PagoDocumento.dividir(t.total(), bd(porcentajeContado));
    }

    private static BigDecimal sumaCargos(List<LineaAsiento> lineas) {
        return lineas.stream().map(LineaAsiento::cargo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumaAbonos(List<LineaAsiento> lineas) {
        return lineas.stream().map(LineaAsiento::abono).reduce(BigDecimal.ZERO, BigDecimal::add);
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

        List<LineaAsiento> lineas = Asientos.compra(inventario, ivaCredito, caja, proveedores, t, pago(t, "0"));

        assertEquals(3, lineas.size());
        assertEquals(bd("152.00"), cargoDe(lineas, inventario));
        assertEquals(bd("19.76"), cargoDe(lineas, ivaCredito));
        assertEquals(bd("171.76"), abonoDe(lineas, proveedores));
        ReglasPartida.validar(lineas);
    }

    @Test
    void compraSinIvaOmiteLaLineaDeIva() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("100"), BigDecimal.ZERO);
        List<LineaAsiento> lineas = Asientos.compra(inventario, ivaCredito, caja, proveedores, t, pago(t, "0"));

        assertEquals(2, lineas.size());
        assertTrue(lineas.stream().noneMatch(l -> l.cuenta() == ivaCredito));
    }

    @Test
    void compraDeImporteCeroNoGeneraAsiento() {
        TotalesDocumento t = TotalesDocumento.calcular(BigDecimal.ZERO, bd("0.13"));

        assertTrue(Asientos.compra(inventario, ivaCredito, caja, proveedores, t, pago(t, "40")).isEmpty());
    }

    @Test
    void compraAlContadoAcreditaCajaYNoGeneraLineaDeProveedores() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("152"), bd("0.13"));

        // la cuenta de proveedores no se necesita: puede ser nula
        List<LineaAsiento> lineas = Asientos.compra(inventario, ivaCredito, caja, null, t, pago(t, "100"));

        assertEquals(3, lineas.size());
        assertEquals(bd("171.76"), abonoDe(lineas, caja));
        assertTrue(lineas.stream().allMatch(l -> l.cargo().signum() > 0 || l.abono().signum() > 0));
        ReglasPartida.validar(lineas);
    }

    @Test
    void compraMixta40_60RepartePagoEntreCajaYProveedores() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("152"), bd("0.13")); // total 171.76

        List<LineaAsiento> lineas = Asientos.compra(inventario, ivaCredito, caja, proveedores, t, pago(t, "40"));

        assertEquals(4, lineas.size());
        assertEquals(bd("68.70"), abonoDe(lineas, caja));
        assertEquals(bd("103.06"), abonoDe(lineas, proveedores));
        ReglasPartida.validar(lineas);
    }

    @Test
    void compraConPorcentajeQueNoDivideExactoCuadraAlCentavo() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("152"), bd("0.13")); // total 171.76

        List<LineaAsiento> lineas = Asientos.compra(inventario, ivaCredito, caja, proveedores, t, pago(t, "33.33"));

        // contado = 171.76 x 0.3333 = 57.2476 -> 57.25 ; credito = 114.51
        assertEquals(bd("57.25"), abonoDe(lineas, caja));
        assertEquals(bd("114.51"), abonoDe(lineas, proveedores));
        assertEquals(0, sumaCargos(lineas).compareTo(sumaAbonos(lineas)));
        ReglasPartida.validar(lineas);
    }

    // ----------------------------------------------------------------------------------------- venta

    @Test
    void ventaRegistraIngresoConIvaYCostoDeVentas() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("100"), bd("0.13"));

        List<LineaAsiento> lineas = Asientos.venta(caja, clientes, ventas, ivaDebito, costoVentas, inventario, t,
                pago(t, "100"), bd("60.00"));

        assertEquals(5, lineas.size());
        assertEquals(bd("113.00"), cargoDe(lineas, caja));
        assertTrue(lineas.stream().noneMatch(l -> l.cuenta() == clientes));
        assertEquals(bd("100.00"), abonoDe(lineas, ventas));
        assertEquals(bd("13.00"), abonoDe(lineas, ivaDebito));
        assertEquals(bd("60.00"), cargoDe(lineas, costoVentas));
        assertEquals(bd("60.00"), abonoDe(lineas, inventario));
        ReglasPartida.validar(lineas);
    }

    @Test
    void ventaConCostoCeroOmiteLasLineasDeCosto() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("100"), bd("0.13"));
        List<LineaAsiento> lineas = Asientos.venta(caja, clientes, ventas, ivaDebito, costoVentas, inventario, t,
                pago(t, "100"), BigDecimal.ZERO);

        assertEquals(3, lineas.size());
        ReglasPartida.validar(lineas);
    }

    @Test
    void ventaGratisConCostoSoloRegistraElCosto() {
        TotalesDocumento t = TotalesDocumento.calcular(BigDecimal.ZERO, bd("0.13"));
        List<LineaAsiento> lineas = Asientos.venta(caja, clientes, ventas, ivaDebito, costoVentas, inventario, t,
                pago(t, "100"), bd("5"));

        assertEquals(2, lineas.size());
        assertEquals(bd("5.00"), cargoDe(lineas, costoVentas));
        ReglasPartida.validar(lineas);
    }

    @Test
    void ventaSinImporteNiCostoNoGeneraAsiento() {
        TotalesDocumento t = TotalesDocumento.calcular(BigDecimal.ZERO, bd("0.13"));

        assertTrue(Asientos.venta(caja, clientes, ventas, ivaDebito, costoVentas, inventario, t, pago(t, "0"),
                BigDecimal.ZERO).isEmpty());
    }

    @Test
    void ventaALaCreditoDebitaClientesYNoGeneraLineaDeCaja() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("100"), bd("0.13"));

        // la cuenta de caja no se necesita: puede ser nula
        List<LineaAsiento> lineas = Asientos.venta(null, clientes, ventas, ivaDebito, costoVentas, inventario, t,
                pago(t, "0"), bd("60.00"));

        assertEquals(5, lineas.size());
        assertEquals(bd("113.00"), cargoDe(lineas, clientes));
        ReglasPartida.validar(lineas);
    }

    @Test
    void ventaMixta40_60DebitaCajaYClientes() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("110"), bd("0.13")); // total 124.30

        List<LineaAsiento> lineas = Asientos.venta(caja, clientes, ventas, ivaDebito, costoVentas, inventario, t,
                pago(t, "40"), bd("52.00"));

        assertEquals(6, lineas.size());
        assertEquals(bd("49.72"), cargoDe(lineas, caja));
        assertEquals(bd("74.58"), cargoDe(lineas, clientes));
        assertEquals(bd("110.00"), abonoDe(lineas, ventas));
        assertEquals(bd("14.30"), abonoDe(lineas, ivaDebito));
        ReglasPartida.validar(lineas);
    }

    @Test
    void ventaConPorcentajeQueNoDivideExactoCuadraAlCentavo() {
        TotalesDocumento t = TotalesDocumento.calcular(bd("100.05"), bd("0.13")); // IVA 13.01 ; total 113.06

        List<LineaAsiento> lineas = Asientos.venta(caja, clientes, ventas, ivaDebito, costoVentas, inventario, t,
                pago(t, "33.33"), bd("60.00"));

        // contado = 113.06 x 0.3333 = 37.6829 -> 37.68 ; credito = 75.38
        assertEquals(bd("37.68"), cargoDe(lineas, caja));
        assertEquals(bd("75.38"), cargoDe(lineas, clientes));
        assertEquals(0, sumaCargos(lineas).compareTo(sumaAbonos(lineas)));
        assertTrue(lineas.stream().allMatch(l -> l.cargo().signum() > 0 || l.abono().signum() > 0));
        ReglasPartida.validar(lineas);
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
