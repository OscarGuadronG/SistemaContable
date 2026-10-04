package com.sistemacontable.control.service;

import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.enums.NaturalezaCuenta;

/** Constructores de entidades en memoria (con id) para pruebas unitarias de servicios. */
final class Fabrica {

    private Fabrica() {
    }

    static Cuenta cuenta(long id, int codigo, NaturalezaCuenta naturaleza, boolean activa, boolean imputable) {
        Cuenta c = new Cuenta();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setNaturaleza(naturaleza);
        c.setActivo(activa);
        c.setImputable(imputable);
        return c;
    }

    /** Cuenta deudora, activa e imputable. */
    static Cuenta cuenta(long id, int codigo) {
        return cuenta(id, codigo, NaturalezaCuenta.Deudora, true, true);
    }

    static Almacen almacen(long id, boolean habilitado) {
        Almacen a = new Almacen();
        a.setId(id);
        a.setNombre("Almacen " + id);
        a.setEstado(habilitado);
        return a;
    }

    static Lote lote(long id) {
        Lote l = new Lote();
        l.setId(id);
        l.setNumLote("L-" + id);
        return l;
    }
}
