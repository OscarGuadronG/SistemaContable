package com.sistemacontable.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.sistemacontable.entity.enums.FormaPago;

/** Registro inmutable de la operación y sus importes históricos. */
@Entity
@Table(name = "devolucion_compra_detalle", schema = "public")
public class DevolucionCompraDetalle extends EntidadBase {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_devolucion")
    private DevolucionCompra devolucion;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote")
    private Lote lote;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_almacen")
    private Almacen almacen;
    @Column(name = "cantidad")
    private BigDecimal cantidad;
    @Column(name = "costo_unitario")
    private BigDecimal costoUnitario;
    public DevolucionCompra getDevolucion() { return devolucion; }
    public void setDevolucion(DevolucionCompra value) { this.devolucion = value; }
    public Lote getLote() { return lote; }
    public void setLote(Lote value) { this.lote = value; }
    public Almacen getAlmacen() { return almacen; }
    public void setAlmacen(Almacen value) { this.almacen = value; }
    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal value) { this.cantidad = value; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal value) { this.costoUnitario = value; }
}
