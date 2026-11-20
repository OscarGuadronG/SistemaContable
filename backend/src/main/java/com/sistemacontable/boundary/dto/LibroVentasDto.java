package com.sistemacontable.boundary.dto;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record LibroVentasDto(List<Fila> filas, Totales totales) {
    public record Fila(LocalDate fecha, Long ventaId, String numeroDocumento, String cliente, String nit,
            FormaPago formaPago, BigDecimal subtotal, BigDecimal descuento, BigDecimal iva, BigDecimal total) {}
    public record Totales(BigDecimal subtotal, BigDecimal descuento, BigDecimal iva, BigDecimal total) {}
}
