package com.sistemacontable.control.service.cmd;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record VentaCmd(Long clienteId, Long empleadoId, Long almacenId, LocalDate fecha, String numeroDocumento,
        String descripcion, FormaPago formaPago, String numeroCheque, BigDecimal descuento,
        List<LineaVentaCmd> lineas) {
    public VentaCmd { lineas = lineas == null ? List.of() : List.copyOf(lineas); }
}
