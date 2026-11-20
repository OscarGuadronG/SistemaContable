package com.sistemacontable.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Parámetro configurable de negocio. */
@Entity
@jakarta.persistence.Cacheable(false)
@Table(name = "parametro", schema = "public")
public class Parametro {
    @Id @Column(name = "clave", length = 100) private String clave;
    @Column(name = "valor", nullable = false) private String valor;
    @Column(name = "descripcion") private String descripcion;
    public String getClave() { return clave; }
    public void setClave(String v) { clave = v; }
    public String getValor() { return valor; }
    public void setValor(String v) { valor = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = v; }
}
