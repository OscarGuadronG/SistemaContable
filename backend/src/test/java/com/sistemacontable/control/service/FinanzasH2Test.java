package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.*;
import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.config.ConfigContable;
import com.sistemacontable.control.service.cmd.*;
import com.sistemacontable.entity.*;
import com.sistemacontable.entity.enums.*;
import com.sistemacontable.security.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Flujo real JPA: compra con gastos → FIFO → devoluciones → saldos y cobro. Sin credenciales externas. */
class FinanzasH2Test {
    @Test void documentosPagosYDevolucionesSonConsultablesConSusAsientosCuadrados() {
        EntityManager em=JpaTestSupport.nuevoEmConTransaccion();
        try {
            LocalDate fecha=LocalDate.of(2026,7,1);
            Periodo periodo=new Periodo();periodo.setAnioFiscal(2026);periodo.setTrimestre((short)1);periodo.setFechaInicio(fecha);periodo.setFechaFin(fecha.plusMonths(3).minusDays(1));em.persist(periodo);
            for(int codigo:new int[]{1101,11031,1104,11081,1110,2101,2108,5101,5102,5103,4101,4105,4109,5203,4108,5201}) {
                Cuenta c=new Cuenta();c.setCodigo(codigo);c.setNombre("Cuenta "+codigo);c.setActivo(true);c.setImputable(true);c.setNaturaleza(NaturalezaCuenta.Deudora);em.persist(c);
            }
            Departamento departamento=new Departamento();departamento.setNombre("Bodega");em.persist(departamento);
            Empleado empleado=new Empleado();empleado.setNombre("Ana");empleado.setApellido("Pérez");empleado.setDui("98765432-1");empleado.setNumeroIsss("987654321");empleado.setDepartamento(departamento);em.persist(empleado);
            UnidadMedida unidad=new UnidadMedida();unidad.setNombre("Unidad");unidad.setAbrev("u");em.persist(unidad);
            TipoProducto tipo=new TipoProducto();tipo.setNombre("Cereal");em.persist(tipo);
            Producto producto=new Producto();producto.setNombre("Cereal");producto.setTipo(tipo);producto.setUnidadMedida(unidad);em.persist(producto);
            Lote lote=new Lote();lote.setProducto(producto);lote.setNumLote("NUEVO");em.persist(lote);
            Almacen almacen=new Almacen();almacen.setNombre("Central");almacen.setEstado(true);em.persist(almacen);
            Cliente cliente=new Cliente();cliente.setNombre("Cliente");cliente.setNit("FIN-1");em.persist(cliente);
            Proveedor proveedor=new Proveedor();proveedor.setNombre("Proveedor");proveedor.setNit("FIN-2");em.persist(proveedor);em.flush();
            UsuarioActual usuario=new UsuarioActual();Perfil perfil=new Perfil();perfil.setId(UUID.randomUUID());perfil.setRol(RolApp.Administrador);perfil.setIdEmpleado(empleado.getId());usuario.establecer(perfil,"admin@example.test");
            Cableado cableado=new Cableado(em,com.sistemacontable.control.config.ConfigPruebas.con(java.util.Map.of()));cableado.reemplazar(UsuarioActual.class,usuario);
            var compra=cableado.obtener(CompraService.class).registrar(new CompraCmd(proveedor.getId(),null,fecha,"CCF-1",null,FormaPago.Credito,null,BigDecimal.ZERO,new BigDecimal("200"),List.of(new LineaCompraCmd(lote.getId(),null,almacen.getId(),BigDecimal.TEN,new BigDecimal("80")))));
            assertMoney("1104",compra.total());
            var venta=cableado.obtener(VentaService.class).registrar(new VentaCmd(cliente.getId(),null,almacen.getId(),fecha.plusDays(1),"FAC-1",null,FormaPago.Credito,null,BigDecimal.ZERO,List.of(new LineaVentaCmd(producto.getId(),new BigDecimal("4")))));
            assertMoney("587.60",venta.total());assertMoney("400",venta.costoVentas());
            var devoluciones=cableado.obtener(DevolucionService.class);
            var dc=devoluciones.comprar(new DevolucionCompraCmd(compra.compra().getId(),fecha.plusDays(10),new BigDecimal("50"),"Defecto",null,List.of(new LineaDevolucionCmd(lote.getId(),almacen.getId(),BigDecimal.ONE))));
            assertMoney("100",dc.montoCosto());assertMoney("50",dc.montoCompensado());
            var dv=devoluciones.vender(new DevolucionVentaCmd(venta.venta().getId(),fecha.plusDays(10),"Cliente",null,List.of(new LineaDevolucionCmd(lote.getId(),almacen.getId(),BigDecimal.ONE))));
            assertMoney("130",dv.monto());assertMoney("100",dv.costo());
            var creditos=cableado.obtener(CreditoService.class);
            assertMoney("1054",creditos.obtener("compra",compra.compra().getId(),fecha.plusDays(45)).saldo());
            assertMoney("440.70",creditos.obtener("venta",venta.venta().getId(),fecha.plusDays(45)).saldo());
            var pago=creditos.pagar(new PagoCreditoCmd("venta",venta.venta().getId(),fecha.plusDays(46),new BigDecimal("200"),FormaPago.Contado,null,"Abono"));
            assertMoney("8",pago.pago().interes());
            assertMoney("240.70",creditos.obtener("venta",venta.venta().getId(),fecha.plusDays(46)).saldo());
            assertMoney("6",Bd.stock(em,lote.getId(),almacen.getId()));
            em.flush();
            List<Partida> partidas=em.createQuery("SELECT p FROM Partida p",Partida.class).getResultList();
            assertEquals(5,partidas.size());
            for(Partida p:partidas) {
                assertNotNull(p.getPeriodo());
                var lineas=em.createQuery("SELECT d FROM PartidaDetalle d WHERE d.partida.id=:id",PartidaDetalle.class).setParameter("id",p.getId()).getResultList();
                assertMoney(lineas.stream().map(PartidaDetalle::getCargo).reduce(BigDecimal.ZERO,BigDecimal::add).toPlainString(),lineas.stream().map(PartidaDetalle::getAbono).reduce(BigDecimal.ZERO,BigDecimal::add));
            }
            assertEquals(1,creditos.listar("venta","pendiente",cliente.getId()).size());
            assertEquals(1,devoluciones.listarCompra(null,null,compra.compra().getId()).size());
            assertEquals(1,devoluciones.listarVenta(null,null,venta.venta().getId()).size());
        } finally {JpaTestSupport.cerrarConRollback(em);}
    }
    private static void assertMoney(String expected,BigDecimal actual) {assertEquals(0,new BigDecimal(expected).compareTo(actual));}
}
