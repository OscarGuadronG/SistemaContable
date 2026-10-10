package com.sistemacontable.boundary.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.boundary.dto.AlmacenDto;
import com.sistemacontable.boundary.dto.BalanzaDto;
import com.sistemacontable.boundary.dto.CompraDto;
import com.sistemacontable.boundary.dto.CuentaDto;
import com.sistemacontable.boundary.dto.EmpleadoDto;
import com.sistemacontable.boundary.dto.ExistenciasDto;
import com.sistemacontable.boundary.dto.KardexDto;
import com.sistemacontable.boundary.dto.LoteDto;
import com.sistemacontable.boundary.dto.PagoPlanillaDto;
import com.sistemacontable.boundary.dto.PartidaDto;
import com.sistemacontable.boundary.dto.PlanillaDetalleDto;
import com.sistemacontable.boundary.dto.ResultadoAjusteDto;
import com.sistemacontable.boundary.dto.ResultadoCompraDto;
import com.sistemacontable.boundary.dto.ResultadoVentaDto;
import com.sistemacontable.boundary.dto.ResumenPlanillaDto;
import com.sistemacontable.boundary.dto.TipoProductoDto;
import com.sistemacontable.boundary.dto.VentaDto;
import com.sistemacontable.control.service.Balanza;
import com.sistemacontable.control.service.FilaBalanza;
import com.sistemacontable.control.service.LineaPlanilla;
import com.sistemacontable.control.service.MovimientoKardex;
import com.sistemacontable.control.service.PagoPlanilla;
import com.sistemacontable.control.service.ResultadoAjuste;
import com.sistemacontable.control.service.ResultadoCompra;
import com.sistemacontable.control.service.ResultadoPlanilla;
import com.sistemacontable.control.service.ResultadoVenta;
import com.sistemacontable.control.service.ResumenPlanilla;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Departamento;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Existencias;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Planilla;
import com.sistemacontable.entity.PlanillaDetalle;
import com.sistemacontable.entity.Producto;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.Tercero;
import com.sistemacontable.entity.TipoProducto;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import com.sistemacontable.entity.enums.EstadoPlanilla;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Mapeo entidad -&gt; DTO: puro (sin servicios ni consultas). Las relaciones solo se leen por su id: todas las
 * entidades relacionadas de estas pruebas tienen unicamente el id cargado, igual que una referencia perezosa.
 */
class MapeadoresTest {

    private static <T extends com.sistemacontable.entity.EntidadBase> T conId(T entidad, long id) {
        entidad.setId(id);
        return entidad;
    }

    private static Lote lote(long id) {
        return conId(new Lote(), id);
    }

    private static Almacen almacen(long id) {
        return conId(new Almacen(), id);
    }

    // ------------------------------------------------------------------------------------ catalogos

    @Test
    void mapeaUnEmpleadoConSoloElIdDelDepartamento() {
        Empleado e = conId(new Empleado(), 4L);
        e.setNombre("Ana");
        e.setApellido("Lopez");
        e.setFechNac(LocalDate.of(1990, 5, 17));
        e.setDepartamento(conId(new Departamento(), 2L));
        e.setSalario(new BigDecimal("650.50"));
        e.setEstado(EstadoEmpleado.Suspendido);
        e.setNumeroIsss("123456789");
        e.setDui("00000000-0");

        EmpleadoDto dto = CatalogoMapper.empleado(e);

        assertEquals(new EmpleadoDto(4L, "Ana", "Lopez", LocalDate.of(1990, 5, 17), 2L, new BigDecimal("650.50"),
                EstadoEmpleado.Suspendido, "123456789", "00000000-0"), dto);
    }

