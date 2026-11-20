package com.sistemacontable.control.service;

import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Asientos automaticos (lineas de partida) de compra, venta y ajuste de inventario. Son funciones puras: reciben las
 * cuentas ya resueltas y los importes, y omiten las lineas de importe cero (una linea debe tener cargo o abono mayor
 * que cero). Si todos los importes son cero devuelven una lista vacia: no hay nada que contabilizar.
 *
 * <p>Las cuentas de pago (caja, proveedores, clientes) solo se usan cuando su linea se genera: si el reparto
 * contado/credito deja una de ellas en cero, esa cuenta puede ser {@code null}.</p>
 */
public final class Asientos {

    private Asientos() {
    }

    public static List<LineaAsiento> compraCompleta(Cuenta inventario, Cuenta ivaCredito, Cuenta descuentos,
            Cuenta caja, Cuenta bancos, Cuenta proveedores, FormaPago formaPago, BigDecimal subtotal,
            BigDecimal descuento, BigDecimal gastos, BigDecimal iva, BigDecimal total) {
        List<LineaAsiento> lineas = new ArrayList<>();
        cargo(lineas, inventario, subtotal.add(gastos));
        cargo(lineas, ivaCredito, iva);
        abono(lineas, descuentos, descuento);
        abono(lineas, cuentaPago(formaPago, caja, bancos, proveedores), total);
        return lineas;
    }

    public static List<LineaAsiento> ventaCompleta(Cuenta caja, Cuenta bancos, Cuenta clientes, Cuenta rebajas,
            Cuenta ventas, Cuenta ivaDebito, Cuenta costoVentas, Cuenta inventario, FormaPago formaPago,
            BigDecimal subtotal, BigDecimal descuento, BigDecimal iva, BigDecimal total, BigDecimal costo) {
        List<LineaAsiento> lineas = new ArrayList<>();
        cargo(lineas, cuentaPago(formaPago, caja, bancos, clientes), total);
        cargo(lineas, rebajas, descuento);
        abono(lineas, ventas, subtotal);
        abono(lineas, ivaDebito, iva);
        BigDecimal costoRedondeado = Dinero.redondear(costo);
        cargo(lineas, costoVentas, costoRedondeado);
        abono(lineas, inventario, costoRedondeado);
        return lineas;
    }

    private static Cuenta cuentaPago(FormaPago forma, Cuenta caja, Cuenta bancos, Cuenta credito) {
        return switch (forma) {
            case Contado -> caja;
            case Cheque -> bancos;
            case Credito -> credito;
        };
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
