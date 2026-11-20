package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.sistemacontable.boundary.dto.*;
import com.sistemacontable.control.service.cmd.*;
import com.sistemacontable.control.FinanzasDAO;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.*;
import com.sistemacontable.entity.enums.*;
import com.sistemacontable.security.UsuarioActual;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FinanzasServiceTest {
    FinanzasDAO dao; ParametroService parametros; PartidaService partidas;
    ExistenciasService stock; KardexService kardex; UsuarioActual usuario;
    CreditoService creditos; DevolucionService devoluciones;
    final LocalDate fecha=LocalDate.of(2026,7,1);
    final List<List<LineaAsiento>> asientos=new ArrayList<>();
    final Map<Class<?>,Object> guardados=new HashMap<>();
    Empleado empleado; Lote lote; Almacen almacen; Compra compra; Venta venta;
    @BeforeEach void preparar() throws Exception {
        dao=mock(FinanzasDAO.class); parametros=mock(ParametroService.class); partidas=mock(PartidaService.class);
        stock=mock(ExistenciasService.class); kardex=mock(KardexService.class); usuario=mock(UsuarioActual.class);
        creditos=new CreditoService(); devoluciones=new DevolucionService();
        for(Object servicio:List.of(creditos,devoluciones)) for(Field f:servicio.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            Object d=Map.of(FinanzasDAO.class,dao,ParametroService.class,parametros,PartidaService.class,partidas,
                    ExistenciasService.class,stock,KardexService.class,kardex,UsuarioActual.class,usuario).get(f.getType());
            if(d!=null)f.set(servicio,d);
        }
        when(parametros.entero(anyString())).thenReturn(30);
        when(parametros.decimal("iva.tasa")).thenReturn(new BigDecimal("0.13"));
        when(partidas.cuentaConfigurada(anyString())).thenAnswer(i->{ Cuenta c=new Cuenta(); c.setId((long)i.getArgument(0,String.class).hashCode()); c.setActivo(true); c.setImputable(true); return c; });
        when(partidas.registrar(any(),any(),anyString(),any(),any(),any(),anyList())).thenAnswer(i->{
            List<LineaAsiento> lineas=i.getArgument(6); ReglasPartida.validar(lineas); asientos.add(lineas);
            Partida p=new Partida();p.setId(99L);p.setFecha(i.getArgument(1));p.setTipo(i.getArgument(0));return p;
        });
        doAnswer(i->{ Object e=i.getArgument(0); if(e instanceof EntidadBase b)b.setId(80L);guardados.put(e.getClass(),e);return null; }).when(dao).guardar(any());
        empleado=new Empleado();empleado.setId(1L);
        almacen=new Almacen();almacen.setId(1L);almacen.setEstado(true);
        lote=new Lote();lote.setId(2L);
        when(dao.buscar(Empleado.class,1L,false)).thenReturn(empleado);
        when(dao.buscar(Lote.class,2L,false)).thenReturn(lote);
        when(dao.buscar(Almacen.class,1L,false)).thenReturn(almacen);
        when(dao.buscar(DevolucionCompra.class,80L,false)).thenAnswer(i->guardados.get(DevolucionCompra.class));
        when(dao.buscar(DevolucionVenta.class,80L,false)).thenAnswer(i->guardados.get(DevolucionVenta.class));
        when(dao.cantidadDevuelta(anyBoolean(),anyLong(),anyLong(),anyLong())).thenReturn(BigDecimal.ZERO);
        when(kardex.registrar(any(),any(),any(),any(),any(),anyString(),anyList())).thenAnswer(i->{Kardex k=new Kardex();k.setId(90L);return new MovimientoKardex(k,List.of());});
        Proveedor proveedor=new Proveedor();proveedor.setId(3L);proveedor.setNombre("Proveedor");
        compra=new Compra();compra.setId(10L);compra.setFecha(fecha);compra.setFormaPago(FormaPago.Credito);compra.setProveedor(proveedor);
        compra.setTotal(new BigDecimal("1130"));compra.setTasaInteres(new BigDecimal("0.02"));compra.setFechaVencimiento(fecha.plusMonths(3));
        when(dao.buscar(Compra.class,10L,true)).thenReturn(compra);
        when(dao.compensadoCompra(10L)).thenReturn(BigDecimal.ZERO);
        Cliente cliente=new Cliente();cliente.setId(4L);cliente.setNombre("Cliente");
        venta=new Venta();venta.setId(11L);venta.setFecha(fecha);venta.setFormaPago(FormaPago.Credito);venta.setCliente(cliente);
        venta.setSubtotal(new BigDecimal("390"));venta.setDescuento(BigDecimal.ZERO);venta.setIva(new BigDecimal("50.70"));venta.setTotal(new BigDecimal("440.70"));venta.setTasaInteres(new BigDecimal("0.02"));
        when(dao.buscar(Venta.class,11L,true)).thenReturn(venta);
        when(dao.devueltoVenta(11L)).thenReturn(BigDecimal.ZERO);
    }
    @Test void pagoCompraCuarentaYCincoDiasGuardaInteresYPartidaCuadrada() {
        var resultado=creditos.pagar(new PagoCreditoCmd("compra",10L,fecha.plusDays(45),new BigDecimal("500"),FormaPago.Cheque,"001","Abono"));
        assertEquals(new BigDecimal("20.00"),resultado.pago().interes());
        assertEquals(new BigDecimal("520.00"),asientos.getFirst().stream().map(LineaAsiento::cargo).reduce(BigDecimal.ZERO,BigDecimal::add));
        assertEquals(1,asientos.size());verify(dao).buscar(Compra.class,10L,true);
    }
    @Test void cobroVentaAntesDeTreintaDiasExoneraYSobrepagoNoEscribe() {
        var r=creditos.pagar(new PagoCreditoCmd("venta",11L,fecha.plusDays(29),new BigDecimal("200"),FormaPago.Contado,null,null));
        assertEquals(new BigDecimal("0.00"),r.pago().interes());assertEquals(2,asientos.getFirst().size());
        assertThrows(ReglaNegocioException.class,()->creditos.pagar(new PagoCreditoCmd("venta",11L,fecha.plusDays(29),new BigDecimal("500"),FormaPago.Contado,null,null)));
        assertEquals(1,asientos.size());
    }
    @Test void devolucionProveedorCompensaCincuentaPorCientoDelCostoConGastos() {
        CompraDetalle cd=new CompraDetalle();cd.setLote(lote);cd.setAlmacen(almacen);cd.setCantidad(BigDecimal.TEN);
        when(dao.lineasCompra(10L)).thenReturn(List.of(cd));
        Kardex k=new Kardex();k.setDestino(almacen);KardexDetalle kd=new KardexDetalle();kd.setKardex(k);kd.setLote(lote);kd.setCantidad(BigDecimal.TEN);kd.setPrecioCosto(new BigDecimal("100"));
        when(dao.entradasCompra(10L)).thenReturn(List.of(kd));
        var r=devoluciones.comprar(new DevolucionCompraCmd(10L,fecha.plusDays(20),new BigDecimal("50"),"Defecto",1L,List.of(new LineaDevolucionCmd(2L,1L,new BigDecimal("2")))));
        assertEquals(new BigDecimal("200.00"),r.montoCosto());assertEquals(new BigDecimal("100.00"),r.montoCompensado());
        assertEquals(3,asientos.getFirst().size());verify(stock).disminuir(lote,almacen,new BigDecimal("2"));
    }
    @Test void devolucionClienteReviertePrecioIvaYCostoHistorico() {
        VentaDetalle vd=new VentaDetalle();vd.setLote(lote);vd.setAlmacen(almacen);vd.setCantidad(new BigDecimal("3"));vd.setPrecio(new BigDecimal("130"));vd.setCostoUnitario(new BigDecimal("100"));
        when(dao.lineasVenta(11L)).thenReturn(List.of(vd));
        var r=devoluciones.vender(new DevolucionVentaCmd(11L,fecha.plusDays(20),"Cliente",1L,List.of(new LineaDevolucionCmd(2L,1L,BigDecimal.ONE))));
        assertEquals(new BigDecimal("130.00"),r.monto());assertEquals(new BigDecimal("100.00"),r.costo());
        assertEquals(new BigDecimal("246.90"),asientos.getFirst().stream().map(LineaAsiento::cargo).reduce(BigDecimal.ZERO,BigDecimal::add));
        verify(stock).aumentar(lote,almacen,BigDecimal.ONE);
    }
    @Test void cantidadesRepetidasSeAcumulanYNoSuperanLoVendido() {
        VentaDetalle vd=new VentaDetalle();vd.setLote(lote);vd.setAlmacen(almacen);vd.setCantidad(new BigDecimal("3"));
        when(dao.lineasVenta(11L)).thenReturn(List.of(vd));
        assertThrows(ReglaNegocioException.class,()->devoluciones.vender(new DevolucionVentaCmd(11L,fecha.plusDays(20),null,1L,
                List.of(new LineaDevolucionCmd(2L,1L,new BigDecimal("2")),new LineaDevolucionCmd(2L,1L,new BigDecimal("2"))))));
        verify(dao,never()).guardar(any());
    }
}
