package com.sistemacontable.control.service;
import com.sistemacontable.entity.Partida;
import java.math.BigDecimal;
public record ResumenPartida(Partida partida,BigDecimal totalCargo,BigDecimal totalAbono) { }
