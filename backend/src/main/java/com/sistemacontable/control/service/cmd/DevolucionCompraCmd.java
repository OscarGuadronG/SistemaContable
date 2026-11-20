package com.sistemacontable.control.service.cmd;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record DevolucionCompraCmd(Long compraId, LocalDate fecha,
        BigDecimal porcentajeCompensacion,
        String motivo, Long empleadoId, List<LineaDevolucionCmd> lineas) {
    public DevolucionCompraCmd { lineas = lineas == null ? List.of() : List.copyOf(lineas); }
}
