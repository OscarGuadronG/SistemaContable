package com.sistemacontable.control.service;

import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Departamento;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Producto;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.TipoProducto;
import com.sistemacontable.entity.UnidadMedida;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;

/**
 * Datos base para las pruebas de integracion (H2 + EclipseLink): plan de cuentas con los codigos por defecto de
 * {@code contabilidad.properties}, un empleado, proveedor, cliente, un producto con dos lotes y tres almacenes
 * (dos habilitados y uno deshabilitado). Todo se persiste en la transaccion de la prueba, que termina en rollback.
 */
final class EscenarioContable {

    final EntityManager em;

    Cuenta caja;
    Cuenta bancos;
    Cuenta clientes;
    Cuenta inventario;
    Cuenta ivaCredito;
    Cuenta proveedores;
    Cuenta ivaDebito;
    Cuenta retIsss;
    Cuenta retAfp;
    Cuenta retRenta;
    Cuenta ventas;
    Cuenta costoVentas;
    Cuenta sueldos;
    Cuenta ajusteInventario;

    Departamento departamento;
    Empleado empleado;
    Proveedor proveedor;
    Cliente cliente;
    Producto producto;
    Lote loteA;
    Lote loteB;
    Almacen almacenA;
    Almacen almacenB;
    Almacen almacenCerrado;

    EscenarioContable(EntityManager em) {
        this.em = em;
        sembrarCuentas();
        sembrarInventarioYPersonas();
        em.flush();
    }

    private Cuenta cuenta(int codigo, NaturalezaCuenta naturaleza) {
        Cuenta c = new Cuenta();
        c.setCodigo(codigo);
        c.setNombre("Cuenta " + codigo);
        c.setNaturaleza(naturaleza);
        c.setActivo(true);
        c.setImputable(true);
        em.persist(c);
        return c;
    }

    private void sembrarCuentas() {
        caja = cuenta(1101, NaturalezaCuenta.Deudora);
        bancos = cuenta(1102, NaturalezaCuenta.Deudora);
        clientes = cuenta(1103, NaturalezaCuenta.Deudora);
        inventario = cuenta(1104, NaturalezaCuenta.Deudora);
        ivaCredito = cuenta(1105, NaturalezaCuenta.Deudora);
        proveedores = cuenta(2101, NaturalezaCuenta.Acreedora);
        ivaDebito = cuenta(2102, NaturalezaCuenta.Acreedora);
        retIsss = cuenta(2103, NaturalezaCuenta.Acreedora);
        retAfp = cuenta(2104, NaturalezaCuenta.Acreedora);
        retRenta = cuenta(2105, NaturalezaCuenta.Acreedora);
        ventas = cuenta(4101, NaturalezaCuenta.Acreedora);
        costoVentas = cuenta(5101, NaturalezaCuenta.Deudora);
        sueldos = cuenta(5201, NaturalezaCuenta.Deudora);
        ajusteInventario = cuenta(5301, NaturalezaCuenta.Deudora);
    }

    private void sembrarInventarioYPersonas() {
        departamento = new Departamento();
        departamento.setNombre("Ventas");
        em.persist(departamento);

        empleado = nuevoEmpleado("Ana", "Lopez", "00000001-1", "ISSS-1", new BigDecimal("1000.00"));

        proveedor = new Proveedor();
        proveedor.setNombre("Proveedor SA");
        proveedor.setNit("0614-000000-001-1");
        em.persist(proveedor);

        cliente = new Cliente();
        cliente.setNombre("Cliente SA");
        cliente.setNit("0614-000000-002-2");
        em.persist(cliente);

        UnidadMedida unidad = new UnidadMedida();
        unidad.setNombre("Unidad");
        unidad.setAbrev("u");
        em.persist(unidad);

        TipoProducto tipo = new TipoProducto();
        tipo.setNombre("General");
        em.persist(tipo);

        producto = new Producto();
        producto.setNombre("Producto");
        producto.setUnidadMedida(unidad);
        producto.setTipo(tipo);
        em.persist(producto);

        loteA = lote("A-1");
        loteB = lote("B-1");
        almacenA = almacen("Central", true);
        almacenB = almacen("Sucursal", true);
        almacenCerrado = almacen("Cerrado", false);
    }

    Empleado nuevoEmpleado(String nombre, String apellido, String dui, String isss, BigDecimal salario) {
        Empleado e = new Empleado();
        e.setNombre(nombre);
        e.setApellido(apellido);
        e.setDepartamento(departamento);
        e.setSalario(salario);
        e.setNumeroIsss(isss);
        e.setDui(dui);
        em.persist(e);
        return e;
    }

    private Lote lote(String numero) {
        Lote l = new Lote();
        l.setProducto(producto);
        l.setNumLote(numero);
        em.persist(l);
        return l;
    }

    private Almacen almacen(String nombre, boolean habilitado) {
        Almacen a = new Almacen();
        a.setNombre(nombre);
        a.setEstado(habilitado);
        em.persist(a);
        return a;
    }

    /** Cantidad de filas de una entidad en la base (dentro de la transaccion actual). */
    long contar(Class<?> entidad) {
        Query q = em.createQuery("SELECT COUNT(e) FROM " + entidad.getSimpleName() + " e");
        return ((Number) q.getSingleResult()).longValue();
    }
}
