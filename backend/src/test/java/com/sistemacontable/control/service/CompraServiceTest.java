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
import java.util.List;
import java.util.Optional;
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
class CompraServiceTest {
    @Mock ProveedorDAO proveedorDAO; @Mock EmpleadoDAO empleadoDAO; @Mock LoteDAO loteDAO; @Mock AlmacenDAO almacenDAO;
    @Mock CompraDAO compraDAO; @Mock CompraDetalleDAO compraDetalleDAO; @Mock ExistenciasService existenciasService;
    @Mock KardexService kardexService; @Mock PartidaService partidaService; @Mock ParametroService parametros;
    @Mock ProductoDAO productoDAO; @Mock UsuarioActual usuarioActual;
    @InjectMocks CompraService servicio;

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private record Datos(Proveedor proveedor,Empleado empleado,Almacen almacen,Lote lote,Cuenta inventario,Cuenta iva,Cuenta descuentos,Cuenta caja,Cuenta banco,Cuenta proveedores) { }

    private Datos preparar(FormaPago pago) {
        Proveedor prov=new Proveedor(); prov.setId(1L); Empleado emp=new Empleado(); emp.setId(2L);
        Almacen alm=new Almacen(); alm.setId(3L); alm.setEstado(true); Lote lote=new Lote(); lote.setId(4L);
        Cuenta inventario=cuenta(11,11081); Cuenta iva=cuenta(12,1110); Cuenta descuentos=cuenta(13,4105);
        Cuenta caja=cuenta(14,1101); Cuenta banco=cuenta(15,11031);Cuenta proveedores=cuenta(16,2101);
        when(proveedorDAO.buscarPorId(1L)).thenReturn(Optional.of(prov)); when(empleadoDAO.buscarPorId(2L)).thenReturn(Optional.of(emp));
        when(almacenDAO.buscarPorId(3L)).thenReturn(Optional.of(alm));when(loteDAO.buscarPorId(4L)).thenReturn(Optional.of(lote));
        when(parametros.decimal("iva.tasa")).thenReturn(bd("0.13"));
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_INVENTARIO)).thenReturn(inventario);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_IVA_CREDITO)).thenReturn(iva);
        when(partidaService.cuentaConfigurada("cuenta.descuentos.compras")).thenReturn(descuentos);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_CAJA)).thenReturn(caja);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_BANCOS)).thenReturn(banco);
        when(partidaService.cuentaConfigurada(ConfigContable.CUENTA_PROVEEDORES)).thenReturn(proveedores);
        MovimientoKardex mov=new MovimientoKardex(new Kardex(),List.of());
        when(kardexService.registrar(eq(TipoMovimiento.Entrada),isNull(),eq(alm),any(Compra.class),isNull(),anyString(),any(OffsetDateTime.class),anyList())).thenReturn(mov);
        when(partidaService.registrar(any(),any(),anyString(),any(),any(),any(),anyList())).thenReturn(new Partida());
        return new Datos(prov,emp,alm,lote,inventario,iva,descuentos,caja,banco,proveedores);
    }

    private static Cuenta cuenta(long id,int codigo) { Cuenta c=new Cuenta();c.setId(id);c.setCodigo(codigo);c.setActivo(true);c.setImputable(true);c.setNaturaleza(NaturalezaCuenta.Deudora);return c; }

    @Test void registraContadoConGastosProrrateadosYDescuento() {
        Datos d=preparar(FormaPago.Contado);
        var cmd=new CompraCmd(1L,2L,LocalDate.of(2026,11,2),"FAC-1","compra",FormaPago.Contado,null,bd("1"),bd("4"),
                List.of(new LineaCompraCmd(4L,null,3L,bd("2"),bd("10"))));
        ResultadoCompra r=servicio.registrar(cmd);
        assertEquals(bd("20.00"),r.subtotal()); assertEquals(bd("2.47"),r.iva()); assertEquals(bd("25.47"),r.total());
        assertEquals(FormaPago.Contado,r.compra().getFormaPago());
        ArgumentCaptor<List<DetalleKardex>> kardex=ArgumentCaptor.forClass(List.class);
        verify(kardexService).registrar(eq(TipoMovimiento.Entrada),isNull(),eq(d.almacen()),any(Compra.class),isNull(),anyString(),any(OffsetDateTime.class),kardex.capture());
        assertEquals(bd("12.0000"),kardex.getValue().get(0).precioCosto());
        ArgumentCaptor<List<LineaAsiento>> asiento=ArgumentCaptor.forClass(List.class);
        verify(partidaService).registrar(eq(TipoPartida.Egreso),eq(cmd.fecha()),anyString(),any(Compra.class),isNull(),isNull(),asiento.capture());
        assertEquals(bd("24.00"),asiento.getValue().stream().filter(l->l.cuenta()==d.inventario()).findFirst().orElseThrow().cargo());
        assertEquals(bd("1.00"),asiento.getValue().stream().filter(l->l.cuenta()==d.descuentos()).findFirst().orElseThrow().abono());
        assertEquals(bd("25.47"),asiento.getValue().stream().filter(l->l.cuenta()==d.caja()).findFirst().orElseThrow().abono());
    }

    @Test void chequeExigeNumeroAntesDeEscribir() {
        var cmd=new CompraCmd(1L,2L,LocalDate.now(),"C-2",null,FormaPago.Cheque," ",BigDecimal.ZERO,BigDecimal.ZERO,
                List.of(new LineaCompraCmd(4L,null,3L,BigDecimal.ONE,BigDecimal.TEN)));
        assertThrows(ReglaNegocioException.class,()->servicio.registrar(cmd));
        verify(compraDAO,never()).crear(any()); verify(existenciasService,never()).aumentar(any(),any(),any());
    }
}
