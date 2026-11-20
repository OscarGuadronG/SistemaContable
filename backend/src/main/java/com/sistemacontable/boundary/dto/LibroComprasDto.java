package com.sistemacontable.boundary.dto;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record LibroComprasDto(List<Fila> filas, Totales totales) {
    public record Fila(LocalDate fecha, Long compraId, String numeroDocumento, String proveedor, String nit,
            FormaPago formaPago, BigDecimal subtotal, BigDecimal descuento, BigDecimal gastosAdicionales, BigDecimal iva, BigDecimal total) {}
    public record Totales(BigDecimal subtotal, BigDecimal descuento, BigDecimal gastosAdicionales, BigDecimal iva, BigDecimal total) {}
}
