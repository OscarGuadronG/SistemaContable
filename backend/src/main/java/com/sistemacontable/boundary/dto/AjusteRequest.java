package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.AjusteCmd;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Ajuste de inventario: {@code cantidad} con signo explicito (positiva entra, negativa sale) y motivo obligatorio. */
public record AjusteRequest(@NotNull Long almacenId,
        @NotNull Long loteId,
        @NotNull BigDecimal cantidad,
        @NotBlank String motivo) {

    public AjusteCmd aCmd() {
        return new AjusteCmd(almacenId, loteId, cantidad, motivo);
    }
}
