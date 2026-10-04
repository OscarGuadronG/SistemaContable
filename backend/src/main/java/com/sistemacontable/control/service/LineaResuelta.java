package com.sistemacontable.control.service;

import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Lote;
import java.math.BigDecimal;

/** Linea de compra o venta ya validada, con el lote y el almacen cargados. */
record LineaResuelta(Lote lote, Almacen almacen, BigDecimal cantidad, BigDecimal precio) {

    /** Importe sin redondear de la linea (cantidad x precio). */
    BigDecimal importe() {
        return cantidad.multiply(precio);
    }
}
