package com.sistemacontable.control.service.cmd;
import java.math.BigDecimal;
public record LineaCompraCmd(Long loteId, NuevoLoteCmd nuevoLote, Long almacenId, BigDecimal cantidad, BigDecimal precio) {}