    @Test
    void mapeaUnaCuentaConYSinPadre() {
        Cuenta raiz = conId(new Cuenta(), 1L);
        raiz.setCodigo(1);
        raiz.setNombre("Activo");
        raiz.setNaturaleza(NaturalezaCuenta.Deudora);
        Cuenta hija = conId(new Cuenta(), 2L);
        hija.setCodigo(11);
        hija.setNombre("Caja");
        hija.setNaturaleza(NaturalezaCuenta.Deudora);
        hija.setImputable(true);
        hija.setPadre(raiz);

        CuentaDto dtoRaiz = CatalogoMapper.cuenta(raiz);
        CuentaDto dtoHija = CatalogoMapper.cuenta(hija);

        assertNull(dtoRaiz.padreId());
        assertEquals(false, dtoRaiz.imputable());
        assertEquals(true, dtoRaiz.activo());
        assertEquals(1L, dtoHija.padreId());
        assertEquals(11, dtoHija.codigo());
        assertEquals(true, dtoHija.imputable());
    }

    @Test
    void mapeaTipoDeProductoYProductoPorIds() {
        TipoProducto raiz = conId(new TipoProducto(), 1L);
        raiz.setNombre("Bebidas");
        TipoProducto hijo = conId(new TipoProducto(), 2L);
        hijo.setNombre("Gaseosas");
        hijo.setPadre(raiz);
        Producto p = conId(new Producto(), 9L);
        p.setNombre("Cola");
        p.setTipo(hijo);
        p.setUnidadMedida(conId(new com.sistemacontable.entity.UnidadMedida(), 3L));

        assertEquals(new TipoProductoDto(1L, "Bebidas", null), CatalogoMapper.tipoProducto(raiz));
        assertEquals(1L, CatalogoMapper.tipoProducto(hijo).padreId());
        assertEquals(2L, CatalogoMapper.producto(p).tipoId());
        assertEquals(3L, CatalogoMapper.producto(p).unidadMedidaId());
    }

    @Test
    void mapeaLoteAlmacenYExistencias() {
        Lote l = conId(new Lote(), 5L);
        l.setProducto(conId(new Producto(), 8L));
        l.setNumLote("L-2026-01");
        l.setFechCaducidad(LocalDate.of(2027, 1, 31));
        Almacen a = conId(new Almacen(), 6L);
        a.setNombre("Central");
        a.setEstado(true);
        Existencias ex = conId(new Existencias(), 7L);
        ex.setLote(l);
        ex.setAlmacen(a);
        ex.setCantidad(new BigDecimal("12.5000"));

        assertEquals(new LoteDto(5L, 8L, "L-2026-01", LocalDate.of(2027, 1, 31)), CatalogoMapper.lote(l));
        assertEquals(new AlmacenDto(6L, "Central", null, true), CatalogoMapper.almacen(a));
        assertEquals(new ExistenciasDto(7L, 5L, 6L, new BigDecimal("12.5000")), InventarioMapper.existencias(ex));
    }

    @Test
    void unaEntidadNulaSeMapeaANulo() {
        assertNull(CatalogoMapper.departamento(null));
        assertNull(CatalogoMapper.empleado(null));
        assertNull(InventarioMapper.kardex(null, List.of()));
        assertNull(ContabilidadMapper.partida(null, List.of()));
    }

    // ------------------------------------------------------------------------------ compra y venta

    @Test
    void mapeaUnaCompraConSusLineas() {
        Compra c = conId(new Compra(), 10L);
        c.setFecha(LocalDate.of(2026, 3, 1));
        c.setProveedor(conId(new Proveedor(), 3L));
        c.setEmpleado(conId(new Empleado(), 4L));
        c.setDescripcion("Reposicion");
        CompraDetalle d = conId(new CompraDetalle(), 100L);
        d.setCompra(c);
        d.setLote(lote(5L));
        d.setAlmacen(almacen(6L));
        d.setCantidad(new BigDecimal("2"));
        d.setPrecio(new BigDecimal("10.25"));

        CompraDto dto = InventarioMapper.compra(c, List.of(d));

        assertEquals(10L, dto.id());
        assertEquals(LocalDate.of(2026, 3, 1), dto.fecha());
        assertEquals(3L, dto.proveedorId());
        assertEquals(4L, dto.empleadoId());
        assertEquals(List.of(new CompraDto.Linea(100L, 5L, 6L, new BigDecimal("2"), new BigDecimal("10.25"))),
                dto.lineas());
    }

