package com.sistemacontable.boundary.dto;
import com.sistemacontable.entity.enums.FormaPago;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PagoCreditoRequest(@NotBlank @Pattern(regexp="venta|compra") String tipo,
        @NotNull Long documentoId, @NotNull LocalDate fecha, @NotNull @Positive BigDecimal capital,
        @NotNull FormaPago formaPago, String numeroCheque, String descripcion) {
    public com.sistemacontable.control.service.cmd.PagoCreditoCmd aCmd() {
        return new com.sistemacontable.control.service.cmd.PagoCreditoCmd(tipo,documentoId,fecha,capital,formaPago,numeroCheque,descripcion);
    }
}
