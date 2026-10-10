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
        transaccion(c -> c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId, empleadoId, null, "Compra",
                bd("0"),
                List.of(new LineaCompraCmd(lote, almacen, bd(cantidad), bd(precio))))));
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

    // ------------------------------------------------------------------------------------- compra

    @Test
    void laCompraActualizaExistenciasKardexYPartidaCuadrada() {
        ResultadoCompra r = transaccion(c -> c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId,
                empleadoId, LocalDate.of(2026, 5, 4), "Reposicion", bd("0"), List.of(
                        new LineaCompraCmd(loteA, almacenA, bd("10"), bd("5.00")),
                        new LineaCompraCmd(loteB, almacenA, bd("4"), bd("25.50"))))));

        assertEquals(bd("171.76"), r.total());
        leer(em -> {
            assertEquals(0, bd("10").compareTo(Bd.stock(em, loteA, almacenA)));
            assertEquals(0, bd("4").compareTo(Bd.stock(em, loteB, almacenA)));
            List<Kardex> kardex = em.createQuery("SELECT k FROM Kardex k ORDER BY k.id", Kardex.class).getResultList();
            assertEquals(2, kardex.size());
            for (Kardex k : kardex) {
                assertEquals(TipoMovimiento.Entrada, k.getTipoMovimiento());
                assertNull(k.getOrigen());
                assertEquals(almacenA, k.getDestino().getId());
                assertEquals(r.compra().getId(), k.getCompra().getId());
                assertNull(k.getVenta());
            }
            List<KardexDetalle> detalles = em.createQuery("SELECT d FROM KardexDetalle d ORDER BY d.id",
                    KardexDetalle.class).getResultList();
            // el orden de insercion de filas hermanas no esta garantizado: se comparan los costos ordenados
            assertEquals(List.of(bd("5"), bd("25.5")), detalles.stream()
                    .map(d -> d.getPrecioCosto().stripTrailingZeros()).sorted().toList());
            Partida p = em.find(Partida.class, r.partida().getId());
            assertEquals(TipoPartida.Diario, p.getTipo());
            assertEquals(r.compra().getId(), p.getCompra().getId());
            List<PartidaDetalle> lineas = lineasDe(em, p);
            assertEquals(3, lineas.size());
            assertEquals(0, bd("171.76").compareTo(suma(lineas, true)));
            assertEquals(0, suma(lineas, true).compareTo(suma(lineas, false)));
            return null;
        });
    }

    @Test
    void dosLineasDelMismoLoteYAlmacenAcumulanEnUnaSolaFilaDeExistencias() {
        transaccion(c -> c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId, empleadoId, null, null,
                bd("0"),
                List.of(new LineaCompraCmd(loteA, almacenA, bd("3"), bd("2")),
                        new LineaCompraCmd(loteA, almacenA, bd("4"), bd("2"))))));

        leer(em -> {
            assertEquals(0, bd("7").compareTo(Bd.stock(em, loteA, almacenA)));
            assertEquals(1, Bd.contar(em, Existencias.class));
            return null;
        });
    }

    @Test
    void unaCompraEnUnAlmacenDeshabilitadoNoDejaNada() {
        List<Object> antes = estado();

        assertThrows(ReglaNegocioException.class, () -> transaccion(c -> c.obtener(CompraService.class).registrar(
                new CompraCmd(proveedorId, empleadoId, null, null,
                        bd("0"),
                        List.of(new LineaCompraCmd(loteA, almacenA, bd("1"), bd("1")),
                                new LineaCompraCmd(loteB, almacenCerrado, bd("1"), bd("1")))))));

        assertEquals(antes, estado());
    }

    @Test
    void unaCuentaConfiguradaInexistenteImpideLaCompraSinDejarNada() {
        List<Object> antes = estado();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> transaccion(Map.of("cuenta.proveedores", "9999"), c -> c.obtener(CompraService.class)
                        .registrar(new CompraCmd(proveedorId, empleadoId, null, null,
                                bd("0"),
                                List.of(new LineaCompraCmd(loteA, almacenA, bd("1"), bd("1")))))));

        assertTrue(ex.getMessage().contains("cuenta.proveedores"), ex.getMessage());
        assertEquals(antes, estado());
    }

    @Test
    void unFalloAMitadDeLaEscrituraDeUnaCompraRevierteTodo() {
        List<Object> antes = estado();

        assertThrows(IllegalStateException.class, () -> transaccion(c -> {
            c.reemplazar(PartidaDetalleDAO.class, new PartidaDetalleDAO() {
                @Override
                public PartidaDetalle crear(PartidaDetalle entidad) {
                    throw new IllegalStateException("fallo simulado al escribir la partida");
                }
            });
            return c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId, empleadoId, null, null,
                    bd("0"),
                    List.of(new LineaCompraCmd(loteA, almacenA, bd("5"), bd("2")))));
        }));

        assertEquals(antes, estado());
    }

    // ---------------------------------------------------------------------- compra: contado / credito

    private ResultadoCompra comprarConPorcentaje(Map<String, String> config, String porcentajeContado) {
        return transaccion(config, c -> c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId, empleadoId,
                LocalDate.of(2026, 5, 4), "Reposicion", bd(porcentajeContado), List.of(
                        new LineaCompraCmd(loteA, almacenA, bd("10"), bd("5.00")),
                        new LineaCompraCmd(loteB, almacenA, bd("4"), bd("25.50"))))));
    }

    @Test
    void laCompraMixta40_60AcreditaCajaYProveedoresYLaPartidaCuadra() {
        ResultadoCompra r = comprarConPorcentaje(Map.of(), "40");

        leer(em -> {
            List<PartidaDetalle> lineas = lineasDe(em, em.find(Partida.class, r.partida().getId()));
            assertEquals(4, lineas.size());
            assertEquals(0, bd("152.00").compareTo(lineaDe(lineas, 1104).getCargo()));
            assertEquals(0, bd("19.76").compareTo(lineaDe(lineas, 1105).getCargo()));
            assertEquals(0, bd("68.70").compareTo(lineaDe(lineas, 1101).getAbono()));
            assertEquals(0, bd("103.06").compareTo(lineaDe(lineas, 2101).getAbono()));
            assertEquals(0, suma(lineas, true).compareTo(suma(lineas, false)));
            assertEquals(0, bd("171.76").compareTo(suma(lineas, true)));
            return null;
        });
    }

    @Test
    void laCompraConPorcentajeQueNoDivideExactoCuadraAlCentavoYNoGeneraLineasEnCero() {
        ResultadoCompra r = comprarConPorcentaje(Map.of(), "33.33");

        leer(em -> {
            List<PartidaDetalle> lineas = lineasDe(em, em.find(Partida.class, r.partida().getId()));
            assertEquals(0, bd("57.25").compareTo(lineaDe(lineas, 1101).getAbono()));
            assertEquals(0, bd("114.51").compareTo(lineaDe(lineas, 2101).getAbono()));
            assertEquals(0, suma(lineas, true).compareTo(suma(lineas, false)));
            assertTrue(lineas.stream().allMatch(l -> l.getCargo().signum() > 0 || l.getAbono().signum() > 0));
            return null;
        });
    }

    @Test
    void unaCompraAlContadoNoNecesitaLaCuentaDeProveedoresPeroUnaMixtaSi() {
        // 100 % contado: proveedores no se usa, aunque su codigo configurado no exista
        ResultadoCompra r = comprarConPorcentaje(Map.of("cuenta.proveedores", "9999"), "100");
        leer(em -> {
            List<PartidaDetalle> lineas = lineasDe(em, em.find(Partida.class, r.partida().getId()));
            assertEquals(3, lineas.size());
            assertEquals(0, bd("171.76").compareTo(lineaDe(lineas, 1101).getAbono()));
            return null;
        });

        // mixta: necesita ambas y, al faltar una, no queda nada del intento
        List<Object> antes = estado();
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> comprarConPorcentaje(Map.of("cuenta.proveedores", "9999"), "40"));
        assertTrue(ex.getMessage().contains("cuenta.proveedores"), ex.getMessage());
        assertEquals(antes, estado());
    }

    @Test
    void unaCompraMixtaSinLaCuentaDeCajaNoDejaNada() {
        List<Object> antes = estado();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> comprarConPorcentaje(Map.of("cuenta.caja", "9999"), "40"));

        assertTrue(ex.getMessage().contains("cuenta.caja"), ex.getMessage());
        assertEquals(antes, estado());
    }

    @Test
    void unPorcentajeFueraDeRangoEnLaCompraNoDejaNada() {
        List<Object> antes = estado();

        assertThrows(ReglaNegocioException.class, () -> comprarConPorcentaje(Map.of(), "100.01"));
        assertThrows(ReglaNegocioException.class, () -> comprarConPorcentaje(Map.of(), "-1"));

        assertEquals(antes, estado());
    }

    @Test
    void unFalloAMitadDeLaEscrituraDeUnaCompraMixtaRevierteTodo() {
        List<Object> antes = estado();

        assertThrows(IllegalStateException.class, () -> transaccion(c -> {
            c.reemplazar(PartidaDetalleDAO.class, new PartidaDetalleDAO() {
                @Override
                public PartidaDetalle crear(PartidaDetalle entidad) {
                    throw new IllegalStateException("fallo simulado al escribir la partida");
                }
            });
            return c.obtener(CompraService.class).registrar(new CompraCmd(proveedorId, empleadoId, null, null,
                    bd("40"), List.of(new LineaCompraCmd(loteA, almacenA, bd("5"), bd("2")))));
        }));

        assertEquals(antes, estado());
    }

    // -------------------------------------------------------------------------------------- venta

    @Test
    void laVentaDescuentaStockAlCostoPromedioPonderadoYContabilizaIngresoYCosto() {
        comprar(loteA, almacenA, "10", "5.00");
        comprar(loteA, almacenA, "10", "7.00"); // promedio ponderado = (50 + 70) / 20 = 6.0000

        ResultadoVenta r = transaccion(c -> c.obtener(VentaService.class).registrar(new VentaCmd(clienteId, empleadoId,
                LocalDate.of(2026, 5, 5), "Mostrador", bd("100"), List.of(new LineaVentaCmd(loteA, almacenA, bd("4"), bd("20.00"))))));

        assertEquals(bd("80.00"), r.subtotal());
        assertEquals(bd("10.40"), r.iva());
        assertEquals(bd("90.40"), r.total());
        assertEquals(bd("24.00"), r.costoVentas());
        leer(em -> {
            assertEquals(0, bd("16").compareTo(Bd.stock(em, loteA, almacenA)));
            Kardex salida = em.createQuery("SELECT k FROM Kardex k WHERE k.tipoMovimiento = :t", Kardex.class)
                    .setParameter("t", TipoMovimiento.Salida).getSingleResult();
            assertEquals(almacenA, salida.getOrigen().getId());
            assertNull(salida.getDestino());
            assertEquals(r.venta().getId(), salida.getVenta().getId());
            KardexDetalle d = em.createQuery("SELECT d FROM KardexDetalle d WHERE d.kardex = :k", KardexDetalle.class)
                    .setParameter("k", salida).getSingleResult();
            assertEquals(0, bd("6.0000").compareTo(d.getPrecioCosto()));
            assertEquals(0, bd("4").compareTo(d.getCantidad()));

            Partida p = em.find(Partida.class, r.partida().getId());
            assertEquals(TipoPartida.Ingreso, p.getTipo());
            List<PartidaDetalle> lineas = lineasDe(em, p);
            assertEquals(5, lineas.size());
            assertEquals(0, bd("90.40").compareTo(lineaDe(lineas, 1101).getCargo())); // contado: Caja
            assertEquals(0, bd("80.00").compareTo(lineaDe(lineas, 4101).getAbono()));
            assertEquals(0, bd("10.40").compareTo(lineaDe(lineas, 2102).getAbono()));
            assertEquals(0, bd("24.00").compareTo(lineaDe(lineas, 5101).getCargo()));
            assertEquals(0, bd("24.00").compareTo(lineaDe(lineas, 1104).getAbono()));
            assertEquals(0, suma(lineas, true).compareTo(suma(lineas, false)));
            assertEquals(0, bd("114.40").compareTo(suma(lineas, true)));
            return null;
        });
    }

    @Test
    void conCeroPorCientoDeContadoElCobroVaALaCuentaDeClientes() {
        comprar(loteA, almacenA, "10", "5.00");

        ResultadoVenta r = transaccion(c -> c.obtener(VentaService.class)
                .registrar(new VentaCmd(clienteId, empleadoId, null, null,
                        bd("0"),
                        List.of(new LineaVentaCmd(loteA, almacenA, bd("1"), bd("10.00"))))));

        leer(em -> {
            List<PartidaDetalle> lineas = lineasDe(em, em.find(Partida.class, r.partida().getId()));
            assertEquals(0, bd("11.30").compareTo(lineaDe(lineas, 1103).getCargo()));
            assertTrue(lineas.stream().noneMatch(l -> l.getCuenta().getCodigo() == 1101));
            return null;
        });
    }

    private ResultadoVenta venderConPorcentaje(Map<String, String> config, String porcentajeContado, String cantidad,
            String precio) {
        return transaccion(config, c -> c.obtener(VentaService.class).registrar(new VentaCmd(clienteId, empleadoId,
                LocalDate.of(2026, 5, 5), null, bd(porcentajeContado),
                List.of(new LineaVentaCmd(loteA, almacenA, bd(cantidad), bd(precio))))));
    }

    @Test
    void laVentaMixta40_60DebitaCajaYClientesYLaPartidaCuadra() {
        comprar(loteA, almacenA, "10", "5.00");

        // 4 x 20.00 = 80.00 ; IVA 10.40 ; total 90.40 ; costo 4 x 5.00 = 20.00
        ResultadoVenta r = venderConPorcentaje(Map.of(), "40", "4", "20.00");

        assertEquals(bd("90.40"), r.total());
        leer(em -> {
            List<PartidaDetalle> lineas = lineasDe(em, em.find(Partida.class, r.partida().getId()));
            assertEquals(6, lineas.size());
            assertEquals(0, bd("36.16").compareTo(lineaDe(lineas, 1101).getCargo()));
            assertEquals(0, bd("54.24").compareTo(lineaDe(lineas, 1103).getCargo()));
            assertEquals(0, bd("80.00").compareTo(lineaDe(lineas, 4101).getAbono()));
            assertEquals(0, bd("10.40").compareTo(lineaDe(lineas, 2102).getAbono()));
            assertEquals(0, bd("20.00").compareTo(lineaDe(lineas, 5101).getCargo()));
            assertEquals(0, bd("20.00").compareTo(lineaDe(lineas, 1104).getAbono()));
            assertEquals(0, suma(lineas, true).compareTo(suma(lineas, false)));
            return null;
        });
    }

    @Test
    void laVentaConPorcentajeQueNoDivideExactoCuadraAlCentavo() {
        comprar(loteA, almacenA, "10", "5.00");

        // 3 x 33.35 = 100.05 ; IVA 13.01 ; total 113.06 ; contado 33.33 % = 37.68 ; credito 75.38
        ResultadoVenta r = venderConPorcentaje(Map.of(), "33.33", "3", "33.35");

        assertEquals(bd("113.06"), r.total());
        leer(em -> {
            List<PartidaDetalle> lineas = lineasDe(em, em.find(Partida.class, r.partida().getId()));
            assertEquals(0, bd("37.68").compareTo(lineaDe(lineas, 1101).getCargo()));
            assertEquals(0, bd("75.38").compareTo(lineaDe(lineas, 1103).getCargo()));
            assertEquals(0, suma(lineas, true).compareTo(suma(lineas, false)));
            assertTrue(lineas.stream().allMatch(l -> l.getCargo().signum() > 0 || l.getAbono().signum() > 0));
            return null;
        });
    }

    @Test
    void unaVentaAlContadoNoNecesitaLaCuentaDeClientesPeroUnaMixtaSi() {
        comprar(loteA, almacenA, "10", "5.00");

        // 100 % contado: clientes no se usa, aunque su codigo configurado no exista
        ResultadoVenta r = venderConPorcentaje(Map.of("cuenta.clientes", "9999"), "100", "1", "10.00");
        leer(em -> {
            List<PartidaDetalle> lineas = lineasDe(em, em.find(Partida.class, r.partida().getId()));
            assertEquals(5, lineas.size());
            assertEquals(0, bd("11.30").compareTo(lineaDe(lineas, 1101).getCargo()));
            return null;
        });

        // mixta: necesita ambas y, al faltar una, no queda nada del intento
        List<Object> antes = estado();
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> venderConPorcentaje(Map.of("cuenta.clientes", "9999"), "40", "1", "10.00"));
        assertTrue(ex.getMessage().contains("cuenta.clientes"), ex.getMessage());
        assertEquals(antes, estado());
    }

    @Test
    void unPorcentajeFueraDeRangoEnLaVentaNoDejaNada() {
        comprar(loteA, almacenA, "10", "5.00");
        List<Object> antes = estado();

        assertThrows(ReglaNegocioException.class, () -> venderConPorcentaje(Map.of(), "100.01", "1", "10.00"));
        assertThrows(ReglaNegocioException.class, () -> venderConPorcentaje(Map.of(), "-1", "1", "10.00"));

        assertEquals(antes, estado());
    }

    @Test
    void unFalloAMitadDeLaEscrituraDeUnaVentaMixtaRevierteStockKardexYPartida() {
        comprar(loteA, almacenA, "10", "5.00");
        List<Object> antes = estado();

        assertThrows(IllegalStateException.class, () -> transaccion(c -> {
            c.reemplazar(PartidaDetalleDAO.class, new PartidaDetalleDAO() {
                @Override
                public PartidaDetalle crear(PartidaDetalle entidad) {
                    throw new IllegalStateException("fallo simulado al escribir la partida");
                }
            });
            return c.obtener(VentaService.class).registrar(new VentaCmd(clienteId, empleadoId, null, null, bd("40"),
                    List.of(new LineaVentaCmd(loteA, almacenA, bd("4"), bd("10")))));
        }));

        assertEquals(antes, estado());
    }

    @Test
    void conStockInsuficienteEnLaSegundaLineaNoQuedaNadaDeLaPrimera() {
        comprar(loteA, almacenA, "10", "5.00");
        comprar(loteB, almacenA, "1", "5.00");
        List<Object> antes = estado();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> transaccion(c -> c.obtener(VentaService.class).registrar(new VentaCmd(clienteId, empleadoId, null,
                        null, bd("100"), List.of(new LineaVentaCmd(loteA, almacenA, bd("3"), bd("10")),
                                new LineaVentaCmd(loteB, almacenA, bd("5"), bd("10")))))));

        assertTrue(ex.getMessage().contains("insuficiente"), ex.getMessage());
        assertEquals(antes, estado());
    }

    @Test
    void lineasQueSumadasSuperanElStockSeRechazanAunqueCadaUnaAlcance() {
        comprar(loteA, almacenA, "10", "5.00");
        List<Object> antes = estado();

        assertThrows(ReglaNegocioException.class,
                () -> transaccion(c -> c.obtener(VentaService.class).registrar(new VentaCmd(clienteId, empleadoId, null,
                        null, bd("100"), List.of(new LineaVentaCmd(loteA, almacenA, bd("6"), bd("10")),
                                new LineaVentaCmd(loteA, almacenA, bd("6"), bd("10")))))));

        assertEquals(antes, estado());
    }

    @Test
    void unaCuentaConfiguradaInexistenteImpideLaVentaSinDejarNada() {
        comprar(loteA, almacenA, "10", "5.00");
        List<Object> antes = estado();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> transaccion(Map.of("cuenta.ventas", "9999"), c -> c.obtener(VentaService.class).registrar(
                        new VentaCmd(clienteId, empleadoId, null, null,
                                bd("100"),
                                List.of(new LineaVentaCmd(loteA, almacenA, bd("1"), bd("10")))))));

        assertTrue(ex.getMessage().contains("cuenta.ventas"), ex.getMessage());
        assertEquals(antes, estado());
    }

    @Test
    void unFalloAMitadDeLaEscrituraDeUnaVentaRevierteStockKardexYPartida() {
        comprar(loteA, almacenA, "10", "5.00");
        List<Object> antes = estado();

        assertThrows(IllegalStateException.class, () -> transaccion(c -> {
            c.reemplazar(PartidaDetalleDAO.class, new PartidaDetalleDAO() {
                @Override
                public PartidaDetalle crear(PartidaDetalle entidad) {
                    throw new IllegalStateException("fallo simulado al escribir la partida");
                }
            });
            return c.obtener(VentaService.class).registrar(new VentaCmd(clienteId, empleadoId, null, null,
                    bd("100"),
                    List.of(new LineaVentaCmd(loteA, almacenA, bd("4"), bd("10")))));
        }));

        assertEquals(antes, estado());
        leer(em -> {
            assertEquals(0, bd("10").compareTo(Bd.stock(em, loteA, almacenA)));
            return null;
        });
    }

    @Test
    void unaVentaDeUnLoteSinComprasSeRechazaPorFaltaDeCosto() {
        // stock obtenido por un ajuste de entrada: no hay compra_detalle, por tanto no hay costo conocido
        transaccion(c -> c.obtener(AjusteService.class).registrar(new AjusteCmd(almacenA, loteB, bd("5"), "Inicial")));
        List<Object> antes = estado();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> transaccion(c -> c.obtener(VentaService.class).registrar(new VentaCmd(clienteId, empleadoId, null,
                        null, bd("100"), List.of(new LineaVentaCmd(loteB, almacenA, bd("1"), bd("10")))))));

        assertTrue(ex.getMessage().contains("costo"), ex.getMessage());
        assertEquals(antes, estado());
    }

    // ------------------------------------------------------------------------------------ traslado

    @Test
    void elTrasladoMueveStockEntreAlmacenesConUnKardexTrasladoYSinPartida() {
        comprar(loteA, almacenA, "10", "5.00");
        long partidasAntes = leer(em -> Bd.contar(em, Partida.class));

        MovimientoKardex m = transaccion(c -> c.obtener(TrasladoService.class).registrar(new TrasladoCmd(almacenA,
                almacenB, "Reabastecer sucursal", List.of(new LineaTrasladoCmd(loteA, bd("4"))))));

        leer(em -> {
            assertEquals(0, bd("6").compareTo(Bd.stock(em, loteA, almacenA)));
            assertEquals(0, bd("4").compareTo(Bd.stock(em, loteA, almacenB)));
            Kardex k = em.find(Kardex.class, m.kardex().getId());
            assertEquals(TipoMovimiento.Traslado, k.getTipoMovimiento());
            assertEquals(almacenA, k.getOrigen().getId());
            assertEquals(almacenB, k.getDestino().getId());
            assertNull(k.getCompra());
            assertNull(k.getVenta());
            assertEquals(partidasAntes, Bd.contar(em, Partida.class));
            return null;
        });
    }

    @Test
    void unTrasladoConStockInsuficienteOConAlmacenesIgualesNoDejaNada() {
        comprar(loteA, almacenA, "3", "5.00");
        List<Object> antes = estado();

        assertThrows(ReglaNegocioException.class, () -> transaccion(c -> c.obtener(TrasladoService.class).registrar(
                new TrasladoCmd(almacenA, almacenB, null, List.of(new LineaTrasladoCmd(loteA, bd("4")))))));
        assertThrows(ReglaNegocioException.class, () -> transaccion(c -> c.obtener(TrasladoService.class).registrar(
                new TrasladoCmd(almacenA, almacenA, null, List.of(new LineaTrasladoCmd(loteA, bd("1")))))));

        assertEquals(antes, estado());
    }

    // -------------------------------------------------------------------------------------- ajuste

    @Test
    void elAjusteDeEntradaYSalidaUsaDestinoYOrigenConCantidadPositiva() {
        comprar(loteA, almacenA, "10", "5.00");

        MovimientoKardex entrada = transaccion(c -> c.obtener(AjusteService.class)
                .registrar(new AjusteCmd(almacenA, loteA, bd("2"), "Sobrante")).movimiento());
        MovimientoKardex salida = transaccion(c -> c.obtener(AjusteService.class)
                .registrar(new AjusteCmd(almacenA, loteA, bd("-5"), "Merma")).movimiento());

        leer(em -> {
            assertEquals(0, bd("7").compareTo(Bd.stock(em, loteA, almacenA)));
            Kardex e = em.find(Kardex.class, entrada.kardex().getId());
            assertEquals(TipoMovimiento.Ajuste, e.getTipoMovimiento());
            assertNull(e.getOrigen());
            assertEquals(almacenA, e.getDestino().getId());
            assertEquals("Sobrante", e.getDescripcion());
            Kardex s = em.find(Kardex.class, salida.kardex().getId());
            assertEquals(almacenA, s.getOrigen().getId());
            assertNull(s.getDestino());
            KardexDetalle d = em.createQuery("SELECT d FROM KardexDetalle d WHERE d.kardex = :k", KardexDetalle.class)
                    .setParameter("k", s).getSingleResult();
            assertEquals(0, bd("5").compareTo(d.getCantidad()));
            assertTrue(d.getCantidad().signum() > 0);
            return null;
        });
    }

    @Test
    void conPartidaDeAjusteActivaSeContabilizaContraLaCuentaDeAjuste() {
        comprar(loteA, almacenA, "10", "5.00");

        ResultadoAjuste r = transaccion(Map.of(ConfigContable.AJUSTE_GENERA_PARTIDA, "true"), c -> c.obtener(
                AjusteService.class).registrar(new AjusteCmd(almacenA, loteA, bd("-2"), "Merma")));

        assertNotNull(r.partida());
        leer(em -> {
            Partida p = em.find(Partida.class, r.partida().getId());
            assertEquals(TipoPartida.Ajuste, p.getTipo());
            List<PartidaDetalle> lineas = lineasDe(em, p);
            assertEquals(2, lineas.size());
            assertEquals(0, bd("10.00").compareTo(lineaDe(lineas, 5301).getCargo()));
            assertEquals(0, bd("10.00").compareTo(lineaDe(lineas, 1104).getAbono()));
            return null;
        });
    }

    @Test
    void unAjusteDeSalidaMayorQueElStockNoDejaNada() {
        comprar(loteA, almacenA, "3", "5.00");
        List<Object> antes = estado();

        assertThrows(ReglaNegocioException.class, () -> transaccion(c -> c.obtener(AjusteService.class)
                .registrar(new AjusteCmd(almacenA, loteA, bd("-4"), "Merma"))));

        assertEquals(antes, estado());
    }
}
