package com.sistemacontable.control.service;

import com.sistemacontable.entity.Cuenta;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Asientos automaticos (lineas de partida) de compra, venta y ajuste de inventario. Son funciones puras: reciben las
 * cuentas ya resueltas y los importes, y omiten las lineas de importe cero (una linea debe tener cargo o abono mayor
 * que cero). Si todos los importes son cero devuelven una lista vacia: no hay nada que contabilizar.
 */
public final class Asientos {

    private Asientos() {
    }

    /** Compra: Inventario e IVA credito fiscal al debe; Proveedores (total) al haber. */
    public static List<LineaAsiento> compra(Cuenta inventario, Cuenta ivaCredito, Cuenta proveedores,
            TotalesDocumento t) {
        List<LineaAsiento> lineas = new ArrayList<>();
        cargo(lineas, inventario, t.subtotal());
        cargo(lineas, ivaCredito, t.iva());
        abono(lineas, proveedores, t.total());
        return lineas;
    }

    /**
     * Venta: la cuenta de cobro (Caja o Clientes) al debe por el total; Ventas e IVA debito fiscal al haber; y el
     * costo de ventas al debe contra Inventario al haber por {@code costo}.
     */
    public static List<LineaAsiento> venta(Cuenta cobro, Cuenta ventas, Cuenta ivaDebito, Cuenta costoVentas,
            Cuenta inventario, TotalesDocumento t, BigDecimal costo) {
        List<LineaAsiento> lineas = new ArrayList<>();
        cargo(lineas, cobro, t.total());
        abono(lineas, ventas, t.subtotal());
        abono(lineas, ivaDebito, t.iva());
        BigDecimal costoRedondeado = Dinero.redondear(costo);
        cargo(lineas, costoVentas, costoRedondeado);
        abono(lineas, inventario, costoRedondeado);
        return lineas;
    }

    /**
     * Ajuste de inventario por {@code valor}: una entrada debita Inventario y acredita la cuenta de ajuste; una salida
     * hace lo contrario.
     */
    public static List<LineaAsiento> ajuste(Cuenta inventario, Cuenta cuentaAjuste, BigDecimal valor, boolean entrada) {
        List<LineaAsiento> lineas = new ArrayList<>();
        BigDecimal importe = Dinero.redondear(valor);
        if (entrada) {
            cargo(lineas, inventario, importe);
            abono(lineas, cuentaAjuste, importe);
        } else {
            cargo(lineas, cuentaAjuste, importe);
            abono(lineas, inventario, importe);
        }
        return lineas;
    }

    /**
     * Pago de planilla: Sueldos (devengado) al debe; retenciones de ISSS, AFP y renta, y Bancos por el liquido, al
     * haber.
     */
    public static List<LineaAsiento> planilla(Cuenta sueldos, Cuenta retencionIsss, Cuenta retencionAfp,
            Cuenta retencionRenta, Cuenta bancos, BigDecimal devengado, BigDecimal isss, BigDecimal afp,
            BigDecimal renta, BigDecimal liquido) {
        List<LineaAsiento> lineas = new ArrayList<>();
        cargo(lineas, sueldos, Dinero.redondear(devengado));
        abono(lineas, retencionIsss, Dinero.redondear(isss));
        abono(lineas, retencionAfp, Dinero.redondear(afp));
        abono(lineas, retencionRenta, Dinero.redondear(renta));
        abono(lineas, bancos, Dinero.redondear(liquido));
        return lineas;
    }

    private static void cargo(List<LineaAsiento> lineas, Cuenta cuenta, BigDecimal monto) {
        if (monto.signum() > 0) {
            lineas.add(LineaAsiento.cargo(cuenta, monto));
        }
    }

    private static void abono(List<LineaAsiento> lineas, Cuenta cuenta, BigDecimal monto) {
        if (monto.signum() > 0) {
            lineas.add(LineaAsiento.abono(cuenta, monto));
        }
    }
}
