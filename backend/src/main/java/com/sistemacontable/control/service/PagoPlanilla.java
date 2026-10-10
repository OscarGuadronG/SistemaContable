package com.sistemacontable.control.service;

import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Planilla;

/** Resultado de pagar una planilla: la planilla ya Pagado, su partida de egreso y el calculo pagado. */
public record PagoPlanilla(Planilla planilla, Partida partida, ResumenPlanilla resumen) {
}
