package com.sistemacontable.boundary.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record DevolucionCompraRequest(@NotNull Long compraId, @NotNull LocalDate fecha,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal porcentajeCompensacion,
        String motivo, Long empleadoId, @NotEmpty List<@NotNull @Valid LineaDevolucionRequest> lineas) {
    public com.sistemacontable.control.service.cmd.DevolucionCompraCmd aCmd() {
        return new com.sistemacontable.control.service.cmd.DevolucionCompraCmd(compraId,fecha,porcentajeCompensacion,motivo,empleadoId,lineas == null ? null : lineas.stream().map(LineaDevolucionRequest::aCmd).toList());
    }
}
