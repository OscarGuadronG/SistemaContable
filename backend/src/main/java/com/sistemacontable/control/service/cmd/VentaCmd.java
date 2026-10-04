package com.sistemacontable.control.service.cmd;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Orden de registrar una venta. {@code fecha} nula equivale a hoy. */
public record VentaCmd(Long clienteId, Long empleadoId, LocalDate fecha, String descripcion,
        List<LineaVentaCmd> lineas) {

    public VentaCmd {
        lineas = lineas == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(lineas));
    }
}
