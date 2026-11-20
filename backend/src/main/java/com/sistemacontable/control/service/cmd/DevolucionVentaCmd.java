package com.sistemacontable.control.service.cmd;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record DevolucionVentaCmd(Long ventaId, LocalDate fecha,
        String motivo, Long empleadoId, List<LineaDevolucionCmd> lineas) {
    public DevolucionVentaCmd { lineas = lineas == null ? List.of() : List.copyOf(lineas); }
}
