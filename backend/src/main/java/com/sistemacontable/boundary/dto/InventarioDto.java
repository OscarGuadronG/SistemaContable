package com.sistemacontable.boundary.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record InventarioDto(Long productoId, String nombre, String descripcion, Tipo tipo, Unidad unidad,
        BigDecimal existenciaTotal, BigDecimal costoPromedio, List<PorAlmacen> porAlmacen, List<Lote> lotes) {
    public record Tipo(Long id, String nombre) {}
    public record Unidad(Long id, String nombre, String abrev) {}
    public record PorAlmacen(Long almacenId, String almacen, BigDecimal cantidad) {}
    public record Lote(Long loteId, String numLote, LocalDate fechCaducidad, Long almacenId, String almacen,
            BigDecimal cantidad, BigDecimal costoUnitario) {}
}
