package com.sistemacontable.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.PartidaDetalleDAO.SumaCuenta;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cliente;
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
import com.sistemacontable.entity.TipoProducto;
import com.sistemacontable.entity.UnidadMedida;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ejecuta las consultas JPQL reales de los DAO con EclipseLink contra el esquema de docs/esquema-bd.sql
 * (H2 en memoria, todo con rollback). Comprueba que cada nombre de campo existe y que el resultado es el
 * esperado; complementa a las pruebas con Mockito, que solo verifican parametros.
 */
class ConsultasJpqlTest {

    private EntityManager em;

    private Departamento dep;
    private Empleado ana;
    private Empleado inactivo;
    private Proveedor proveedor;
    private Cliente cliente;
    private TipoProducto raiz;
    private TipoProducto hijo;
    private Producto producto;
    private Lote lote;
    private Almacen central;
    private Almacen secundario;
    private Almacen cerrado;
    private Cuenta activo;
    private Cuenta caja;
    private Cuenta ventas;

    @BeforeEach
    void abrir() {
        em = JpaTestSupport.nuevoEmConTransaccion();
        sembrar();
    }

    @AfterEach
    void cerrar() {
        JpaTestSupport.cerrarConRollback(em);
    }

    private <D> D dao(D dao) {
        JpaTestSupport.inyectarEm(dao, em);
        return dao;
    }

    private void sembrar() {
        dep = new Departamento();
        dep.setNombre("Ventas");
        em.persist(dep);
        ana = empleado("Ana", "Zamora", "111", "00000001-1", EstadoEmpleado.Activo);
        inactivo = empleado("Beto", "Alvarado", "222", "00000002-2", EstadoEmpleado.Inactivo);

        proveedor = new Proveedor();
        proveedor.setNombre("Prov");
        proveedor.setNit("0614-P");
        em.persist(proveedor);
        cliente = new Cliente();
        cliente.setNombre("Cli");
        cliente.setNit("0614-C");
        em.persist(cliente);

        UnidadMedida um = new UnidadMedida();
        um.setNombre("Unidad");
        um.setAbrev("u");
        em.persist(um);
        raiz = new TipoProducto();
        raiz.setNombre("General");
        em.persist(raiz);
        hijo = new TipoProducto();
        hijo.setNombre("Bebidas");
        hijo.setPadre(raiz);
        em.persist(hijo);
        producto = new Producto();
        producto.setNombre("Agua");
        producto.setUnidadMedida(um);
        producto.setTipo(hijo);
        em.persist(producto);
        lote = new Lote();
        lote.setProducto(producto);
        lote.setNumLote("L-1");
        em.persist(lote);

        central = almacen("Central", true);
        secundario = almacen("Secundario", true);
        cerrado = almacen("Cerrado", false);

        activo = cuenta(1, "Activo", NaturalezaCuenta.Deudora, false, null);
        caja = cuenta(11, "Caja", NaturalezaCuenta.Deudora, true, activo);
        ventas = cuenta(41, "Ventas", NaturalezaCuenta.Acreedora, true, null);
        em.flush();
    }

    private Empleado empleado(String nombre, String apellido, String isss, String dui, EstadoEmpleado estado) {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        e.setApellido(apellido);
        e.setDepartamento(dep);
        e.setNumeroIsss(isss);
        e.setDui(dui);
        e.setEstado(estado);
        em.persist(e);
        return e;
    }

    private Almacen almacen(String nombre, boolean estado) {
        Almacen a = new Almacen();
        a.setNombre(nombre);
        a.setEstado(estado);
        em.persist(a);
        return a;
    }

    private Cuenta cuenta(int codigo, String nombre, NaturalezaCuenta nat, boolean imputable, Cuenta padre) {
        Cuenta c = new Cuenta();
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setNaturaleza(nat);
        c.setImputable(imputable);
        c.setPadre(padre);
        em.persist(c);
        return c;
    }

