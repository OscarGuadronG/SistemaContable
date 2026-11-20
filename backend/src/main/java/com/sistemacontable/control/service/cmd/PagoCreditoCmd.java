package com.sistemacontable.control.service.cmd;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record PagoCreditoCmd(String tipo,
        Long documentoId, LocalDate fecha, BigDecimal capital,
        FormaPago formaPago, String numeroCheque, String descripcion) { }
