package com.sistemacontable.control.service.cmd;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Orden de registrar una compra. {@code fecha} nula equivale a hoy. */
public record CompraCmd(Long proveedorId, Long empleadoId, LocalDate fecha, String descripcion,
        List<LineaCompraCmd> lineas) {

    public CompraCmd {
        lineas = lineas == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(lineas));
    }
}
