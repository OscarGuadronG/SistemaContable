package com.sistemacontable.control.service;

import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import java.util.List;

/**
 * Invariantes de un movimiento de kardex segun el tipo:
 * <ul>
 *   <li>{@code Entrada}: solo destino (almacen que recibe); puede llevar compra, nunca venta.</li>
 *   <li>{@code Salida}: solo origen (almacen que entrega); puede llevar venta, nunca compra.</li>
 *   <li>{@code Traslado}: origen y destino distintos; sin compra ni venta.</li>
 *   <li>{@code Ajuste}: exactamente un almacen, sin compra ni venta. La cantidad del detalle es siempre positiva
 *       y el signo del ajuste lo da la posicion del almacen: entrada de ajuste = destino, salida de ajuste =
 *       origen.</li>
 * </ul>
 * Las lineas llevan lote, cantidad mayor que cero y costo mayor o igual que cero.
 */
public final class ReglasKardex {

    private ReglasKardex() {
    }

    public static void validar(TipoMovimiento tipo, Almacen origen, Almacen destino, Compra compra, Venta venta,
            List<DetalleKardex> detalles) {
        if (tipo == null) {
            throw new ReglaNegocioException("El tipo de movimiento de kardex es obligatorio");
        }
        if (compra != null && venta != null) {
            throw new ReglaNegocioException("Un movimiento de kardex no puede ligarse a una compra y a una venta");
        }
        switch (tipo) {
            case Entrada -> {
                exigir(origen == null && destino != null, "Una entrada lleva solo almacen de destino");
                exigir(venta == null, "Una entrada no puede ligarse a una venta");
            }
            case Salida -> {
                exigir(origen != null && destino == null, "Una salida lleva solo almacen de origen");
                exigir(compra == null, "Una salida no puede ligarse a una compra");
            }
            case Traslado -> {
                exigir(origen != null && destino != null, "Un traslado lleva almacen de origen y de destino");
                exigir(!origen.equals(destino), "El origen y el destino de un traslado deben ser distintos");
                exigir(compra == null && venta == null, "Un traslado no se liga a compra ni a venta");
            }
            case Ajuste -> {
                exigir((origen == null) != (destino == null), "Un ajuste lleva exactamente un almacen (origen o destino)");
                exigir(compra == null && venta == null, "Un ajuste no se liga a compra ni a venta");
            }
            default -> throw new ReglaNegocioException("Tipo de movimiento no soportado: " + tipo);
        }
        validarDetalles(detalles);
    }

    private static void validarDetalles(List<DetalleKardex> detalles) {
        exigir(detalles != null && !detalles.isEmpty(), "El movimiento de kardex debe tener al menos una linea");
        int numero = 0;
        for (DetalleKardex d : detalles) {
            numero++;
            String donde = "La linea " + numero + " del kardex ";
            exigir(d != null, donde + "es nula");
            exigir(d.lote() != null, donde + "no tiene lote");
            exigir(d.cantidad() != null && d.cantidad().signum() > 0, donde + "debe tener cantidad mayor que cero");
            exigir(d.precioCosto() != null && d.precioCosto().signum() >= 0,
                    donde + "debe tener costo mayor o igual que cero");
        }
    }

    private static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new ReglaNegocioException(mensaje);
        }
    }
}