    private Compra compra(BigDecimal cantidad, BigDecimal precio, Lote lote) {
        Compra c = new Compra();
        c.setProveedor(proveedor);
        c.setEmpleado(ana);
        em.persist(c);
        CompraDetalle d = new CompraDetalle();
        d.setCompra(c);
        d.setLote(lote);
        d.setAlmacen(central);
        d.setCantidad(cantidad);
        d.setPrecio(precio);
        em.persist(d);
        em.flush();
        return c;
    }

    // ---------------------------------------------------------------- AbstractDAO

    @Test
    void findRangeYCountFuncionanSobreElMapeoReal() {
        DepartamentoDAO dao = dao(new DepartamentoDAO());
        for (int i = 0; i < 5; i++) {
            Departamento d = new Departamento();
            d.setNombre("D" + i);
            dao.crear(d);
        }
        dao.flush();

        List<Departamento> pagina = dao.findRange(1, 3);

        assertEquals(3, pagina.size());
        assertTrue(pagina.get(0).getId() < pagina.get(1).getId() && pagina.get(1).getId() < pagina.get(2).getId());
        assertEquals(6L, dao.count()); // 5 + el departamento de la siembra
        assertEquals(1, dao.findRange(5, 5).size());
    }

    @Test
    void crearModificarBuscarYEliminarConElMotorReal() {
        DepartamentoDAO dao = dao(new DepartamentoDAO());
        Departamento d = new Departamento();
        d.setNombre("Nuevo");
        dao.crear(d);
        dao.flush();
        Long id = d.getId();
        assertTrue(dao.buscarPorId(id).isPresent());

        em.detach(d);
        d.setNombre("Cambiado");
        assertEquals("Cambiado", dao.modificar(d).getNombre());

        dao.eliminar(id);
        dao.flush();
        assertFalse(dao.buscarPorId(id).isPresent());
        assertThrows(NoEncontradoException.class, () -> dao.eliminar(id));
    }

    // ---------------------------------------------------------------- Cuenta

    @Test
    void cuentaFinders() {
        CuentaDAO dao = dao(new CuentaDAO());

        assertEquals("Caja", dao.findByCodigo(11).orElseThrow().getNombre());
        assertTrue(dao.findByCodigo(999).isEmpty());
        assertEquals(List.of(caja), dao.findHijos(activo));
        assertEquals(List.of(activo, caja, ventas), dao.findTodas());
        assertTrue(dao.findHijos(ventas).isEmpty());
        assertTrue(dao.existenHijosActivos(activo));
        assertFalse(dao.existenHijosActivos(ventas));

        caja.setActivo(false);
        em.flush();
        assertFalse(dao.existenHijosActivos(activo));

        assertFalse(dao.tieneMovimientos(caja));
        Partida p = partida(LocalDate.of(2026, 1, 15), TipoPartida.Diario);
        linea(p, caja, "100", "0");
        em.flush();
        assertTrue(dao.tieneMovimientos(caja));
    }

    // ---------------------------------------------------------------- Existencias, compras y ventas

    @Test
    void existenciasFindForUpdateYStockTotal() {
        ExistenciasDAO dao = dao(new ExistenciasDAO());
        assertTrue(dao.findForUpdate(lote.getId(), central.getId()).isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(dao.stockTotalPorLote(lote.getId())));

        existencia(central, "10.5");
        existencia(secundario, "4");
        em.flush();

        Existencias fila = dao.findForUpdate(lote.getId(), central.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("10.5").compareTo(fila.getCantidad()));
        assertEquals(0, new BigDecimal("14.5").compareTo(dao.stockTotalPorLote(lote.getId())));

        existencia(central, "1"); // duplicado: la BD no tiene UNIQUE(id_lote, id_almacen)
        em.flush();
        assertThrows(IllegalStateException.class, () -> dao.findForUpdate(lote.getId(), central.getId()));
    }

    private void existencia(Almacen almacen, String cantidad) {
        Existencias x = new Existencias();
        x.setLote(lote);
        x.setAlmacen(almacen);
        x.setCantidad(new BigDecimal(cantidad));
        em.persist(x);
    }

