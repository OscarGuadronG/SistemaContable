package com.sistemacontable.control.service;
import static org.junit.jupiter.api.Assertions.*;
import com.sistemacontable.control.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
class InteresCreditoTest {
    private final LocalDate fecha = LocalDate.of(2026,7,1);
    @Test void exoneracionAntesDeTreintaDias() {
        var calculo = InteresCredito.calcular(fecha,fecha.plusDays(29),new BigDecimal("500"),new BigDecimal("0.02"),30);
        assertEquals(0,calculo.meses()); assertEquals(new BigDecimal("0.00"),calculo.interes());
    }
    @Test void cuarentaYCincoDiasSonDosMeses() {
        var calculo = InteresCredito.calcular(fecha,fecha.plusDays(45),new BigDecimal("500"),new BigDecimal("0.02"),30);
        assertEquals(45,calculo.dias()); assertEquals(2,calculo.meses()); assertEquals(new BigDecimal("20.00"),calculo.interes());
    }
    @Test void exactamenteTreintaDiasDevengaUnMesYRedondea() {
        assertEquals(new BigDecimal("3.33"),InteresCredito.calcular(fecha,fecha.plusDays(30),new BigDecimal("166.65"),new BigDecimal("0.02"),30).interes());
        assertThrows(ReglaNegocioException.class,()->InteresCredito.calcular(fecha,fecha.minusDays(1),BigDecimal.ONE,BigDecimal.ONE,30));
    }
}
