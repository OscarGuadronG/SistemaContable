package com.sistemacontable.boundary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.sistemacontable.entity.enums.FormaPago;
import com.sistemacontable.boundary.dto.PartidaDto;

/** Compra. {@code lineas} es nulo (y no se serializa) en los listados; {@code GET /compras/{id}} las incluye. */
public record CompraDto(Long id,
        LocalDate fecha,
        Long proveedorId,
        Long empleadoId,
        String numeroDocumento,
        String descripcion,
        FormaPago formaPago,
        String numeroCheque,
        BigDecimal descuento,
        BigDecimal gastosAdicionales,
        BigDecimal subtotal,
        BigDecimal iva,
        BigDecimal total,
        BigDecimal tasaInteres,
        Integer plazoMeses,
        LocalDate fechaVencimiento,
        List<KardexDto> kardex,
        PartidaDto partida,
        List<Linea> lineas) {

    /** Linea de una compra. */
    public record Linea(Long id, Long loteId, Long almacenId, BigDecimal cantidad, BigDecimal precio) {
    }
}
