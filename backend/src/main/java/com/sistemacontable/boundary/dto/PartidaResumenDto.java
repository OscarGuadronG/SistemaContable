package com.sistemacontable.boundary.dto;
import com.sistemacontable.entity.enums.TipoPartida;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PartidaResumenDto(Long id,LocalDate fecha,String concepto,TipoPartida tipo,String origen,
        TerceroDto tercero,BigDecimal totalCargo,BigDecimal totalAbono) { }
