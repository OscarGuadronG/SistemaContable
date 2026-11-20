package com.sistemacontable.control.service.cmd;
import java.math.BigDecimal;
public record LineaVentaCmd(Long productoId, BigDecimal cantidad) {}