    @Test
    void sinLineasLaCompraLasOmite() {
        Compra c = conId(new Compra(), 10L);
        c.setProveedor(conId(new Proveedor(), 3L));
        c.setEmpleado(conId(new Empleado(), 4L));

        assertNull(InventarioMapper.compra(c, null).lineas());
        assertTrue(InventarioMapper.compra(c, List.of()).lineas().isEmpty());
    }

    @Test
    void mapeaUnaVentaConSusLineas() {
        Venta v = conId(new Venta(), 11L);
        v.setFecha(LocalDate.of(2026, 3, 2));
        v.setCliente(conId(new com.sistemacontable.entity.Cliente(), 7L));
        v.setEmpleado(conId(new Empleado(), 4L));
        VentaDetalle d = conId(new VentaDetalle(), 101L);
        d.setLote(lote(5L));
        d.setAlmacen(almacen(6L));
        d.setCantidad(BigDecimal.ONE);
        d.setPrecio(new BigDecimal("30"));

        VentaDto dto = InventarioMapper.venta(v, List.of(d));

        assertEquals(7L, dto.clienteId());
        assertEquals(List.of(new VentaDto.Linea(101L, 5L, 6L, BigDecimal.ONE, new BigDecimal("30"))), dto.lineas());
    }

    // ------------------------------------------------------------------------------------- kardex

    private static Kardex kardex(long id, TipoMovimiento tipo, Almacen origen, Almacen destino) {
        Kardex k = conId(new Kardex(), id);
        k.setTipoMovimiento(tipo);
        k.setOrigen(origen);
        k.setDestino(destino);
        k.setFecha(OffsetDateTime.of(2026, 3, 3, 10, 0, 0, 0, ZoneOffset.UTC));
        return k;
    }

    private static KardexDetalle lineaKardex(long id, Kardex k, long loteId, String cantidad, String costo) {
        KardexDetalle d = conId(new KardexDetalle(), id);
        d.setKardex(k);
        d.setLote(lote(loteId));
        d.setCantidad(new BigDecimal(cantidad));
        d.setPrecioCosto(new BigDecimal(costo));
        return d;
    }

    @Test
    void mapeaUnTrasladoConOrigenYDestino() {
        Kardex k = kardex(20L, TipoMovimiento.Traslado, almacen(1L), almacen(2L));

        KardexDto dto = InventarioMapper.kardex(k, List.of(lineaKardex(200L, k, 5L, "3", "4.5000")));

        assertEquals(TipoMovimiento.Traslado, dto.tipoMovimiento());
        assertEquals(1L, dto.origenId());
        assertEquals(2L, dto.destinoId());
        assertNull(dto.compraId());
        assertNull(dto.ventaId());
        assertEquals(List.of(new KardexDto.Linea(200L, 5L, new BigDecimal("3"), new BigDecimal("4.5000"))),
                dto.lineas());
    }

    @Test
    void unaEntradaDeCompraLlevaElIdDeLaCompraYNoTieneOrigen() {
        Kardex k = kardex(21L, TipoMovimiento.Entrada, null, almacen(2L));
        k.setCompra(conId(new Compra(), 10L));

        KardexDto dto = InventarioMapper.kardex(k, List.of());

        assertNull(dto.origenId());
        assertEquals(10L, dto.compraId());
    }

    // ------------------------------------------------------------------------------------ resultados

