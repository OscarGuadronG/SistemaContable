package com.sistemacontable.control.service.cmd;
import java.time.LocalDate;
public record NuevoLoteCmd(Long productoId, String numLote, LocalDate fechCaducidad) {}
