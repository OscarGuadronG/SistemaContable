package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.config.ConfigPruebas;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.AjusteCmd;
import com.sistemacontable.control.service.cmd.CompraCmd;
import com.sistemacontable.control.service.cmd.LineaCompraCmd;
import com.sistemacontable.control.service.cmd.LineaTrasladoCmd;
import com.sistemacontable.control.service.cmd.LineaVentaCmd;
import com.sistemacontable.control.service.cmd.TrasladoCmd;
import com.sistemacontable.control.service.cmd.VentaCmd;
import com.sistemacontable.entity.Compra;
import com.sistemacontable.entity.CompraDetalle;
import com.sistemacontable.entity.Existencias;
import com.sistemacontable.entity.Kardex;
import com.sistemacontable.entity.KardexDetalle;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.PartidaDetalle;
import com.sistemacontable.entity.Venta;
import com.sistemacontable.entity.VentaDetalle;
import com.sistemacontable.entity.enums.TipoMovimiento;
import com.sistemacontable.entity.enums.TipoPartida;
import com.sistemacontable.entity.enums.FormaPago;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Compra, venta, traslado y ajuste con servicios y DAOs reales sobre H2 (EclipseLink) y transacciones reales
 * RESOURCE_LOCAL. Cada operacion corre en su propia transaccion que se confirma si termina bien y se REVIERTE ante
 * cualquier excepcion, igual que haria el contenedor JTA con {@code @ApplicationException(rollback=true)}. Asi se
 * comprueba, leyendo con un EntityManager nuevo, que un fallo no deja kardex, existencias ni partidas a medias.
 *
 * <p>Esta clase confirma datos en la H2 compartida de la JVM; {@link Bd#vaciar} los borra despues de cada prueba.</p>
 */
class InventarioH2Test {

    private EntityManagerFactory emf;
    private Long productoId;
    private Long proveedorId;
    private Long empleadoId;
    private Long clienteId;
    private Long loteA;
    private Long loteB;
    private Long almacenA;
    private Long almacenB;
    private Long almacenCerrado;

    @BeforeEach
    void sembrar() {
        emf = JpaTestSupport.emf();
        Bd.vaciar(emf);
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            EscenarioContable esc = new EscenarioContable(em);
            em.getTransaction().commit();
            productoId = esc.producto.getId();
            proveedorId = esc.proveedor.getId();
            empleadoId = esc.empleado.getId();
            clienteId = esc.cliente.getId();
            loteA = esc.loteA.getId();
            loteB = esc.loteB.getId();
            almacenA = esc.almacenA.getId();
            almacenB = esc.almacenB.getId();
            almacenCerrado = esc.almacenCerrado.getId();
        } finally {
            em.close();
        }
    }

    @AfterEach
    void limpiar() {
        Bd.vaciar(emf);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** Ejecuta {@code accion} en una transaccion: commit si termina bien, rollback si lanza cualquier excepcion. */
    private <R> R transaccion(Map<String, String> config, Function<Cableado, R> accion) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            R resultado = accion.apply(new Cableado(em, ConfigPruebas.con(config)));
            em.getTransaction().commit();
            return resultado;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private <R> R transaccion(Function<Cableado, R> accion) {
        return transaccion(Map.of(), accion);
    }

    private <R> R leer(Function<EntityManager, R> consulta) {
        EntityManager em = emf.createEntityManager();
        try {
            return consulta.apply(em);
        } finally {
            em.close();
        }
    }

    private void comprar(Long lote, Long almacen, String cantidad, String precio) {
        transaccion(c -> c.obtener(CompraService.class).registrar(compra(lote,almacen,cantidad,precio,FormaPago.Credito)));

    }

    /** Fotografia del estado: filas por tabla relevante y stock de los dos lotes en el almacen A. */
    private List<Object> estado() {
        return leer(em -> List.of(Bd.contar(em, Compra.class), Bd.contar(em, CompraDetalle.class),
                Bd.contar(em, Venta.class), Bd.contar(em, VentaDetalle.class), Bd.contar(em, Kardex.class),
                Bd.contar(em, KardexDetalle.class), Bd.contar(em, Partida.class), Bd.contar(em, PartidaDetalle.class),
                Bd.contar(em, Existencias.class), Bd.stock(em, loteA, almacenA).stripTrailingZeros(),
                Bd.stock(em, loteB, almacenA).stripTrailingZeros(), Bd.stock(em, loteA, almacenB).stripTrailingZeros()));
    }

    private static BigDecimal suma(List<PartidaDetalle> lineas, boolean cargos) {
        return lineas.stream().map(l -> cargos ? l.getCargo() : l.getAbono()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** La linea de la cuenta con ese codigo (el orden de insercion de filas hermanas no esta garantizado). */
    private static PartidaDetalle lineaDe(List<PartidaDetalle> lineas, int codigoCuenta) {
        return lineas.stream().filter(l -> l.getCuenta().getCodigo() == codigoCuenta).findFirst().orElseThrow();
    }

    private List<PartidaDetalle> lineasDe(EntityManager em, Partida p) {
        return em.createQuery("SELECT d FROM PartidaDetalle d WHERE d.partida = :p ORDER BY d.id", PartidaDetalle.class)
                .setParameter("p", p).getResultList();
    }

    private CompraCmd compra(Long lote,Long almacen,String cantidad,String precio,FormaPago forma) {
        return new CompraCmd(proveedorId,empleadoId,null,"CCF",null,forma,forma==FormaPago.Cheque ? "001" : null,
                BigDecimal.ZERO,BigDecimal.ZERO,List.of(new LineaCompraCmd(lote,null,almacen,bd(cantidad),bd(precio))));
    }
    private VentaCmd venta(String cantidad,FormaPago forma) {
        return new VentaCmd(clienteId,empleadoId,almacenA,null,"FAC",null,forma,forma==FormaPago.Cheque ? "002" : null,
                BigDecimal.ZERO,List.of(new LineaVentaCmd(productoId,bd(cantidad))));
    }
    private void cuadrada(Partida p) {
        leer(em -> {var lineas=lineasDe(em,em.find(Partida.class,p.getId()));
            assertEquals(0,suma(lineas,true).compareTo(suma(lineas,false)));return null;});
    }
    @Test void compraAgrupaKardexPorAlmacenYActualizaStock() {
        var r=transaccion(c->c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId,empleadoId,null,"CCF",null,FormaPago.Credito,null,
                BigDecimal.ZERO,BigDecimal.ZERO,List.of(new LineaCompraCmd(loteA,null,almacenA,bd("10"),bd("5")),new LineaCompraCmd(loteB,null,almacenA,bd("4"),bd("25.50"))))));
        assertEquals(bd("171.76"),r.total());assertEquals(1,r.movimientos().size());
        leer(em->{assertEquals(0,bd("10").compareTo(Bd.stock(em,loteA,almacenA)));assertEquals(0,bd("4").compareTo(Bd.stock(em,loteB,almacenA)));return null;});
        cuadrada(r.partida());
    }
    @Test void compraAcumulaLineasRepetidasEnUnaExistencia() {
        transaccion(c->c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId,empleadoId,null,null,null,FormaPago.Contado,null,null,null,
                List.of(new LineaCompraCmd(loteA,null,almacenA,bd("3"),bd("2")),new LineaCompraCmd(loteA,null,almacenA,bd("4"),bd("2"))))));
        leer(em->{assertEquals(0,bd("7").compareTo(Bd.stock(em,loteA,almacenA)));assertEquals(1,Bd.contar(em,Existencias.class));return null;});
    }
    @Test void compraEnAlmacenCerradoNoDejaDatos() {
        var antes=estado();assertThrows(ReglaNegocioException.class,()->transaccion(c->c.obtener(CompraService.class).registrar(compra(loteA,almacenCerrado,"1","1",FormaPago.Contado))));assertEquals(antes,estado());
    }
    @Test void comprasContadoChequeCreditoUsanCuentaYTipoCorrectos() {
        for(FormaPago forma:FormaPago.values()) {
            var r=transaccion(c->c.obtener(CompraService.class).registrar(compra(loteA,almacenA,"2","10",forma)));
            leer(em->{Partida p=em.find(Partida.class,r.partida().getId());assertEquals(forma==FormaPago.Credito?TipoPartida.Diario:TipoPartida.Egreso,p.getTipo());
                int codigo=forma==FormaPago.Credito?2101:forma==FormaPago.Cheque?11031:1101;
                assertEquals(0,bd("22.60").compareTo(lineaDe(lineasDe(em,p),codigo).getAbono()));return null;});cuadrada(r.partida());
        }
    }
    @Test void cuentaDePagoInexistenteRechazaSinCambios() {
        var antes=estado();assertThrows(ReglaNegocioException.class,()->transaccion(Map.of("cuenta.proveedores","9999"),c->c.obtener(CompraService.class).registrar(compra(loteA,almacenA,"1","1",FormaPago.Credito))));assertEquals(antes,estado());
        assertNotNull(transaccion(Map.of("cuenta.proveedores","9999"),c->c.obtener(CompraService.class).registrar(compra(loteA,almacenA,"1","1",FormaPago.Contado))));
    }
    @Test void falloDurantePartidaRevierteCompraYStock() {
        var antes=estado();assertThrows(IllegalStateException.class,()->transaccion(c->{fallarPartida(c);return c.obtener(CompraService.class).registrar(compra(loteA,almacenA,"5","2",FormaPago.Credito));}));assertEquals(antes,estado());
    }
    private static void fallarPartida(Cableado c) {
        c.reemplazar(PartidaDetalleDAO.class,new PartidaDetalleDAO(){@Override public PartidaDetalle crear(PartidaDetalle d){throw new IllegalStateException("Fallo simulado escribiendo partida");}});
    }
    @Test void ventaUsaMargenYPromedioDeEntradasEnEseAlmacen() {
        comprar(loteA,almacenA,"10","5");comprar(loteA,almacenA,"10","7");
        var r=transaccion(c->c.obtener(VentaService.class).registrar(venta("4",FormaPago.Contado)));
        assertEquals(bd("31.20"),r.subtotal());assertEquals(bd("4.06"),r.iva());assertEquals(bd("35.26"),r.total());assertEquals(bd("24.00"),r.costoVentas());
        leer(em->{assertEquals(0,bd("16").compareTo(Bd.stock(em,loteA,almacenA)));return null;});cuadrada(r.partida());
    }
    @Test void fifoConsumePrimeraEntradaLuegoSegundoLote() {
        // Fechas distintas: si coinciden, el contrato desempata por id de lote, no por orden de persist().
        transaccion(c->c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId,empleadoId,LocalDate.now().minusDays(1),null,null,FormaPago.Credito,null,null,null,List.of(new LineaCompraCmd(loteA,null,almacenA,bd("3"),bd("5"))))));
        comprar(loteB,almacenA,"4","10");
        var r=transaccion(c->c.obtener(VentaService.class).registrar(venta("5",FormaPago.Credito)));
        assertEquals(2,r.detalles().size());assertEquals(bd("35.00"),r.costoVentas());
        leer(em->{assertEquals(0,BigDecimal.ZERO.compareTo(Bd.stock(em,loteA,almacenA)));assertEquals(0,bd("2").compareTo(Bd.stock(em,loteB,almacenA)));assertEquals(TipoPartida.Ingreso,em.find(Partida.class,r.partida().getId()).getTipo());return null;});cuadrada(r.partida());
    }
    @Test void ventaSinStockSuficienteAcumuladoNoDejaCambios() {
        comprar(loteA,almacenA,"10","5");var antes=estado();
        assertThrows(ReglaNegocioException.class,()->transaccion(c->c.obtener(VentaService.class).registrar(new VentaCmd(clienteId,empleadoId,almacenA,null,null,null,FormaPago.Contado,null,null,List.of(new LineaVentaCmd(productoId,bd("6")),new LineaVentaCmd(productoId,bd("6")))))));assertEquals(antes,estado());
    }
    @Test void falloDurantePartidaRevierteVentaYStock() {
        comprar(loteA,almacenA,"10","5");var antes=estado();
        assertThrows(IllegalStateException.class,()->transaccion(c->{fallarPartida(c);return c.obtener(VentaService.class).registrar(venta("4",FormaPago.Contado));}));assertEquals(antes,estado());
    }
    @Test void ventaConCuentaFaltanteSeRechazaSinCambios() {
        comprar(loteA,almacenA,"10","5");var antes=estado();
        assertThrows(ReglaNegocioException.class,()->transaccion(Map.of("cuenta.ventas","9999"),c->c.obtener(VentaService.class).registrar(venta("1",FormaPago.Contado))));assertEquals(antes,estado());
    }
    @Test void trasladoSinGastosMueveStockSinPartida() {
        comprar(loteA,almacenA,"10","5");long antes=leer(em->Bd.contar(em,Partida.class));
        var m=transaccion(c->c.obtener(TrasladoService.class).registrar(new TrasladoCmd(almacenA,almacenB,null,"Traslado",java.math.BigDecimal.ZERO,com.sistemacontable.entity.enums.FormaPago.Contado,null,List.of(new LineaTrasladoCmd(loteA,bd("4"))))));
        leer(em->{assertEquals(0,bd("6").compareTo(Bd.stock(em,loteA,almacenA)));assertEquals(0,bd("4").compareTo(Bd.stock(em,loteA,almacenB)));assertEquals(antes,Bd.contar(em,Partida.class));assertEquals(TipoMovimiento.Traslado,em.find(Kardex.class,m.movimiento().kardex().getId()).getTipoMovimiento());return null;});
    }
    @Test void trasladoInvalidoNoDejaCambios() {
        comprar(loteA,almacenA,"3","5");var antes=estado();
        assertThrows(ReglaNegocioException.class,()->transaccion(c->c.obtener(TrasladoService.class).registrar(new TrasladoCmd(almacenA,almacenB,null,null,java.math.BigDecimal.ZERO,com.sistemacontable.entity.enums.FormaPago.Contado,null,List.of(new LineaTrasladoCmd(loteA,bd("4")))))));
        assertThrows(ReglaNegocioException.class,()->transaccion(c->c.obtener(TrasladoService.class).registrar(new TrasladoCmd(almacenA,almacenA,null,null,java.math.BigDecimal.ZERO,com.sistemacontable.entity.enums.FormaPago.Contado,null,List.of(new LineaTrasladoCmd(loteA,bd("1")))))));assertEquals(antes,estado());
    }
    @Test void ajustesGeneranPartidasConKardexYContrapartidaSegunSigno() {
        comprar(loteA,almacenA,"10","5");
        var e=transaccion(c->c.obtener(AjusteService.class).registrar(new AjusteCmd(almacenA,loteA,bd("2"),"Sobrante")));
        var s=transaccion(c->c.obtener(AjusteService.class).registrar(new AjusteCmd(almacenA,loteA,bd("-5"),"Merma")));
        leer(em->{assertEquals(0,bd("7").compareTo(Bd.stock(em,loteA,almacenA)));assertEquals(e.movimiento().kardex().getId(),em.find(Partida.class,e.partida().getId()).getIdKardex());
            assertEquals(0,bd("10").compareTo(lineaDe(lineasDe(em,em.find(Partida.class,e.partida().getId())),5203).getAbono()));
            assertEquals(0,bd("25").compareTo(lineaDe(lineasDe(em,em.find(Partida.class,s.partida().getId())),4109).getCargo()));return null;});cuadrada(e.partida());cuadrada(s.partida());
    }
    @Test void ajusteDeSalidaMayorAlStockNoCambiaDatos() {
        comprar(loteA,almacenA,"3","5");var antes=estado();assertThrows(ReglaNegocioException.class,()->transaccion(c->c.obtener(AjusteService.class).registrar(new AjusteCmd(almacenA,loteA,bd("-4"),"Merma"))));assertEquals(antes,estado());
    }
    @Test void trasladoConGastosDaCostoDistintoPorAlmacenYAlimentaInventarioPreciosYLibros() {
        comprar(loteA,almacenA,"10","5");
        var traslado=transaccion(c->c.obtener(TrasladoService.class).registrar(new TrasladoCmd(almacenA,almacenB,LocalDate.now(),"Transporte",bd("40"),FormaPago.Cheque,null,List.of(new LineaTrasladoCmd(loteA,bd("4"))))));
        assertEquals(bd("60.00"),traslado.costoTotal());cuadrada(traslado.partida());
        transaccion(c->{
            var inventario=c.obtener(InventarioService.class);
            var origen=inventario.buscar(productoId,almacenA);var destino=inventario.buscar(productoId,almacenB);
            assertEquals(0,bd("6").compareTo(origen.existenciaTotal()));assertEquals(0,bd("5").compareTo(origen.costoPromedio()));
            assertEquals(0,bd("4").compareTo(destino.existenciaTotal()));assertEquals(0,bd("15").compareTo(destino.costoPromedio()));
            assertEquals(2,destino.porAlmacen().size());
            assertEquals(0,bd("19.50").compareTo(inventario.precios(almacenB).getFirst().precioVenta()));
            assertEquals(1,inventario.listar("general",null,almacenB,"con",0,50).size());
            assertEquals(1,inventario.contar(null,null,almacenB,"con"));
            var libros=c.obtener(LibroService.class);
            var libro=libros.compras(LocalDate.now(),LocalDate.now());assertEquals(1,libro.filas().size());assertEquals(0,bd("56.50").compareTo(libro.totales().total()));
            assertTrue(libros.ventas(null,null).filas().isEmpty());
            var consulta=c.obtener(TrasladoService.class).listar(LocalDate.now(),LocalDate.now(),almacenB,0,50);
            assertEquals(1,consulta.size());assertNotNull(consulta.getFirst().partida());assertEquals(2,consulta.getFirst().partidaLineas().size());
            return null;
        });
        var antes=estado();
        assertThrows(ReglaNegocioException.class,()->transaccion(c->c.obtener(TrasladoService.class).registrar(new TrasladoCmd(almacenA,almacenB,LocalDate.now(),null,bd("1"),FormaPago.Credito,null,List.of(new LineaTrasladoCmd(loteA,bd("1")))))));
        assertEquals(antes,estado());
    }

}
