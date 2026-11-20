package com.sistemacontable.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.sistemacontable.entity.enums.FormaPago;

/** Registro inmutable de la operación y sus importes históricos. */
@Entity
@Table(name = "devolucion_compra", schema = "public")
public class DevolucionCompra extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra")
    private Compra compra;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado")
    private Empleado empleado;
    @Column(name = "fecha")
    private LocalDate fecha;
    @Column(name = "porcentaje_compensacion")
    private BigDecimal porcentajeCompensacion;
    @Column(name = "monto_costo")
    private BigDecimal montoCosto;
    @Column(name = "monto_compensado")
    private BigDecimal montoCompensado;
    @Column(name = "motivo")
    private String motivo;
    public Compra getCompra() { return compra; }
    public void setCompra(Compra value) { this.compra = value; }
    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado value) { this.empleado = value; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate value) { this.fecha = value; }
    public BigDecimal getPorcentajeCompensacion() { return porcentajeCompensacion; }
    public void setPorcentajeCompensacion(BigDecimal value) { this.porcentajeCompensacion = value; }
    public BigDecimal getMontoCosto() { return montoCosto; }
    public void setMontoCosto(BigDecimal value) { this.montoCosto = value; }
    public BigDecimal getMontoCompensado() { return montoCompensado; }
    public void setMontoCompensado(BigDecimal value) { this.montoCompensado = value; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String value) { this.motivo = value; }
}
