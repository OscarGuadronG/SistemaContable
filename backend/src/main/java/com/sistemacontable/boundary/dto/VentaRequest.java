package com.sistemacontable.boundary.dto;
import com.sistemacontable.control.service.cmd.LineaVentaCmd;
import com.sistemacontable.control.service.cmd.VentaCmd;
import com.sistemacontable.entity.enums.FormaPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record VentaRequest(@NotNull Long clienteId, Long empleadoId, @NotNull Long almacenId, LocalDate fecha,
        String numeroDocumento, String descripcion, @NotNull FormaPago formaPago, String numeroCheque,
        @DecimalMin("0") BigDecimal descuento, @Valid List<@NotNull Linea> lineas) {
    public record Linea(@NotNull Long productoId, @NotNull @DecimalMin(value="0.0000001") BigDecimal cantidad) {
        LineaVentaCmd aCmd() { return new LineaVentaCmd(productoId, cantidad); }
    }
    public VentaCmd aCmd() { return new VentaCmd(clienteId, empleadoId, almacenId, fecha, numeroDocumento,
            descripcion, formaPago, numeroCheque, descuento, lineas == null ? null : lineas.stream().map(Linea::aCmd).toList()); }
}
