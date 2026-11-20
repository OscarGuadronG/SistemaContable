package com.sistemacontable.boundary.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.sistemacontable.boundary.dto.PartidaDto;
public record TrasladoDto(Long kardexId, LocalDate fecha, AlmacenDto origen, AlmacenDto destino,
        String descripcion, BigDecimal gastosAdicionales, BigDecimal costoTotal, Long partidaId, PartidaDto partida,
        List<Linea> lineas) {
    public record AlmacenDto(Long id, String nombre) {}
    public record Linea(Long loteId, String lote, String producto, BigDecimal cantidad, BigDecimal costoUnitario) {}
}
