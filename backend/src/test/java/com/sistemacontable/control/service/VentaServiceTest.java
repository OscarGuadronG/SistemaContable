package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sistemacontable.control.*;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.*;
import com.sistemacontable.entity.*;
import com.sistemacontable.entity.enums.*;
import com.sistemacontable.security.UsuarioActual;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness=Strictness.LENIENT)
class VentaServiceTest {
    @Mock ClienteDAO clienteDAO; @Mock EmpleadoDAO empleadoDAO; @Mock AlmacenDAO almacenDAO; @Mock VentaDAO ventaDAO;
    @Mock VentaDetalleDAO ventaDetalleDAO; @Mock ExistenciasService existenciasService; @Mock KardexService kardexService;
    @Mock PartidaService partidaService; @Mock ParametroService parametros; @Mock ProductoDAO productoDAO; @Mock UsuarioActual usuarioActual;
    @InjectMocks VentaService servicio;
    private static BigDecimal bd(String s){return new BigDecimal(s);}
    private record Datos(Almacen almacen,Producto producto,Lote lote1,Lote lote2,Cuenta banco,Cuenta rebajas,Cuenta ventas,Cuenta iva,Cuenta costo,Cuenta inventario){}

    private Datos preparar() {
        Cliente c=new Cliente();c.setId(1L);Empleado e=new Empleado();e.setId(2L);Almacen a=new Almacen();a.setId(3L);a.setEstado(true);
        Producto p=new Producto();p.setId(4L);Lote l1=new Lote();l1.setId(5L);l1.setProducto(p);Lote l2=new Lote();l2.setId(6L);l2.setProducto(p);
        Existencias x1=new Existencias();x1.setLote(l1);x1.setCantidad(bd("2"));Existencias x2=new Existencias();x2.setLote(l2);x2.setCantidad(bd("2"));
        when(clienteDAO.buscarPorId(1L)).thenReturn(Optional.of(c));when(empleadoDAO.buscarPorId(2L)).thenReturn(Optional.of(e));
        when(almacenDAO.buscarPorId(3L)).thenReturn(Optional.of(a));when(productoDAO.buscarPorId(4L)).thenReturn(Optional.of(p));
        when(existenciasService.porProductoAlmacen(4L,3L)).thenReturn(new ArrayList<>(List.of(x1,x2)));
        when(existenciasService.costoEntradas(5L,3L)).thenReturn(Optional.of(bd("4")));when(existenciasService.costoEntradas(6L,3L)).thenReturn(Optional.of(bd("6")));
        when(existenciasService.primeraEntrada(5L,3L)).thenReturn(Optional.of(OffsetDateTime.parse("2026-01-01T00:00:00Z")));
        when(existenciasService.primeraEntrada(6L,3L)).thenReturn(Optional.of(OffsetDateTime.parse("2026-02-01T00:00:00Z")));
        when(parametros.decimal("venta.margen")).thenReturn(bd("0.30"));when(parametros.decimal("iva.tasa")).thenReturn(bd("0.13"));
        Cuenta banco=cuenta(11,11031);Cuenta rebajas=cuenta(12,5103);Cuenta ventas=cuenta(13,5101);
        Cuenta iva=cuenta(14,2108);Cuenta costo=cuenta(15,4101);Cuenta inventario=cuenta(16,11081);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_BANCOS)).thenReturn(banco);
        when(partidaService.cuentaConfigurada("cuenta.rebajas.ventas")).thenReturn(rebajas);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_VENTAS)).thenReturn(ventas);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_IVA_DEBITO)).thenReturn(iva);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_COSTO_VENTAS)).thenReturn(costo);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO)).thenReturn(inventario);
        when(kardexService.registrar(eq(TipoMovimiento.Salida),eq(a),isNull(),isNull(),any(Venta.class),anyString(),any(OffsetDateTime.class),anyList()))
                .thenReturn(new MovimientoKardex(new Kardex(),List.of()));
        when(partidaService.registrar(any(),any(),anyString(),any(),any(),any(),anyList())).thenReturn(new Partida());
        return new Datos(a,p,l1,l2,banco,rebajas,ventas,iva,costo,inventario);
    }

    private static Cuenta cuenta(long id,int codigo){Cuenta c=new Cuenta();c.setId(id);c.setCodigo(codigo);c.setActivo(true);c.setImputable(true);c.setNaturaleza(NaturalezaCuenta.Deudora);return c;}

    @Test void determinaPrecioConMargenYConsumeLotesEnFIFO() {
        Datos d=preparar();
        VentaCmd cmd=new VentaCmd(1L,2L,3L,LocalDate.of(2026,11,5),"F-1","venta",FormaPago.Cheque,"CH-3",bd("1"),List.of(new LineaVentaCmd(4L,bd("3"))));
        ResultadoVenta r=servicio.registrar(cmd);
        assertEquals(bd("19.50"),r.subtotal());assertEquals(bd("2.41"),r.iva());assertEquals(bd("20.91"),r.total());assertEquals(bd("14.00"),r.costoVentas());
        assertEquals(2,r.detalles().size());assertSame(d.lote1(),r.detalles().get(0).getLote());assertEquals(bd("2"),r.detalles().get(0).getCantidad());
        assertSame(d.lote2(),r.detalles().get(1).getLote());assertEquals(bd("1"),r.detalles().get(1).getCantidad());
        assertEquals(bd("6.50"),r.detalles().get(0).getPrecio());assertEquals(FormaPago.Cheque,r.venta().getFormaPago());
        ArgumentCaptor<List<LineaAsiento>> asiento=ArgumentCaptor.forClass(List.class);
        verify(partidaService).registrar(eq(TipoPartida.Ingreso),eq(cmd.fecha()),anyString(),isNull(),any(Venta.class),isNull(),asiento.capture());
        assertEquals(bd("20.91"),asiento.getValue().stream().filter(l->l.cuenta()==d.banco()).findFirst().orElseThrow().cargo());
        assertEquals(bd("14.00"),asiento.getValue().stream().filter(l->l.cuenta()==d.costo()).findFirst().orElseThrow().cargo());
    }

    @Test void rechazaCantidadMayorAlStockAntesDeEscribir() {
        preparar(); VentaCmd cmd=new VentaCmd(1L,2L,3L,LocalDate.now(),null,null,FormaPago.Contado,null,BigDecimal.ZERO,List.of(new LineaVentaCmd(4L,bd("5"))));
        assertThrows(ReglaNegocioException.class,()->servicio.registrar(cmd));
        verify(ventaDAO,never()).crear(any());verify(existenciasService,never()).disminuir(any(),any(),any());verify(partidaService,never()).registrar(any(),any(),anyString(),any(),any(),any(),anyList());
    }
}
