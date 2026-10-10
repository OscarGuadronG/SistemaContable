package com.sistemacontable.boundary.dto;

import com.sistemacontable.control.service.cmd.LoteCmd;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

/** Datos para crear o modificar un lote ({@code productoId} es obligatorio al crear y no puede cambiar al modificar). */
public record LoteRequest(Long productoId,
        @NotBlank String numLote,
        LocalDate fechCaducidad) {

    public LoteCmd aCmd() {
        return new LoteCmd(productoId, numLote, fechCaducidad);
    }
}
