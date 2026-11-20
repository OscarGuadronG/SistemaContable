package com.sistemacontable.control.service;

import com.sistemacontable.boundary.dto.LibroComprasDto;
import com.sistemacontable.boundary.dto.LibroVentasDto;
import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.Venta;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Stateless @LocalBean
public class LibroService {
    @Inject private CompraDAO compras;
    @Inject private VentaDAO ventas;
    public LibroComprasDto compras(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<LibroComprasDto.Fila> filas = new ArrayList<>();
        long cantidad = compras.count();
        for (int offset = 0; offset < cantidad; offset += 100) for (Compra c : compras.findRange(offset, (int)Math.min(100, cantidad - offset))) {
            if (dentro(c.getFecha(), desde, hasta)) filas.add(new LibroComprasDto.Fila(c.getFecha(), c.getId(), c.getNumeroDocumento(), c.getProveedor().getNombre(), c.getProveedor().getNit(), c.getFormaPago(), c.getSubtotal(), c.getDescuento(), c.getGastosAdicionales(), c.getIva(), c.getTotal()));
        }
        filas.sort(java.util.Comparator.comparing(LibroComprasDto.Fila::fecha).thenComparing(LibroComprasDto.Fila::compraId));
        BigDecimal subtotal = sumC(filas, 0), descuento = sumC(filas, 1), gastos = sumC(filas, 2), iva = sumC(filas, 3), total = sumC(filas, 4);
        return new LibroComprasDto(List.copyOf(filas), new LibroComprasDto.Totales(subtotal, descuento, gastos, iva, total));
    }
    public LibroVentasDto ventas(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<LibroVentasDto.Fila> filas = new ArrayList<>();
        long cantidad = ventas.count();
        for (int offset = 0; offset < cantidad; offset += 100) for (Venta v : ventas.findRange(offset, (int)Math.min(100, cantidad - offset))) {
            if (dentro(v.getFecha(), desde, hasta)) filas.add(new LibroVentasDto.Fila(v.getFecha(), v.getId(), v.getNumeroDocumento(), v.getCliente().getNombre(), v.getCliente().getNit(), v.getFormaPago(), v.getSubtotal(), v.getDescuento(), v.getIva(), v.getTotal()));
        }
        filas.sort(java.util.Comparator.comparing(LibroVentasDto.Fila::fecha).thenComparing(LibroVentasDto.Fila::ventaId));
        BigDecimal subtotal = sumV(filas, 0), descuento = sumV(filas, 1), iva = sumV(filas, 2), total = sumV(filas, 3);
        return new LibroVentasDto(List.copyOf(filas), new LibroVentasDto.Totales(subtotal, descuento, iva, total));
    }
    private static void validarRango(LocalDate desde, LocalDate hasta) { if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final"); }
    private static boolean dentro(LocalDate f, LocalDate d, LocalDate h) { return (d == null || !f.isBefore(d)) && (h == null || !f.isAfter(h)); }
    private static BigDecimal sumC(List<LibroComprasDto.Fila> f, int x) { return Dinero.redondear(f.stream().map(a -> switch(x) { case 0 -> a.subtotal(); case 1 -> a.descuento(); case 2 -> a.gastosAdicionales(); case 3 -> a.iva(); default -> a.total(); }).reduce(BigDecimal.ZERO, BigDecimal::add)); }
    private static BigDecimal sumV(List<LibroVentasDto.Fila> f, int x) { return Dinero.redondear(f.stream().map(a -> switch(x) { case 0 -> a.subtotal(); case 1 -> a.descuento(); case 2 -> a.iva(); default -> a.total(); }).reduce(BigDecimal.ZERO, BigDecimal::add)); }
}
