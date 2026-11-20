package com.sistemacontable.control.service;
import static org.junit.jupiter.api.Assertions.*;
import com.sistemacontable.control.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
class ReglasDevolucionTest {
    @Test void admiteLimiteYRechazaFueraDePlazoOAnterior() {
        LocalDate fecha=LocalDate.of(2026,7,1);
        assertDoesNotThrow(()->ReglasDevolucion.plazo(fecha,fecha.plusDays(30),30));
        assertThrows(ReglaNegocioException.class,()->ReglasDevolucion.plazo(fecha,fecha.plusDays(31),30));
        assertThrows(ReglaNegocioException.class,()->ReglasDevolucion.plazo(fecha,fecha.minusDays(1),30));
    }
    @Test void restaCantidadesDevueltas() {
        assertDoesNotThrow(()->ReglasDevolucion.cantidad(new BigDecimal("8"),BigDecimal.TEN,new BigDecimal("2")));
        assertThrows(ReglaNegocioException.class,()->ReglasDevolucion.cantidad(new BigDecimal("9"),BigDecimal.TEN,new BigDecimal("2")));
        assertThrows(ReglaNegocioException.class,()->ReglasDevolucion.cantidad(BigDecimal.ZERO,BigDecimal.TEN,BigDecimal.ZERO));
    }
}
