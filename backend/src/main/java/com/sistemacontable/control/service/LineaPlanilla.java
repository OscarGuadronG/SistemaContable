package com.sistemacontable.control.service;

import com.sistemacontable.entity.PlanillaDetalle;

/** Un detalle de planilla junto con su calculo. */
public record LineaPlanilla(PlanillaDetalle detalle, ResultadoPlanilla resultado) {
}
