package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.config.ConfigPruebas;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.ClienteCmd;
import com.sistemacontable.control.service.cmd.CompraCmd;
import com.sistemacontable.control.service.cmd.EmpleadoCmd;
import com.sistemacontable.control.service.cmd.LineaCompraCmd;
import com.sistemacontable.control.service.cmd.TipoProductoCmd;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Departamento;
import com.sistemacontable.entity.TipoProducto;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Servicios de catalogos con DAOs reales sobre H2: unicidad, referencias y jerarquia; todo en rollback. */
class CatalogosH2Test {

    private EntityManager em;
    private EscenarioContable esc;
    private Cableado cableado;

    @BeforeEach
    void abrir() {
        em = JpaTestSupport.nuevoEmConTransaccion();
        esc = new EscenarioContable(em);
        cableado = new Cableado(em, ConfigPruebas.porDefecto());
    }

    @AfterEach
    void cerrar() {
        JpaTestSupport.cerrarConRollback(em);
    }

    @Test
    void nitYDuiRepetidosSeRechazanAntesDeLaConstraint() {
        ClienteService clientes = cableado.obtener(ClienteService.class);
        EmpleadoService empleados = cableado.obtener(EmpleadoService.class);

        ReglaNegocioException nit = assertThrows(ReglaNegocioException.class,
                () -> clientes.crear(new ClienteCmd("Otro", null, esc.cliente.getNit())));
        ReglaNegocioException dui = assertThrows(ReglaNegocioException.class, () -> empleados.crear(new EmpleadoCmd(
                "Luis", "Perez", null, esc.departamento.getId(), BigDecimal.TEN, null, "ISSS-9", esc.empleado.getDui())));

        assertTrue(nit.getMessage().contains("NIT"), nit.getMessage());
        assertTrue(dui.getMessage().contains("DUI"), dui.getMessage());
        // y un alta valida si funciona
        assertDoesNotThrow(() -> clientes.crear(new ClienteCmd("Nuevo", null, "9999-9")));
    }

    @Test
    void nadaSeEliminaMientrasTengaReferenciasReales() {
        cableado.obtener(CompraService.class).registrar(new CompraCmd(esc.proveedor.getId(), esc.empleado.getId(), null,
                null, List.of(new LineaCompraCmd(esc.loteA.getId(), esc.almacenA.getId(), BigDecimal.ONE,
                        BigDecimal.TEN))));
        em.flush();

        assertThrows(ReglaNegocioException.class, () -> cableado.obtener(ProveedorService.class)
                .eliminar(esc.proveedor.getId()));
        assertThrows(ReglaNegocioException.class, () -> cableado.obtener(EmpleadoService.class)
                .eliminar(esc.empleado.getId()));
        assertThrows(ReglaNegocioException.class, () -> cableado.obtener(LoteService.class)
                .eliminar(esc.loteA.getId()));
        assertThrows(ReglaNegocioException.class, () -> cableado.obtener(AlmacenService.class)
                .eliminar(esc.almacenA.getId()));
        assertThrows(ReglaNegocioException.class, () -> cableado.obtener(ProductoService.class)
                .eliminar(esc.producto.getId()));
        assertThrows(ReglaNegocioException.class, () -> cableado.obtener(DepartamentoService.class)
                .eliminar(esc.departamento.getId()));
        assertThrows(ReglaNegocioException.class, () -> cableado.obtener(TipoProductoService.class)
                .eliminar(esc.producto.getTipo().getId()));
        // sin referencias: el cliente (sin ventas) y el almacen cerrado (sin movimientos) si se pueden borrar
        assertDoesNotThrow(() -> cableado.obtener(ClienteService.class).eliminar(esc.cliente.getId()));
        assertDoesNotThrow(() -> cableado.obtener(AlmacenService.class).eliminar(esc.almacenCerrado.getId()));
        em.flush();
        assertEquals(0L, Bd.contar(em, Cliente.class));
    }

    @Test
    void jerarquiaDeTiposSinCiclosConLaBaseReal() {
        TipoProductoService tipos = cableado.obtener(TipoProductoService.class);
        TipoProducto raiz = tipos.crear(new TipoProductoCmd("Alimentos", null));
        TipoProducto hijo = tipos.crear(new TipoProductoCmd("Lacteos", raiz.getId()));
        TipoProducto nieto = tipos.crear(new TipoProductoCmd("Quesos", hijo.getId()));
        em.flush();
        em.clear();

        assertThrows(ReglaNegocioException.class,
                () -> tipos.modificar(raiz.getId(), new TipoProductoCmd("Alimentos", nieto.getId())));
        assertEquals(List.of(hijo.getId()), tipos.hijos(raiz.getId()).stream().map(TipoProducto::getId).toList());
        assertThrows(ReglaNegocioException.class, () -> tipos.eliminar(raiz.getId()));
        assertDoesNotThrow(() -> tipos.eliminar(tipos.crear(new TipoProductoCmd("Suelto", null)).getId()));
    }

    @Test
    void unDepartamentoSinEmpleadosSePuedeEliminar() {
        DepartamentoService departamentos = cableado.obtener(DepartamentoService.class);
        Departamento vacio = departamentos.crear(new com.sistemacontable.control.service.cmd.DepartamentoCmd("Vacio", null));

        assertDoesNotThrow(() -> departamentos.eliminar(vacio.getId()));
    }
}
