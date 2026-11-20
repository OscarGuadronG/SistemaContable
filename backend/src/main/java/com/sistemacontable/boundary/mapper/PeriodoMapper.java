package com.sistemacontable.boundary.mapper;
import com.sistemacontable.boundary.dto.PeriodoDto;
import com.sistemacontable.entity.Periodo;
public final class PeriodoMapper {
    private PeriodoMapper() { }
    public static PeriodoDto periodo(Periodo p) { com.sistemacontable.boundary.dto.HojaTrabajoDto hoja=p.getHojaTrabajo()==null?null:hojaJson(p.getHojaTrabajo());
        return new PeriodoDto(p.getId(), p.getAnioFiscal(), p.getTrimestre(), p.getFechaInicio(), p.getFechaFin(),
            p.getEstado(), hoja, p.getFechaCierre(),
            p.getPartidaCierre() == null ? null : p.getPartidaCierre().getId(),
            p.getPartidaApertura() == null ? null : p.getPartidaApertura().getId()); }
    private static com.sistemacontable.boundary.dto.HojaTrabajoDto hojaJson(String texto) {
        try (var jsonb=jakarta.json.bind.JsonbBuilder.create()) {
            return HojaTrabajoMapper.hoja(jsonb.fromJson(texto,java.util.Map.class));
        } catch (Exception e) { throw new IllegalStateException("No se pudo leer la hoja de trabajo guardada",e); }
    }
}
