package com.sistemacontable.control.service;

import com.sistemacontable.control.PartidaDetalleDAO.SumaCuenta;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calculo puro de la balanza de comprobacion: agrega los movimientos de cada cuenta a todos sus ancestros y calcula
 * el saldo segun la naturaleza. Solo aparecen las cuentas con movimientos propios o en sus descendientes, ordenadas
 * por codigo.
 */
public final class CalculoBalanza {

    private CalculoBalanza() {
    }

    /**
     * @param catalogo todas las cuentas
     * @param sumas cargos y abonos por cuenta (solo cuentas con movimientos) del rango
     * @throws NoEncontradoException si alguna suma pertenece a una cuenta que no esta en el catalogo
     */
    public static Balanza calcular(LocalDate desde, LocalDate hasta, List<Cuenta> catalogo, List<SumaCuenta> sumas) {
        Map<Long, Cuenta> porId = new HashMap<>();
        for (Cuenta c : catalogo) {
            porId.put(c.getId(), c);
        }
        Map<Long, BigDecimal[]> acumulado = new HashMap<>();
        BigDecimal totalCargos = BigDecimal.ZERO;
        BigDecimal totalAbonos = BigDecimal.ZERO;
        for (SumaCuenta s : sumas) {
            BigDecimal cargo = s.totalCargo() == null ? BigDecimal.ZERO : s.totalCargo();
            BigDecimal abono = s.totalAbono() == null ? BigDecimal.ZERO : s.totalAbono();
            totalCargos = totalCargos.add(cargo);
            totalAbonos = totalAbonos.add(abono);
            Cuenta actual = porId.get(s.cuentaId());
            if (actual == null) {
                throw new NoEncontradoException("Cuenta", s.cuentaId());
            }
            Set<Long> visitadas = new HashSet<>();
            while (actual != null && visitadas.add(actual.getId())) {
                BigDecimal[] par = acumulado.computeIfAbsent(actual.getId(),
                        id -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
                par[0] = par[0].add(cargo);
                par[1] = par[1].add(abono);
                Cuenta padre = actual.getPadre();
                actual = padre == null ? null : porId.getOrDefault(padre.getId(), padre);
            }
        }

        List<Cuenta> ordenadas = new ArrayList<>(acumulado.keySet().stream().map(porId::get).toList());
        ordenadas.sort(Comparator.comparing(Cuenta::getCodigo));
        List<FilaBalanza> filas = new ArrayList<>();
        for (Cuenta c : ordenadas) {
            BigDecimal[] par = acumulado.get(c.getId());
            BigDecimal cargos = Dinero.redondear(par[0]);
            BigDecimal abonos = Dinero.redondear(par[1]);
            BigDecimal saldo = c.getNaturaleza() == NaturalezaCuenta.Deudora ? cargos.subtract(abonos)
                    : abonos.subtract(cargos);
            filas.add(new FilaBalanza(c.getId(), c.getCodigo(), c.getNombre(), c.getNaturaleza(),
                    Boolean.TRUE.equals(c.getImputable()), cargos, abonos, Dinero.redondear(saldo)));
        }
        return new Balanza(desde, hasta, filas, Dinero.redondear(totalCargos), Dinero.redondear(totalAbonos));
    }
}
