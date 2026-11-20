package com.sistemacontable.boundary.dto;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record CreditoDto(String tipo, Long documentoId, LocalDate fecha, String numeroDocumento,
        Contraparte contraparte, BigDecimal total, BigDecimal pagadoCapital, BigDecimal saldo,
        BigDecimal tasaInteres, Integer plazoMeses, LocalDate fechaVencimiento, boolean vencido,
        List<Pago> pagos, Simulacion simulacion) {
    public record Contraparte(Long id, String nombre) { }
    public record Pago(Long id, LocalDate fecha, BigDecimal capital, BigDecimal interes, FormaPago formaPago,
            String numeroCheque, String descripcion) { }
    public record Simulacion(LocalDate fecha, long dias, long meses, BigDecimal interes) { }
    public record ResultadoPago(Pago pago, PartidaDto partida) { }
}
