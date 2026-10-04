package com.sistemacontable.control.service;

import java.util.Comparator;

/**
 * Par lote/almacen que identifica una fila de existencias. Es comparable (lote y luego almacen) para bloquear las
 * filas siempre en el mismo orden y reducir el riesgo de interbloqueos entre transacciones concurrentes.
 */
public record ClaveStock(Long loteId, Long almacenId) implements Comparable<ClaveStock> {

    private static final Comparator<ClaveStock> ORDEN = Comparator.comparing(ClaveStock::loteId)
            .thenComparing(ClaveStock::almacenId);

    @Override
    public int compareTo(ClaveStock otra) {
        return ORDEN.compare(this, otra);
    }
}
