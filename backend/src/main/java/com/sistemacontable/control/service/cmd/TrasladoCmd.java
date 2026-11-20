package com.sistemacontable.control.service.cmd;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record TrasladoCmd(Long almacenOrigenId, Long almacenDestinoId, LocalDate fecha, String descripcion,
        BigDecimal gastosAdicionales, FormaPago formaPagoGastos, String numeroCheque, List<LineaTrasladoCmd> lineas) {
    public TrasladoCmd { lineas = lineas == null ? List.of() : List.copyOf(lineas); }
}
