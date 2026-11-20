package com.sistemacontable.control.service.cmd;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record LineaDevolucionCmd(Long loteId, Long almacenId,
        BigDecimal cantidad) { }
