package com.sistemacontable.entity;

import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.Instant;

/**
 * Tabla {@code public.partida}.
 */
@Entity
@Table(name = "partida", schema = "public")
public class Partida extends EntidadBase {

    @Column(name = "fecha", nullable = false)
    @NotNull
    private LocalDate fecha = LocalDate.now();

    @Column(name = "concepto", nullable = false)
    @NotBlank
    private String concepto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    @NotNull
    private TipoPartida tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tercero")
    private Tercero tercero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_periodo", nullable = false)
    @NotNull
    private Periodo periodo;

    @Column(name = "id_kardex")
    private Long idKardex;

    @Column(name = "id_planilla")
    private Long idPlanilla;

    @Column(name = "id_pago_credito")
    private Long idPagoCredito;

    @Column(name = "id_devolucion_compra")
    private Long idDevolucionCompra;

    @Column(name = "id_devolucion_venta")
    private Long idDevolucionVenta;

    @Column(name = "creado", nullable = false)
    private Instant creado = Instant.now();

    @Column(name = "id_compra", insertable = false, updatable = false)
    private Long idCompra;

    @Column(name = "id_venta", insertable = false, updatable = false)
    private Long idVenta;

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getConcepto() {
        return concepto;
    }

    public void setConcepto(String concepto) {
        this.concepto = concepto;
    }

    public TipoPartida getTipo() {
        return tipo;
    }

    public void setTipo(TipoPartida tipo) {
        this.tipo = tipo;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public Tercero getTercero() {
        return tercero;
    }

    public void setTercero(Tercero tercero) {
        this.tercero = tercero;
    }

    public Periodo getPeriodo() { return periodo; }
    public void setPeriodo(Periodo periodo) { this.periodo = periodo; }
    public Long getIdKardex() { return idKardex; }
    public void setIdKardex(Long idKardex) { this.idKardex = idKardex; }
    public Long getIdPlanilla() { return idPlanilla; }
    public void setIdPlanilla(Long idPlanilla) { this.idPlanilla = idPlanilla; }
    public Long getIdPagoCredito() { return idPagoCredito; }
    public void setIdPagoCredito(Long idPagoCredito) { this.idPagoCredito = idPagoCredito; }
    public Long getIdDevolucionCompra() { return idDevolucionCompra; }
    public void setIdDevolucionCompra(Long idDevolucionCompra) { this.idDevolucionCompra = idDevolucionCompra; }
    public Long getIdDevolucionVenta() { return idDevolucionVenta; }
    public void setIdDevolucionVenta(Long idDevolucionVenta) { this.idDevolucionVenta = idDevolucionVenta; }
    public Instant getCreado() { return creado; }
    public void setCreado(Instant creado) { this.creado = creado; }
    public Long getIdCompra() { return idCompra != null ? idCompra : compra == null ? null : compra.getId(); }
    public Long getIdVenta() { return idVenta != null ? idVenta : venta == null ? null : venta.getId(); }

    @Override
    public String toString() {
        return "Partida[id=" + getId() + ", fecha=" + fecha + ", concepto=" + concepto + ", tipo=" + tipo + "]";
    }
}
