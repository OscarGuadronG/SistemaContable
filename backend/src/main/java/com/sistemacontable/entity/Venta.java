package com.sistemacontable.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.sistemacontable.entity.enums.FormaPago;

/**
 * Tabla {@code public.ventas}.
 */
@Entity
@Cacheable(false)
@Table(name = "ventas", schema = "public")
public class Venta extends EntidadBase {

    @Column(name = "fecha", nullable = false)
    @NotNull
    private LocalDate fecha = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    @NotNull
    private Cliente cliente;

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
    @Column(name = "subtotal", nullable = false) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "iva", nullable = false) private BigDecimal iva = BigDecimal.ZERO;
    @Column(name = "total", nullable = false) private BigDecimal total = BigDecimal.ZERO;
    @Column(name = "costo_total", nullable = false) private BigDecimal costoTotal = BigDecimal.ZERO;
    @Column(name = "tasa_interes") private BigDecimal tasaInteres;
    @Column(name = "plazo_meses") private Integer plazoMeses;
    @Column(name = "fecha_vencimiento") private LocalDate fechaVencimiento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_almacen", nullable = false) private Almacen almacen;

    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String v) { numeroDocumento = v; }
    public FormaPago getFormaPago() { return formaPago; }
    public void setFormaPago(FormaPago v) { formaPago = v; }
    public String getNumeroCheque() { return numeroCheque; }
    public void setNumeroCheque(String v) { numeroCheque = v; }
    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal v) { descuento = v; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { subtotal = v; }
    public BigDecimal getIva() { return iva; }
    public void setIva(BigDecimal v) { iva = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { total = v; }
    public BigDecimal getCostoTotal() { return costoTotal; }
    public void setCostoTotal(BigDecimal v) { costoTotal = v; }
    public BigDecimal getTasaInteres() { return tasaInteres; }
    public void setTasaInteres(BigDecimal v) { tasaInteres = v; }
    public Integer getPlazoMeses() { return plazoMeses; }
    public void setPlazoMeses(Integer v) { plazoMeses = v; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate v) { fechaVencimiento = v; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen v) { almacen = v; }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
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
        return "Venta[id=" + getId() + ", fecha=" + fecha + ", descripcion=" + descripcion + "]";
    }
}
