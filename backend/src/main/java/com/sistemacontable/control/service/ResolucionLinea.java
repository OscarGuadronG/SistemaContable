package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Lote;
import java.math.BigDecimal;

/** Validacion y resolucion (sin escribir) de una linea de compra o de venta. */
final class ResolucionLinea {

    private ResolucionLinea() {
    }

    /**
     * Exige lote y almacen, cantidad mayor que cero y precio mayor o igual que cero; carga el lote y el almacen
     * (deben existir) y exige que el almacen este habilitado ({@code estado = true}).
     *
     * @param numero posicion de la linea (desde 1), para los mensajes
     */
    static LineaResuelta resolver(LoteDAO loteDAO, AlmacenDAO almacenDAO, Long loteId, Long almacenId,
            BigDecimal cantidad, BigDecimal precio, int numero) {
        String donde = "La linea " + numero;
        if (loteId == null) {
            throw new ReglaNegocioException(donde + " debe indicar el lote");
        }
        if (almacenId == null) {
            throw new ReglaNegocioException(donde + " debe indicar el almacen");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaNegocioException(donde + " debe tener cantidad mayor que cero");
        }
        if (precio == null || precio.signum() < 0) {
            throw new ReglaNegocioException(donde + " debe tener precio mayor o igual que cero");
        }
        Lote lote = loteDAO.buscarPorId(loteId).orElseThrow(() -> new NoEncontradoException("Lote", loteId));
        Almacen almacen = almacenDAO.buscarPorId(almacenId)
                .orElseThrow(() -> new NoEncontradoException("Almacen", almacenId));
        if (!Boolean.TRUE.equals(almacen.getEstado())) {
            throw new ReglaNegocioException("El almacen " + almacenId + " de la linea " + numero
                    + " esta deshabilitado");
        }
        return new LineaResuelta(lote, almacen, cantidad, precio);
    }
}
