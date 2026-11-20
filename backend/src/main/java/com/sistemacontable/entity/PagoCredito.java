package com.sistemacontable.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.sistemacontable.entity.enums.FormaPago;

/** Registro inmutable de la operación y sus importes históricos. */
@Entity
@Table(name = "pago_credito", schema = "public")
public class PagoCredito extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra")
    private Compra compra;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta")
    private Venta venta;
    @Column(name = "fecha")
    private LocalDate fecha;
    @Column(name = "capital")
    private BigDecimal capital;
    @Column(name = "interes")
    private BigDecimal interes;
    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pago")
    private FormaPago formaPago;
    @Column(name = "numero_cheque")
    private String numeroCheque;
    @Column(name = "descripcion")
    private String descripcion;
    public Compra getCompra() { return compra; }
    public void setCompra(Compra value) { this.compra = value; }
    public Venta getVenta() { return venta; }
    public void setVenta(Venta value) { this.venta = value; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate value) { this.fecha = value; }
    public BigDecimal getCapital() { return capital; }
    public void setCapital(BigDecimal value) { this.capital = value; }
    public BigDecimal getInteres() { return interes; }
    public void setInteres(BigDecimal value) { this.interes = value; }
    public FormaPago getFormaPago() { return formaPago; }
    public void setFormaPago(FormaPago value) { this.formaPago = value; }
    public String getNumeroCheque() { return numeroCheque; }
    public void setNumeroCheque(String value) { this.numeroCheque = value; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String value) { this.descripcion = value; }
}
