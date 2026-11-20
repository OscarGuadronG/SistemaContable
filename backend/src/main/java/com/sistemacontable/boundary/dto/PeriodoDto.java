package com.sistemacontable.boundary.dto;

import com.sistemacontable.entity.enums.EstadoPeriodo;
import java.time.Instant;
import java.time.LocalDate;

public record PeriodoDto(Long id, Integer anioFiscal, Short trimestre, LocalDate fechaInicio, LocalDate fechaFin, EstadoPeriodo estado,
        HojaTrabajoDto hojaTrabajo, Instant fechaCierre, Long idPartidaCierre, Long idPartidaApertura) { }
