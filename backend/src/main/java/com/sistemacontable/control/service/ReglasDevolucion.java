package com.sistemacontable.control.service;
import com.sistemacontable.control.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
public final class ReglasDevolucion {
    private ReglasDevolucion() { }
    public static void plazo(LocalDate documento, LocalDate fecha, int maximo) {
        if (documento == null || fecha == null) throw new ReglaNegocioException("La fecha es obligatoria");
        long dias = ChronoUnit.DAYS.between(documento, fecha);
        if (dias < 0) throw new ReglaNegocioException("La devolución no puede ser anterior al documento");
        if (dias > maximo) throw new ReglaNegocioException("La devolución excede el plazo de " + maximo + " días");
    }
    public static void cantidad(BigDecimal pedida, BigDecimal documento, BigDecimal devuelta) {
        if (pedida == null || pedida.signum() <= 0 || pedida.compareTo(documento.subtract(devuelta)) > 0)
            throw new ReglaNegocioException("La cantidad a devolver excede lo registrado menos las devoluciones anteriores");
    }
}
