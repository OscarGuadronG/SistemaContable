package com.sistemacontable.boundary.dto;

import com.sistemacontable.entity.enums.TipoPartida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

/** Partida contable. {@code lineas} es nulo (y no se serializa) en los listados. */
public record PartidaDto(Long id,
        LocalDate fecha,
        String concepto,
        TipoPartida tipo,
        Long compraId,
        Long ventaId,
        Long terceroId,
        Long periodoId,
        Long idKardex,
        Long idPlanilla,
        Long idPagoCredito,
        Long idDevolucionCompra,
        Long idDevolucionVenta,
        Instant creado,
        String origen,
        List<Linea> lineas) {

    public PartidaDto(Long id,LocalDate fecha,String concepto,TipoPartida tipo,Long compraId,Long ventaId,Long terceroId,List<Linea> lineas) {
        this(id,fecha,concepto,tipo,compraId,ventaId,terceroId,null,null,null,null,null,null,null,null,lineas);
    }

    /** Linea de una partida: exactamente uno de cargo o abono es mayor que cero. */
    public record Linea(Long id, Long cuentaId, CuentaRef cuenta, BigDecimal cargo, BigDecimal abono) {
        public Linea(Long id,Long cuentaId,BigDecimal cargo,BigDecimal abono) { this(id,cuentaId,null,cargo,abono); }
    }
    public record CuentaRef(Long id,Integer codigo,String nombre) { }
}
