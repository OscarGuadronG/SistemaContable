package com.sistemacontable.control.service;

import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import java.util.List;

/** Movimiento de kardex persistido: cabecera y sus lineas. */
public record MovimientoKardex(Kardex kardex, List<KardexDetalle> detalles) {

    public MovimientoKardex {
        detalles = List.copyOf(detalles);
    }
}
