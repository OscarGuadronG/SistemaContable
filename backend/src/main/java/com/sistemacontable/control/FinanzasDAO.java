package com.sistemacontable.control;

import com.sistemacontable.entity.*;
import com.sistemacontable.control.exception.NoEncontradoException;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Acceso a documentos financieros. El bloqueo de cabecera serializa pagos y devoluciones. */
@Stateless
@LocalBean
public class FinanzasDAO {
    @PersistenceContext(unitName = "contablePU")
    private EntityManager em;

    public <T> T buscar(Class<T> tipo, Long id, boolean bloquear) {
        if (id == null) throw new IllegalArgumentException("El identificador es obligatorio");
        T valor = bloquear ? em.find(tipo, id, LockModeType.PESSIMISTIC_WRITE) : em.find(tipo, id);
        if (valor == null) throw new NoEncontradoException(tipo.getSimpleName(), id);
        return valor;
    }
    public void guardar(Object valor) { em.persist(valor); }
    public void flush() { em.flush(); }
    public List<Compra> comprasCredito() {
        return em.createQuery("SELECT c FROM Compra c WHERE c.formaPago = :f ORDER BY c.fecha, c.id", Compra.class)
                .setParameter("f", com.sistemacontable.entity.enums.FormaPago.Credito).getResultList();
    }
    public List<Venta> ventasCredito() {
        return em.createQuery("SELECT v FROM Venta v WHERE v.formaPago = :f ORDER BY v.fecha, v.id", Venta.class)
                .setParameter("f", com.sistemacontable.entity.enums.FormaPago.Credito).getResultList();
    }
    public List<PagoCredito> pagos(boolean venta, Long id) {
        return em.createQuery("SELECT p FROM PagoCredito p WHERE p." + (venta ? "venta" : "compra")
                + ".id = :id ORDER BY p.fecha, p.id", PagoCredito.class).setParameter("id", id).getResultList();
    }
    public BigDecimal compensadoCompra(Long id) {
        return suma("SELECT SUM(d.montoCompensado) FROM DevolucionCompra d WHERE d.compra.id = :id", id);
    }
    public BigDecimal devueltoVenta(Long id) {
        return suma("SELECT SUM(d.monto) FROM DevolucionVenta d WHERE d.venta.id = :id", id);
    }
    /** Importe exacto reintegrado a Clientes, incluido IVA y su redondeo por devolución. */
    public BigDecimal reintegroVenta(Long id, Long cuentaId) {
        BigDecimal monto = em.createQuery("SELECT SUM(l.abono) FROM PartidaDetalle l, DevolucionVenta d WHERE l.partida.idDevolucionVenta = d.id AND d.venta.id = :id AND l.cuenta.id = :cuenta", BigDecimal.class)
                .setParameter("id", id).setParameter("cuenta", cuentaId).getSingleResult();
        return monto == null ? BigDecimal.ZERO : monto;
    }
    private BigDecimal suma(String query, Long id) {
        BigDecimal v = em.createQuery(query, BigDecimal.class).setParameter("id", id).getSingleResult();
        return v == null ? BigDecimal.ZERO : v;
    }
    public List<CompraDetalle> lineasCompra(Long id) {
        return em.createQuery("SELECT d FROM CompraDetalle d WHERE d.compra.id = :id ORDER BY d.id", CompraDetalle.class)
                .setParameter("id", id).getResultList();
    }
    public List<VentaDetalle> lineasVenta(Long id) {
        return em.createQuery("SELECT d FROM VentaDetalle d WHERE d.venta.id = :id ORDER BY d.id", VentaDetalle.class)
                .setParameter("id", id).getResultList();
    }
    public List<KardexDetalle> entradasCompra(Long id) {
        return em.createQuery("SELECT d FROM KardexDetalle d WHERE d.kardex.compra.id = :id AND d.kardex.destino IS NOT NULL ORDER BY d.id", KardexDetalle.class)
                .setParameter("id", id).getResultList();
    }
    public BigDecimal cantidadDevuelta(boolean venta, Long documento, Long lote, Long almacen) {
        String entidad = venta ? "DevolucionVentaDetalle" : "DevolucionCompraDetalle";
        var consulta = em.createQuery("SELECT SUM(d.cantidad) FROM " + entidad + " d WHERE d.devolucion."
                + (venta ? "venta" : "compra") + ".id = :id AND d.lote.id = :l" + (venta ? "" : " AND d.almacen.id = :a"), BigDecimal.class)
                .setParameter("id", documento).setParameter("l", lote);
        if (!venta) consulta.setParameter("a", almacen);
        BigDecimal v = consulta.getSingleResult();
        return v == null ? BigDecimal.ZERO : v;
    }
    public List<DevolucionCompra> devolucionesCompra(LocalDate desde, LocalDate hasta, Long id) {
        return devoluciones(DevolucionCompra.class,"compra",desde,hasta,id);
    }
    public List<DevolucionVenta> devolucionesVenta(LocalDate desde, LocalDate hasta, Long id) {
        return devoluciones(DevolucionVenta.class,"venta",desde,hasta,id);
    }
    private <T> List<T> devoluciones(Class<T> tipo,String documento,LocalDate desde,LocalDate hasta,Long id) {
        String jpql="SELECT d FROM "+tipo.getSimpleName()+" d WHERE 1=1";
        if(desde!=null)jpql+=" AND d.fecha >= :desde";
        if(hasta!=null)jpql+=" AND d.fecha <= :hasta";
        if(id!=null)jpql+=" AND d."+documento+".id = :id";
        var query=em.createQuery(jpql+" ORDER BY d.fecha,d.id",tipo);
        if(desde!=null)query.setParameter("desde",desde);
        if(hasta!=null)query.setParameter("hasta",hasta);
        if(id!=null)query.setParameter("id",id);
        return query.getResultList();
    }
    public List<DevolucionCompraDetalle> detallesCompra(Long id) {
        return em.createQuery("SELECT d FROM DevolucionCompraDetalle d WHERE d.devolucion.id = :id ORDER BY d.id", DevolucionCompraDetalle.class).setParameter("id", id).getResultList();
    }
    public List<DevolucionVentaDetalle> detallesVenta(Long id) {
        return em.createQuery("SELECT d FROM DevolucionVentaDetalle d WHERE d.devolucion.id = :id ORDER BY d.id", DevolucionVentaDetalle.class).setParameter("id", id).getResultList();
    }
    public List<Kardex> kardexDevolucion(boolean venta, Long id) {
        return em.createQuery("SELECT k FROM Kardex k WHERE k." + (venta ? "idDevolucionVenta" : "idDevolucionCompra") + " = :id ORDER BY k.fecha, k.id", Kardex.class).setParameter("id", id).getResultList();
    }
    public Partida partidaDevolucion(boolean venta, Long id) {
        return em.createQuery("SELECT p FROM Partida p WHERE p." + (venta ? "idDevolucionVenta" : "idDevolucionCompra") + " = :id ORDER BY p.id", Partida.class).setParameter("id", id).getResultStream().findFirst().orElse(null);
    }
}
