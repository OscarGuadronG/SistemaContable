package com.sistemacontable.control.service;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Periodo;
public record CierrePeriodo(Periodo periodo, Partida partidaCierre, Partida partidaApertura) { }
