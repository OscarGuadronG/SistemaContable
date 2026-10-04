package com.sistemacontable.control.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/** Utilidades puras para arboles con auto-referencia (cuentas, tipos de producto). */
public final class Jerarquia {

    private Jerarquia() {
    }

    /**
     * Indica si asignar {@code nuevoPadre} como padre de {@code nodo} crearia un ciclo: el nuevo padre es el propio
     * nodo o uno de sus descendientes (el nodo aparece subiendo por los ancestros del nuevo padre). Si los datos ya
     * contienen un ciclo por encima del nuevo padre tambien se devuelve {@code true}, en vez de recorrer sin fin.
     *
     * @param padreDe como obtener el padre de un nodo
     */
    public static <T> boolean hayCiclo(T nodo, T nuevoPadre, UnaryOperator<T> padreDe) {
        List<T> visitados = new ArrayList<>();
        T actual = nuevoPadre;
        while (actual != null) {
            if (actual.equals(nodo)) {
                return true;
            }
            for (T visitado : visitados) {
                if (visitado == actual) {
                    return true;
                }
            }
            visitados.add(actual);
            actual = padreDe.apply(actual);
        }
        return false;
    }
}
