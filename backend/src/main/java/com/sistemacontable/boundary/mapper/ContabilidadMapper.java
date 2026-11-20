package com.sistemacontable.boundary.mapper;

import com.sistemacontable.boundary.dto.BalanzaDto;
import com.sistemacontable.boundary.dto.PartidaDto;
import com.sistemacontable.boundary.dto.PartidaResumenDto;
import com.sistemacontable.control.service.Balanza;
import com.sistemacontable.control.service.FilaBalanza;
import com.sistemacontable.control.service.ResumenPartida;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import java.util.List;

/** Entidad a DTO de partidas y balanza. Las {@code lineas} nulas se conservan nulas (los listados no las incluyen). */
public final class ContabilidadMapper {

    private ContabilidadMapper() {
    }

    public static PartidaDto partida(Partida p, List<PartidaDetalle> lineas) {
        return p == null ? null : new PartidaDto(p.getId(), p.getFecha(), p.getConcepto(), p.getTipo(),
                p.getIdCompra(), p.getIdVenta(), Mapeo.id(p.getTercero()), Mapeo.id(p.getPeriodo()),p.getIdKardex(),p.getIdPlanilla(),
                p.getIdPagoCredito(),p.getIdDevolucionCompra(),p.getIdDevolucionVenta(),p.getCreado(),origen(p),
                Mapeo.lista(lineas, ContabilidadMapper::linea));
    }

    public static PartidaResumenDto resumen(ResumenPartida resumen) {
        Partida p=resumen.partida();
        return new PartidaResumenDto(p.getId(),p.getFecha(),p.getConcepto(),p.getTipo(),origen(p),
                CatalogoMapper.tercero(p.getTercero()),resumen.totalCargo(),resumen.totalAbono());
    }

    public static BalanzaDto balanza(Balanza b) {
        return new BalanzaDto(b.desde(), b.hasta(), Mapeo.lista(b.filas(), ContabilidadMapper::fila),
                b.totalCargos(), b.totalAbonos(), b.cuadra(), b.diferencia());
    }

    private static PartidaDto.Linea linea(PartidaDetalle d) {
        var c=d.getCuenta();
        return new PartidaDto.Linea(d.getId(), Mapeo.id(c),new PartidaDto.CuentaRef(c.getId(),c.getCodigo(),c.getNombre()),d.getCargo(), d.getAbono());
    }

    private static String origen(Partida p) {
        if(p.getTipo()==com.sistemacontable.entity.enums.TipoPartida.Cierre) return "cierre";
        if(p.getTipo()==com.sistemacontable.entity.enums.TipoPartida.Apertura) return "apertura";
        if(p.getTipo()==com.sistemacontable.entity.enums.TipoPartida.Ajuste) return "ajuste";
        if(p.getCompra()!=null) return "compra";
        if(p.getVenta()!=null) return "venta";
        if(p.getIdPlanilla()!=null) return "planilla";
        if(p.getIdPagoCredito()!=null) return "pago";
        if(p.getIdDevolucionCompra()!=null||p.getIdDevolucionVenta()!=null) return "devolucion";
        if(p.getIdKardex()!=null) return "traslado";
        return "manual";
    }

    private static BalanzaDto.Fila fila(FilaBalanza f) {
        return new BalanzaDto.Fila(f.cuentaId(), f.codigo(), f.nombre(), f.naturaleza(), f.imputable(), f.cargos(),
                f.abonos(), f.saldo());
    }
}