    @Test
    void compraDetalleTotalesYCostoPromedioPonderado() {
        CompraDetalleDAO dao = dao(new CompraDetalleDAO());
        assertEquals(0, BigDecimal.ZERO.compareTo(dao.totalCompra(0L)));
        assertTrue(dao.costoPromedioPonderado(lote.getId()).isEmpty());

        Compra c1 = compra(new BigDecimal("10"), new BigDecimal("2.00"), lote);
        compra(new BigDecimal("30"), new BigDecimal("4.00"), lote);

        assertEquals(1, dao.buscarPorCompra(c1.getId()).size());
        assertEquals(0, new BigDecimal("20.00").compareTo(dao.totalCompra(c1.getId())));
        // (10*2 + 30*4) / 40 = 3.5; el promedio simple de precios seria 3.0
        assertEquals(0, new BigDecimal("3.5").compareTo(dao.costoPromedioPonderado(lote.getId()).orElseThrow()));
    }

    @Test
    void ventaDetalleBuscarYTotal() {
        Venta v = new Venta();
        v.setCliente(cliente);
        v.setEmpleado(ana);
        em.persist(v);
        VentaDetalle d = new VentaDetalle();
        d.setVenta(v);
        d.setLote(lote);
        d.setAlmacen(central);
        d.setCantidad(new BigDecimal("3"));
        d.setPrecio(new BigDecimal("5.5"));
        em.persist(d);
        em.flush();
        VentaDetalleDAO dao = dao(new VentaDetalleDAO());

        assertEquals(List.of(d), dao.buscarPorVenta(v.getId()));
        assertEquals(0, new BigDecimal("16.5").compareTo(dao.totalVenta(v.getId())));
        assertEquals(0, BigDecimal.ZERO.compareTo(dao.totalVenta(0L)));
    }

    // ---------------------------------------------------------------- Partidas

    private Partida partida(LocalDate fecha, TipoPartida tipo) {
        Partida p = new Partida();
        p.setFecha(fecha);
        p.setConcepto("c");
        p.setTipo(tipo);
        em.persist(p);
        return p;
    }

    private PartidaDetalle linea(Partida p, Cuenta c, String cargo, String abono) {
        PartidaDetalle d = new PartidaDetalle();
        d.setPartida(p);
        d.setCuenta(c);
        d.setCargo(new BigDecimal(cargo));
        d.setAbono(new BigDecimal(abono));
        em.persist(d);
        return d;
    }

