package com.sistemacontable.boundary.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record DevolucionVentaRequest(@NotNull Long ventaId, @NotNull LocalDate fecha,
        String motivo, Long empleadoId, @NotEmpty List<@NotNull @Valid LineaDevolucionRequest> lineas) {
    public com.sistemacontable.control.service.cmd.DevolucionVentaCmd aCmd() {
        return new com.sistemacontable.control.service.cmd.DevolucionVentaCmd(ventaId,fecha,motivo,empleadoId,lineas == null ? null : lineas.stream().map(LineaDevolucionRequest::aCmd).toList());
    }
}
