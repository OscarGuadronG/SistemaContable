package com.sistemacontable.boundary.mapper;
import com.sistemacontable.boundary.dto.TrasladoDto;
import com.sistemacontable.control.service.ResultadoTraslado;
public final class TrasladoMapper {
    private TrasladoMapper() {}
    public static TrasladoDto dto(ResultadoTraslado r, boolean detalle) {
        var k = r.movimiento().kardex();
        var lineas = detalle ? r.movimiento().detalles().stream().map(d -> new TrasladoDto.Linea(d.getLote().getId(), d.getLote().getNumLote(), d.getLote().getProducto().getNombre(), d.getCantidad(), d.getPrecioCosto())).toList() : null;
        var partida = detalle && r.partida() != null ? ContabilidadMapper.partida(r.partida(), r.partidaLineas()) : null;
        return new TrasladoDto(k.getId(), k.getFecha().toLocalDate(), k.getOrigen() == null ? null : new TrasladoDto.AlmacenDto(k.getOrigen().getId(),k.getOrigen().getNombre()), k.getDestino() == null ? null : new TrasladoDto.AlmacenDto(k.getDestino().getId(),k.getDestino().getNombre()), k.getDescripcion(), r.gastosAdicionales(), r.costoTotal(), Mapeo.id(r.partida()), partida, lineas);
    }
}
