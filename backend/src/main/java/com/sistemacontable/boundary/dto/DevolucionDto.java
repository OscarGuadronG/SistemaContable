package com.sistemacontable.boundary.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record DevolucionDto(Long id, Long compraId, Long ventaId, Long empleadoId, LocalDate fecha,
        BigDecimal porcentajeCompensacion, BigDecimal montoCosto, BigDecimal montoCompensado,
        BigDecimal monto, BigDecimal costo, String motivo, List<Linea> lineas,
        List<KardexDto> kardex, PartidaDto partida) {
    public record Linea(Long id, Long loteId, Long almacenId, BigDecimal cantidad,
            BigDecimal precio, BigDecimal costoUnitario) { }
}