    @Test
    void partidaFindByRangoIncluyeExtremos() {
        Partida enero1 = partida(LocalDate.of(2026, 1, 1), TipoPartida.Diario);
        Partida enero31 = partida(LocalDate.of(2026, 1, 31), TipoPartida.Ingreso);
        partida(LocalDate.of(2026, 2, 1), TipoPartida.Egreso);
        em.flush();

        List<Partida> enero = dao(new PartidaDAO()).findByRango(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(List.of(enero1, enero31), enero);
    }

    @Test
    void partidaDetalleSumasPorCuentaYBuscarPorPartida() {
        Partida p1 = partida(LocalDate.of(2026, 1, 10), TipoPartida.Diario);
        linea(p1, caja, "100", "0");
        linea(p1, ventas, "0", "100");
        Partida p2 = partida(LocalDate.of(2026, 1, 20), TipoPartida.Diario);
        linea(p2, caja, "50", "0");
        linea(p2, ventas, "0", "50");
        Partida fuera = partida(LocalDate.of(2026, 3, 1), TipoPartida.Diario);
        linea(fuera, caja, "999", "0");
        linea(fuera, ventas, "0", "999");
        em.flush();
        PartidaDetalleDAO dao = dao(new PartidaDetalleDAO());

        List<SumaCuenta> sumas = dao.sumasPorCuenta(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertEquals(2, sumas.size());
        SumaCuenta deCaja = sumas.stream().filter(s -> s.cuentaId().equals(caja.getId())).findFirst().orElseThrow();
        SumaCuenta deVentas = sumas.stream().filter(s -> s.cuentaId().equals(ventas.getId())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("150").compareTo(deCaja.totalCargo()));
        assertEquals(0, BigDecimal.ZERO.compareTo(deCaja.totalAbono()));
        assertEquals(0, new BigDecimal("150").compareTo(deVentas.totalAbono()));
        assertEquals(2, dao.buscarPorPartida(p1.getId()).size());
        assertTrue(dao.sumasPorCuenta(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)).isEmpty());
    }

    // ---------------------------------------------------------------- Empleado, planilla

    @Test
    void empleadoFinders() {
        EmpleadoDAO dao = dao(new EmpleadoDAO());

        assertEquals(List.of(ana), dao.findActivos());
        assertEquals(inactivo, dao.findByDui("00000002-2").orElseThrow());
        assertEquals(ana, dao.findByNumeroIsss("111").orElseThrow());
        assertTrue(dao.findByDui("nada").isEmpty());
    }

    @Test
    void planillaDetalleFinders() {
        Planilla pl = new Planilla();
        OffsetDateTime ahora = OffsetDateTime.now();
        pl.setFechaInicio(ahora);
        pl.setFechaFin(ahora);
        pl.setFechaPago(ahora);
        em.persist(pl);
        PlanillaDetalle d = new PlanillaDetalle();
        d.setPlanilla(pl);
        d.setEmpleado(ana);
        d.setSueldoBase(new BigDecimal("500"));
        em.persist(d);
        em.flush();
        PlanillaDetalleDAO dao = dao(new PlanillaDetalleDAO());

        assertEquals(List.of(d), dao.buscarPorPlanilla(pl.getId()));
        assertTrue(dao.existePorPlanillaYEmpleado(pl.getId(), ana.getId()));
        assertFalse(dao.existePorPlanillaYEmpleado(pl.getId(), inactivo.getId()));
    }

    // ---------------------------------------------------------------- Catalogos y kardex

    @Test
    void tipoProductoRaicesEHijos() {
        TipoProductoDAO dao = dao(new TipoProductoDAO());

        assertEquals(List.of(raiz), dao.findRaices());
        assertEquals(List.of(hijo), dao.findHijos(raiz));
        assertTrue(dao.findHijos(hijo).isEmpty());
    }

    @Test
    void kardexFindByAlmacenIncluyeEntradasYSalidasConNulos() {
        Kardex entrada = kardex(TipoMovimiento.Entrada, null, central);
        Kardex salida = kardex(TipoMovimiento.Salida, central, null);
        Kardex traslado = kardex(TipoMovimiento.Traslado, central, secundario);
        kardex(TipoMovimiento.Entrada, null, cerrado);
        KardexDetalle kd = new KardexDetalle();
        kd.setKardex(entrada);
        kd.setLote(lote);
        kd.setCantidad(BigDecimal.ONE);
        kd.setPrecioCosto(BigDecimal.TEN);
        em.persist(kd);
        em.flush();

        List<Kardex> deCentral = dao(new KardexDAO()).findByAlmacen(central.getId());

        assertEquals(3, deCentral.size());
        assertTrue(deCentral.containsAll(List.of(entrada, salida, traslado)));
        assertEquals(List.of(traslado), dao(new KardexDAO()).findByAlmacen(secundario.getId()));
        assertEquals(List.of(kd), dao(new KardexDetalleDAO()).buscarPorKardex(entrada.getId()));
    }

    private Kardex kardex(TipoMovimiento tipo, Almacen origen, Almacen destino) {
        Kardex k = new Kardex();
        k.setTipoMovimiento(tipo);
        k.setOrigen(origen);
        k.setDestino(destino);
        em.persist(k);
        return k;
    }

    @Test
    void loteAlmacenYNitFinders() {
        assertEquals(List.of(lote), dao(new LoteDAO()).findByProducto(producto.getId()));
        assertEquals(List.of(central, secundario), dao(new AlmacenDAO()).findActivos());
        assertEquals(cliente, dao(new ClienteDAO()).findByNit("0614-C").orElseThrow());
        assertEquals(proveedor, dao(new ProveedorDAO()).findByNit("0614-P").orElseThrow());
        assertTrue(dao(new ClienteDAO()).findByNit("0614-P").isEmpty());
    }
}
