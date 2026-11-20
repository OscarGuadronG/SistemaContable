package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.enums.FormaPago;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import com.sistemacontable.control.service.cmd.LineaTrasladoCmd;
import com.sistemacontable.control.service.cmd.TrasladoCmd;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrasladoServiceTest {
    @Mock AlmacenDAO almacenDAO;
    @Mock LoteDAO loteDAO;
    @Mock ExistenciasService existencias;
    @Mock KardexService kardex;
    @Mock PartidaService partidas;
    @Mock PartidaDAO partidaDAO;
    @Mock PartidaDetalleDAO partidaDetalleDAO;
    @InjectMocks TrasladoService service;
    private static BigDecimal bd(String n) { return new BigDecimal(n); }

    @Test void reparteGastosSobreCostoDeOrigenYRegistraPartidaVinculada() {
        Almacen origen = Fabrica.almacen(20, true), destino = Fabrica.almacen(21, true);
        Lote a = Fabrica.lote(10), b = Fabrica.lote(11);
        when(almacenDAO.buscarPorId(20L)).thenReturn(Optional.of(origen));
        when(almacenDAO.buscarPorId(21L)).thenReturn(Optional.of(destino));
        when(loteDAO.buscarPorId(10L)).thenReturn(Optional.of(a));
        when(loteDAO.buscarPorId(11L)).thenReturn(Optional.of(b));
        when(existencias.costoEntradas(10L,20L)).thenReturn(Optional.of(bd("10.0000")));
        when(existencias.costoEntradas(11L,20L)).thenReturn(Optional.of(bd("20.0000")));
        when(kardex.registrar(any(), any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Kardex k = new Kardex(); k.setId(777L); k.setTipoMovimiento(TipoMovimiento.Traslado); k.setOrigen(origen); k.setDestino(destino);
            return new MovimientoKardex(k, List.of());
        });
        Cuenta inventario = new Cuenta(), bancos = new Cuenta(); inventario.setId(1L); bancos.setId(2L);
        when(partidas.cuentaConfigurada("cuenta.inventario")).thenReturn(inventario);
        when(partidas.cuentaConfigurada("cuenta.bancos")).thenReturn(bancos);
        Partida p = new Partida(); p.setId(900L);
        when(partidas.registrar(eq(TipoPartida.Diario), any(), anyString(), isNull(), isNull(), isNull(), anyList())).thenReturn(p);
        when(partidaDetalleDAO.buscarPorPartida(900L)).thenReturn(List.of());
        var cmd = new TrasladoCmd(20L,21L,LocalDate.of(2026,11,20),"Flete",bd("3.00"),FormaPago.Cheque,null,
                List.of(new LineaTrasladoCmd(10L,bd("1")),new LineaTrasladoCmd(11L,bd("1"))));

        ResultadoTraslado resultado = service.registrar(cmd);

        ArgumentCaptor<List<DetalleKardex>> rows = ArgumentCaptor.forClass(List.class);
        verify(kardex).registrar(eq(TipoMovimiento.Traslado),eq(origen),eq(destino),isNull(),isNull(),eq("Flete"),any(),rows.capture());
        assertEquals(bd("11.0000"),rows.getValue().get(0).precioCosto());
        assertEquals(bd("22.0000"),rows.getValue().get(1).precioCosto());
        assertEquals(bd("3.00"),resultado.gastosAdicionales());
        assertSame(p,resultado.partida());
        assertEquals(777L,p.getIdKardex());
        verify(partidas).registrar(eq(TipoPartida.Diario),eq(LocalDate.of(2026,11,20)),eq("Flete"),isNull(),isNull(),isNull(),argThat(lines -> lines.size()==2 && lines.get(0).cargo().compareTo(bd("3.00"))==0 && lines.get(1).abono().compareTo(bd("3.00"))==0));
    }
}
