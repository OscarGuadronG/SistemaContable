package com.sistemacontable.boundary.dto;
import com.sistemacontable.entity.enums.EstadoPeriodo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
/** Contrato estable de las cinco parejas de columnas y el resultado del período. */
public record HojaTrabajoDto(PeriodoRef periodo, LocalDate desde, LocalDate hasta, List<Fila> filas,
        Totales totales, BigDecimal utilidad, BigDecimal perdida, Cuadrada cuadrada) {
    public record PeriodoRef(Long id,Integer anioFiscal,Short trimestre,LocalDate fechaInicio,LocalDate fechaFin,EstadoPeriodo estado) { }
    public record Columna(BigDecimal debe,BigDecimal haber) { }
    public record Fila(Long cuentaId,Integer codigo,String nombre,Integer clase,Columna comprobacion,
            Columna ajustes,Columna ajustado,Columna resultados,Columna balance) { }
    public record Totales(Columna comprobacion,Columna ajustes,Columna ajustado,Columna resultados,Columna balance) { }
    public record Cuadrada(boolean comprobacion,boolean ajustado,boolean balance) { }
}
