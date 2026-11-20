package com.sistemacontable.control.service;
import com.sistemacontable.entity.Partida;
import java.math.BigDecimal;
import com.sistemacontable.entity.PartidaDetalle;
import java.util.List;
public record ResultadoTraslado(MovimientoKardex movimiento, Partida partida, List<PartidaDetalle> partidaLineas, BigDecimal gastosAdicionales, BigDecimal costoTotal) {
    public ResultadoTraslado { partidaLineas = partidaLineas == null ? List.of() : List.copyOf(partidaLineas); }
}
