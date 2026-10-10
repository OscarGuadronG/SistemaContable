package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.AbstractDAO;
import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.CompraDetalleDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.ExistenciasDAO;
import com.sistemacontable.control.KardexDAO;
import com.sistemacontable.control.KardexDetalleDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.PartidaDAO;
import com.sistemacontable.control.PlanillaDetalleDAO;
import com.sistemacontable.control.ProductoDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.VentaDetalleDAO;
import com.sistemacontable.entity.Tercero;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Los servicios de catalogo rechazan borrados con {@code existePorReferencia(atributo, valor)}. Como el atributo
 * es un texto, esta prueba comprueba contra el metamodelo JPA real que TODAS las parejas DAO/atributo usadas por los
 * servicios existen (un error de tipeo solo saltaria en produccion cuando las comprobaciones anteriores dieran falso).
 */
class ReferenciasH2Test {

    private EntityManager em;
    private EscenarioContable esc;

    @BeforeEach
    void abrir() {
        em = JpaTestSupport.nuevoEmConTransaccion();
        esc = new EscenarioContable(em);
    }

    @AfterEach
    void cerrar() {
        JpaTestSupport.cerrarConRollback(em);
    }

    private <D extends AbstractDAO<?, ?>> D dao(D dao) {
        JpaTestSupport.inyectarEm(dao, em);
        return dao;
    }

    private void existe(AbstractDAO<?, ?> dao, String atributo, Object valor) {
        // lo que se comprueba es que la consulta se ejecute (atributo valido para esa entidad), no su resultado
        assertDoesNotThrow(() -> dao.existePorReferencia(atributo, valor),
                dao.getClass().getSimpleName() + "." + atributo);
    }

    @Test
    void todosLosAtributosDeReferenciaUsadosPorLosServiciosExisten() {
        Tercero tercero = new Tercero();
        tercero.setNombre("T");
        em.persist(tercero);
        em.flush();

        existe(dao(new EmpleadoDAO()), "departamento", esc.departamento);
        existe(dao(new CompraDAO()), "empleado", esc.empleado);
        existe(dao(new CompraDAO()), "proveedor", esc.proveedor);
        existe(dao(new VentaDAO()), "empleado", esc.empleado);
        existe(dao(new VentaDAO()), "cliente", esc.cliente);
        existe(dao(new PlanillaDetalleDAO()), "empleado", esc.empleado);
        existe(dao(new PartidaDAO()), "tercero", tercero);
        existe(dao(new ProductoDAO()), "unidadMedida", esc.producto.getUnidadMedida());
        existe(dao(new ProductoDAO()), "tipo", esc.producto.getTipo());
        existe(dao(new LoteDAO()), "producto", esc.producto);
        existe(dao(new ExistenciasDAO()), "lote", esc.loteA);
        existe(dao(new ExistenciasDAO()), "almacen", esc.almacenA);
        existe(dao(new CompraDetalleDAO()), "lote", esc.loteA);
        existe(dao(new CompraDetalleDAO()), "almacen", esc.almacenA);
        existe(dao(new VentaDetalleDAO()), "lote", esc.loteA);
        existe(dao(new VentaDetalleDAO()), "almacen", esc.almacenA);
        existe(dao(new KardexDetalleDAO()), "lote", esc.loteA);
        existe(dao(new KardexDAO()), "origen", esc.almacenA);
        existe(dao(new KardexDAO()), "destino", esc.almacenA);
    }
}
