package com.sistemacontable.control.service.cmd;

import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraCmd(Long proveedorId, Long empleadoId, LocalDate fecha, String numeroDocumento, String descripcion,
        FormaPago formaPago, String numeroCheque, BigDecimal descuento, BigDecimal gastosAdicionales,
        List<LineaCompraCmd> lineas) {
    public CompraCmd { lineas = lineas == null ? List.of() : List.copyOf(lineas); }
}
