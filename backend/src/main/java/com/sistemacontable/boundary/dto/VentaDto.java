package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.sistemacontable.entity.enums.FormaPago;
import com.sistemacontable.boundary.dto.PartidaDto;

/** Venta. {@code lineas} es nulo (y no se serializa) en los listados; {@code GET /ventas/{id}} las incluye. */
public record VentaDto(Long id,
        LocalDate fecha,
        Long clienteId,
        Long empleadoId,
        Long almacenId,
        String numeroDocumento,
        String descripcion,
        FormaPago formaPago,
        String numeroCheque,
        BigDecimal descuento,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        BigDecimal costoTotal,
        BigDecimal tasaInteres,
        Integer plazoMeses,
        LocalDate fechaVencimiento,
        List<KardexDto> kardex,
        PartidaDto partida,
        List<Linea> lineas) {

    /** Linea de una venta. */
    public record Linea(Long id, Long loteId, Long almacenId, BigDecimal cantidad, BigDecimal precio, BigDecimal costoUnitario) {
    }
}
