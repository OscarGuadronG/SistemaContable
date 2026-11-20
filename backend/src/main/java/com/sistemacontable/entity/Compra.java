package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.sistemacontable.entity.enums.FormaPago;

/**
 * Tabla {@code public.compra}.
 */
@Entity
@Cacheable(false)
@Table(name = "compra", schema = "public")
public class Compra extends EntidadBase {

    @Column(name = "fecha", nullable = false)
    @NotNull
    private LocalDate fecha = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    @NotNull
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", nullable = false)
    @NotNull
    private Empleado empleado;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "numero_documento") private String numeroDocumento;
    @Enumerated(EnumType.STRING) @Column(name = "forma_pago", nullable = false) private FormaPago formaPago = FormaPago.Contado;
    @Column(name = "numero_cheque") private String numeroCheque;
    @Column(name = "descuento", nullable = false) private BigDecimal descuento = BigDecimal.ZERO;
    @Column(name = "gastos_adicionales", nullable = false) private BigDecimal gastosAdicionales = BigDecimal.ZERO;
    @Column(name = "subtotal", nullable = false) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "iva", nullable = false) private BigDecimal iva = BigDecimal.ZERO;
    @Column(name = "total", nullable = false) private BigDecimal total = BigDecimal.ZERO;
    @Column(name = "tasa_interes") private BigDecimal tasaInteres;
    @Column(name = "plazo_meses") private Integer plazoMeses;
    @Column(name = "fecha_vencimiento") private LocalDate fechaVencimiento;

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String v) { numeroDocumento = v; }
    public FormaPago getFormaPago() { return formaPago; }
    public void setFormaPago(FormaPago v) { formaPago = v; }
    public String getNumeroCheque() { return numeroCheque; }
    public void setNumeroCheque(String v) { numeroCheque = v; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal v) { descuento = v; }
    public BigDecimal getGastosAdicionales() { return gastosAdicionales; }
    public void setGastosAdicionales(BigDecimal v) { gastosAdicionales = v; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { subtotal = v; }
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal v) { iva = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { total = v; }
    public BigDecimal getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(BigDecimal v) { tasaInteres = v; }
    public Integer getPlazoMeses() { return plazoMeses; }
    public void setPlazoMeses(Integer v) { plazoMeses = v; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate v) { fechaVencimiento = v; }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    @Override
    public String toString() {
        return "Compra[id=" + getId() + ", fecha=" + fecha + ", descripcion=" + descripcion + "]";
    }
}
