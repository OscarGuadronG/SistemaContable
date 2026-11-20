package com.sistemacontable.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.sistemacontable.entity.enums.FormaPago;

/** Registro inmutable de la operación y sus importes históricos. */
@Entity
@Table(name = "devolucion_venta_detalle", schema = "public")
public class DevolucionVentaDetalle extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_devolucion")
    private DevolucionVenta devolucion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote")
    private Lote lote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen")
    private Almacen almacen;
    @Column(name = "cantidad")
    private BigDecimal cantidad;
    @Column(name = "precio")
    private BigDecimal precio;
    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;
    public DevolucionVenta getDevolucion() { return devolucion; }
    public void setDevolucion(DevolucionVenta value) { this.devolucion = value; }
    public Lote getLote() { return lote; }
    public void setLote(Lote value) { this.lote = value; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen value) { this.almacen = value; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal value) { this.cantidad = value; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal value) { this.precio = value; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal value) { this.costoUnitario = value; }
}
