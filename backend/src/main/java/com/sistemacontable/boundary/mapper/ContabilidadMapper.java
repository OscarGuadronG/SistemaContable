package com.sistemacontable.boundary.mapper;

import com.sistemacontable.boundary.dto.BalanzaDto;
import com.sistemacontable.boundary.dto.PartidaDto;
import com.sistemacontable.control.service.Balanza;
import com.sistemacontable.control.service.FilaBalanza;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import java.util.List;

/** Entidad a DTO de partidas y balanza. Las {@code lineas} nulas se conservan nulas (los listados no las incluyen). */
public final class ContabilidadMapper {

    private ContabilidadMapper() {
    }

    public static PartidaDto partida(Partida p, List<PartidaDetalle> lineas) {
        return p == null ? null : new PartidaDto(p.getId(), p.getFecha(), p.getConcepto(), p.getTipo(),
                Mapeo.id(p.getCompra()), Mapeo.id(p.getVenta()), Mapeo.id(p.getTercero()),
                Mapeo.lista(lineas, ContabilidadMapper::linea));
    }

    public static BalanzaDto balanza(Balanza b) {
        return new BalanzaDto(b.desde(), b.hasta(), Mapeo.lista(b.filas(), ContabilidadMapper::fila),
                b.totalCargos(), b.totalAbonos(), b.cuadra(), b.diferencia());
    }

    private static PartidaDto.Linea linea(PartidaDetalle d) {
        return new PartidaDto.Linea(d.getId(), Mapeo.id(d.getCuenta()), d.getCargo(), d.getAbono());
    }

    private static BalanzaDto.Fila fila(FilaBalanza f) {
        return new BalanzaDto.Fila(f.cuentaId(), f.codigo(), f.nombre(), f.naturaleza(), f.imputable(), f.cargos(),
                f.abonos(), f.saldo());
    }
}
