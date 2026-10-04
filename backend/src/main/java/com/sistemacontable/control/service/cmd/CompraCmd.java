package com.sistemacontable.control.service.cmd;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orden de registrar una compra. {@code fecha} nula equivale a hoy. {@code porcentajeContado} (0 a 100, obligatorio)
 * es la parte del total pagada al contado (Caja); el resto queda a credito (Proveedores). No se guarda en la base.
 */
public record CompraCmd(Long proveedorId, Long empleadoId, LocalDate fecha, String descripcion,
        BigDecimal porcentajeContado, List<LineaCompraCmd> lineas) {

    public CompraCmd {
        lineas = lineas == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(lineas));
    }
}
