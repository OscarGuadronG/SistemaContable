package com.sistemacontable.control.service;
import com.sistemacontable.boundary.dto.CreditoDto.Simulacion;
import com.sistemacontable.control.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
/** Regla compartida por simulación y pago; meses iniciados, sin interés compuesto. */
public final class InteresCredito {
    private InteresCredito() { }
    public static Simulacion calcular(LocalDate origen, LocalDate fecha, BigDecimal capital,
            BigDecimal tasa, int diasExoneracion) {
        if (origen == null || fecha == null || capital == null || tasa == null)
            throw new ReglaNegocioException("Faltan datos para calcular el interés");
        long dias = ChronoUnit.DAYS.between(origen, fecha);
        if (dias < 0) throw new ReglaNegocioException("La fecha de pago no puede ser anterior al documento");
        long meses = dias < diasExoneracion ? 0 : (dias + 29) / 30;
        return new Simulacion(fecha, dias, meses, Dinero.redondear(capital.multiply(tasa).multiply(BigDecimal.valueOf(meses))));
    }
}
