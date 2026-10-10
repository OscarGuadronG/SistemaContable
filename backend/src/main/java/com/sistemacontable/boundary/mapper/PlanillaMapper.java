package com.sistemacontable.boundary.mapper;

import com.sistemacontable.boundary.dto.PagoPlanillaDto;
import com.sistemacontable.boundary.dto.PlanillaDetalleDto;
import com.sistemacontable.boundary.dto.PlanillaDto;
import com.sistemacontable.boundary.dto.ResultadoPlanillaDto;
import com.sistemacontable.boundary.dto.ResumenPlanillaDto;
import com.sistemacontable.control.service.LineaPlanilla;
import com.sistemacontable.control.service.PagoPlanilla;
import com.sistemacontable.control.service.ResultadoPlanilla;
import com.sistemacontable.control.service.ResumenPlanilla;
import com.sistemacontable.entity.Planilla;
import com.sistemacontable.entity.PlanillaDetalle;

/** Entidad a DTO de planillas, su calculo y su pago. */
public final class PlanillaMapper {

    private PlanillaMapper() {
    }

    public static PlanillaDto planilla(Planilla p) {
        return p == null ? null : new PlanillaDto(p.getId(), p.getFechaInicio(), p.getFechaFin(), p.getFechaPago(),
                p.getDescripcion(), p.getEstado());
    }

    public static PlanillaDetalleDto detalle(PlanillaDetalle d) {
        return d == null ? null : new PlanillaDetalleDto(d.getId(), Mapeo.id(d.getPlanilla()),
                Mapeo.id(d.getEmpleado()), d.getDias(), d.getHorasxDiur(), d.getHorasxNoct(), d.getSueldoBase());
    }

    public static ResumenPlanillaDto resumen(ResumenPlanilla r) {
        return new ResumenPlanillaDto(planilla(r.planilla()), Mapeo.lista(r.lineas(), PlanillaMapper::linea),
                r.totalDevengado(), r.totalIsss(), r.totalAfp(), r.totalRenta(), r.totalDeducciones(),
                r.totalLiquido());
    }

    public static PagoPlanillaDto pago(PagoPlanilla p) {
        return new PagoPlanillaDto(planilla(p.planilla()), Mapeo.id(p.partida()), resumen(p.resumen()));
    }

    private static ResumenPlanillaDto.Linea linea(LineaPlanilla l) {
        return new ResumenPlanillaDto.Linea(detalle(l.detalle()), calculo(l.resultado()));
    }

    private static ResultadoPlanillaDto calculo(ResultadoPlanilla r) {
        return new ResultadoPlanillaDto(r.sueldoProporcional(), r.valorHora(), r.pagoHorasDiurnas(),
                r.pagoHorasNocturnas(), r.devengado(), r.isss(), r.afp(), r.renta(), r.totalDeducciones(),
                r.liquido());
    }
}
