package com.sistemacontable.boundary.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record LineaDevolucionRequest(@NotNull Long loteId, @NotNull Long almacenId,
        @NotNull @Positive BigDecimal cantidad) {
    public com.sistemacontable.control.service.cmd.LineaDevolucionCmd aCmd() {
        return new com.sistemacontable.control.service.cmd.LineaDevolucionCmd(loteId,almacenId,cantidad);
    }
}
