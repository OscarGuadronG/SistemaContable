package com.sistemacontable.control.service.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Orden de trasladar stock de un almacen a otro (distintos). */
public record TrasladoCmd(Long almacenOrigenId, Long almacenDestinoId, String descripcion,
        List<LineaTrasladoCmd> lineas) {

    public TrasladoCmd {
        lineas = lineas == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(lineas));
    }
}
