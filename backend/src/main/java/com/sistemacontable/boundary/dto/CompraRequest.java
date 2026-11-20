package com.sistemacontable.boundary.dto;
import com.sistemacontable.control.service.cmd.CompraCmd;
import com.sistemacontable.control.service.cmd.LineaCompraCmd;
import com.sistemacontable.control.service.cmd.NuevoLoteCmd;
import com.sistemacontable.entity.enums.FormaPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraRequest(@NotNull Long proveedorId, Long empleadoId, LocalDate fecha, String numeroDocumento,
        String descripcion, @NotNull FormaPago formaPago, String numeroCheque,
        @DecimalMin("0") BigDecimal descuento, @DecimalMin("0") BigDecimal gastosAdicionales,
        @Valid List<@NotNull Linea> lineas) {
    public record NuevoLote(@NotNull Long productoId, @NotNull String numLote, LocalDate fechCaducidad) {
        NuevoLoteCmd aCmd() { return new NuevoLoteCmd(productoId, numLote, fechCaducidad); }
    }
    public record Linea(Long loteId, @Valid NuevoLote nuevoLote, @NotNull Long almacenId,
            @NotNull @DecimalMin(value="0.0000001") BigDecimal cantidad,
            @NotNull @DecimalMin("0") BigDecimal precio) {
        LineaCompraCmd aCmd() { return new LineaCompraCmd(loteId, nuevoLote == null ? null : nuevoLote.aCmd(), almacenId, cantidad, precio); }
    }
    public CompraCmd aCmd() { return new CompraCmd(proveedorId, empleadoId, fecha, numeroDocumento, descripcion,
            formaPago, numeroCheque, descuento, gastosAdicionales, lineas == null ? null : lineas.stream().map(Linea::aCmd).toList()); }
}
