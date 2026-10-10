package com.sistemacontable.boundary.dto;

/** Resultado de pagar una planilla: ya Pagado, con el id de su partida de egreso. */
public record PagoPlanillaDto(PlanillaDto planilla,
        Long partidaId,
        ResumenPlanillaDto resumen) {
}
