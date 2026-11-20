package com.sistemacontable.boundary.dto;
import java.math.BigDecimal;
public record PrecioVentaDto(Long productoId, String nombre, InventarioDto.Unidad unidad, InventarioDto.Tipo tipo,
        BigDecimal existencia, BigDecimal costoPromedio, BigDecimal precioVenta) {}
