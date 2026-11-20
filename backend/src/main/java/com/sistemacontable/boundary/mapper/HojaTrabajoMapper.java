package com.sistemacontable.boundary.mapper;
import com.sistemacontable.boundary.dto.HojaTrabajoDto;
import com.sistemacontable.entity.enums.EstadoPeriodo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
/** Convierte tanto cálculos en memoria como instantáneas JSON sin exponer mapas en REST. */
public final class HojaTrabajoMapper {
    private HojaTrabajoMapper() { }
    public static HojaTrabajoDto hoja(Map<String,Object> h) {
        Map<?,?> p=map(h.get("periodo")),t=map(h.get("totales")),q=map(h.get("cuadrada"));
        var periodo=p.isEmpty()?null:new HojaTrabajoDto.PeriodoRef(numero(p.get("id")).longValue(),numero(p.get("anioFiscal")).intValue(),numero(p.get("trimestre")).shortValue(),fecha(p.get("fechaInicio")),fecha(p.get("fechaFin")),EstadoPeriodo.valueOf(p.get("estado").toString()));
        List<?> filas=(List<?>)h.get("filas");
        return new HojaTrabajoDto(periodo,fecha(h.get("desde")),fecha(h.get("hasta")),filas.stream().map(HojaTrabajoMapper::fila).toList(),
                new HojaTrabajoDto.Totales(col(t.get("comprobacion")),col(t.get("ajustes")),col(t.get("ajustado")),col(t.get("resultados")),col(t.get("balance"))),
                numero(h.get("utilidad")),numero(h.get("perdida")),new HojaTrabajoDto.Cuadrada(Boolean.TRUE.equals(q.get("comprobacion")),Boolean.TRUE.equals(q.get("ajustado")),Boolean.TRUE.equals(q.get("balance"))));
    }
    private static HojaTrabajoDto.Fila fila(Object o) {
        Map<?,?> f=map(o);
        return new HojaTrabajoDto.Fila(numero(f.get("cuentaId")).longValue(),numero(f.get("codigo")).intValue(),(String)f.get("nombre"),numero(f.get("clase")).intValue(),col(f.get("comprobacion")),col(f.get("ajustes")),col(f.get("ajustado")),col(f.get("resultados")),col(f.get("balance")));
    }
    private static Map<?,?> map(Object o) { return o instanceof Map<?,?> m?m:Map.of(); }
    private static BigDecimal numero(Object o) { return o==null?BigDecimal.ZERO:new BigDecimal(o.toString()); }
    private static LocalDate fecha(Object o) { return o==null?null:o instanceof LocalDate d?d:LocalDate.parse(o.toString()); }
    private static HojaTrabajoDto.Columna col(Object o) { var c=map(o);return new HojaTrabajoDto.Columna(numero(c.get("debe")),numero(c.get("haber"))); }
}
