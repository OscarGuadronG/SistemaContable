package com.sistemacontable.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.sistemacontable.entity.enums.FormaPago;

/** Registro inmutable de la operación y sus importes históricos. */
@Entity
@Table(name = "devolucion_venta", schema = "public")
public class DevolucionVenta extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta")
    private Venta venta;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;
    @Column(name = "fecha")
    private LocalDate fecha;
    @Column(name = "monto")
    private BigDecimal monto;
    @Column(name = "costo")
    private BigDecimal costo;
    @Column(name = "motivo")
    private String motivo;
    public Venta getVenta() { return venta; }
    public void setVenta(Venta value) { this.venta = value; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado value) { this.empleado = value; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate value) { this.fecha = value; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal value) { this.monto = value; }
    public BigDecimal getCosto() { return costo; }
    public void setCosto(BigDecimal value) { this.costo = value; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String value) { this.motivo = value; }
}