    @Test
    void mapeaElResultadoDeUnaCompraConSuPartida() {
        Compra c = conId(new Compra(), 10L);
        c.setProveedor(conId(new Proveedor(), 3L));
        c.setEmpleado(conId(new Empleado(), 4L));
        Kardex k = kardex(20L, TipoMovimiento.Entrada, null, almacen(2L));
        KardexDetalle kd = lineaKardex(200L, k, 5L, "2", "10.25");
        Partida partida = conId(new Partida(), 50L);

        ResultadoCompraDto dto = InventarioMapper.resultado(new ResultadoCompra(c, List.of(),
                List.of(new MovimientoKardex(k, List.of(kd))), partida, new BigDecimal("20.50"),
                new BigDecimal("2.67"), new BigDecimal("23.17")));

        assertEquals(10L, dto.compra().id());
        assertEquals(1, dto.movimientos().size());
        assertEquals(200L, dto.movimientos().get(0).lineas().get(0).id());
        assertEquals(50L, dto.partidaId());
        assertEquals(new BigDecimal("23.17"), dto.total());
    }

    @Test
    void sinPartidaElIdDeLaPartidaEsNulo() {
        Compra c = conId(new Compra(), 10L);
        c.setProveedor(conId(new Proveedor(), 3L));
        c.setEmpleado(conId(new Empleado(), 4L));

        ResultadoCompraDto dto = InventarioMapper.resultado(new ResultadoCompra(c, List.of(), List.of(), null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        assertNull(dto.partidaId());
    }

    @Test
    void mapeaElResultadoDeUnaVentaConElCostoDeVentas() {
        Venta v = conId(new Venta(), 11L);
        v.setCliente(conId(new com.sistemacontable.entity.Cliente(), 7L));
        v.setEmpleado(conId(new Empleado(), 4L));

        ResultadoVentaDto dto = InventarioMapper.resultado(new ResultadoVenta(v, List.of(), List.of(),
                conId(new Partida(), 51L), new BigDecimal("100.00"), new BigDecimal("13.00"), new BigDecimal("113.00"),
                new BigDecimal("60.00")));

        assertEquals(11L, dto.venta().id());
        assertEquals(51L, dto.partidaId());
        assertEquals(new BigDecimal("60.00"), dto.costoVentas());
        assertEquals(new BigDecimal("113.00"), dto.total());
    }

    @Test
    void mapeaElResultadoDeUnAjusteConYSinPartida() {
        Kardex k = kardex(22L, TipoMovimiento.Ajuste, null, almacen(2L));
        MovimientoKardex mov = new MovimientoKardex(k, List.of(lineaKardex(201L, k, 5L, "1", "2")));

        ResultadoAjusteDto sin = InventarioMapper.resultado(new ResultadoAjuste(mov, null));
        ResultadoAjusteDto con = InventarioMapper.resultado(new ResultadoAjuste(mov, conId(new Partida(), 52L)));

        assertNull(sin.partidaId());
        assertEquals(52L, con.partidaId());
        assertEquals(22L, con.movimiento().id());
    }

    // --------------------------------------------------------------------------------- contabilidad

    @Test
    void mapeaUnaPartidaConSusLineasYSusReferencias() {
        Partida p = conId(new Partida(), 60L);
        p.setFecha(LocalDate.of(2026, 3, 4));
        p.setConcepto("Compra de mercaderia");
        p.setTipo(TipoPartida.Diario);
        p.setCompra(conId(new Compra(), 10L));
        p.setTercero(conId(new Tercero(), 2L));
        PartidaDetalle cargo = conId(new PartidaDetalle(), 600L);
        cargo.setCuenta(conId(new Cuenta(), 1L));
        cargo.setCargo(new BigDecimal("100.00"));
        PartidaDetalle abono = conId(new PartidaDetalle(), 601L);
        abono.setCuenta(conId(new Cuenta(), 2L));
        abono.setAbono(new BigDecimal("100.00"));

        PartidaDto dto = ContabilidadMapper.partida(p, List.of(cargo, abono));

        assertEquals(TipoPartida.Diario, dto.tipo());
        assertEquals(10L, dto.compraId());
        assertNull(dto.ventaId());
        assertEquals(2L, dto.terceroId());
        assertEquals(List.of(
                new PartidaDto.Linea(600L, 1L, new BigDecimal("100.00"), BigDecimal.ZERO),
                new PartidaDto.Linea(601L, 2L, BigDecimal.ZERO, new BigDecimal("100.00"))), dto.lineas());
    }

    @Test
    void mapeaLaBalanzaConSuCuadre() {
        Balanza b = new Balanza(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
                List.of(new FilaBalanza(1L, 11, "Caja", NaturalezaCuenta.Deudora, true, new BigDecimal("100.00"),
                        new BigDecimal("40.00"), new BigDecimal("60.00"))),
                new BigDecimal("100.00"), new BigDecimal("90.00"));

        BalanzaDto dto = ContabilidadMapper.balanza(b);

        assertEquals(LocalDate.of(2026, 1, 1), dto.desde());
        assertEquals(1, dto.filas().size());
        assertEquals(new BalanzaDto.Fila(1L, 11, "Caja", NaturalezaCuenta.Deudora, true, new BigDecimal("100.00"),
                new BigDecimal("40.00"), new BigDecimal("60.00")), dto.filas().get(0));
        assertEquals(false, dto.cuadra());
        assertEquals(new BigDecimal("10.00"), dto.diferencia());
    }

    // --------------------------------------------------------------------------------------- planilla

    @Test
    void mapeaPlanillaDetalleResumenYPago() {
        OffsetDateTime inicio = OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Planilla pl = conId(new Planilla(), 70L);
        pl.setFechaInicio(inicio);
        pl.setFechaFin(inicio.plusDays(14));
        pl.setFechaPago(inicio.plusDays(15));
        pl.setEstado(EstadoPlanilla.Pagado);
        PlanillaDetalle d = conId(new PlanillaDetalle(), 700L);
        d.setPlanilla(pl);
        d.setEmpleado(conId(new Empleado(), 4L));
        d.setDias((short) 30);
        d.setHorasxDiur((short) 2);
        d.setHorasxNoct((short) 1);
        d.setSueldoBase(new BigDecimal("600.00"));
        ResultadoPlanilla r = new ResultadoPlanilla(new BigDecimal("600.00"), new BigDecimal("2.5000"),
                new BigDecimal("6.25"), new BigDecimal("3.75"), new BigDecimal("610.00"), new BigDecimal("30.00"),
                new BigDecimal("40.00"), new BigDecimal("10.00"), new BigDecimal("80.00"), new BigDecimal("530.00"));
        ResumenPlanilla resumen = new ResumenPlanilla(pl, List.of(new LineaPlanilla(d, r)), new BigDecimal("610.00"),
                new BigDecimal("30.00"), new BigDecimal("40.00"), new BigDecimal("10.00"), new BigDecimal("80.00"),
                new BigDecimal("530.00"));

        PlanillaDetalleDto detalle = PlanillaMapper.detalle(d);
        ResumenPlanillaDto dtoResumen = PlanillaMapper.resumen(resumen);
        PagoPlanillaDto pago = PlanillaMapper.pago(new PagoPlanilla(pl, conId(new Partida(), 80L), resumen));

        assertEquals(new PlanillaDetalleDto(700L, 70L, 4L, (short) 30, (short) 2, (short) 1, new BigDecimal("600.00")),
                detalle);
        assertEquals(EstadoPlanilla.Pagado, dtoResumen.planilla().estado());
        assertEquals(1, dtoResumen.lineas().size());
        assertEquals(new BigDecimal("530.00"), dtoResumen.lineas().get(0).calculo().liquido());
        assertEquals(new BigDecimal("530.00"), dtoResumen.totalLiquido());
        assertEquals(80L, pago.partidaId());
        assertEquals(70L, pago.planilla().id());
        assertEquals(new BigDecimal("610.00"), pago.resumen().totalDevengado());
    }
}
