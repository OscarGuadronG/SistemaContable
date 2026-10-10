package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.CompraDAO;
import com.sistemacontable.control.DepartamentoDAO;
import com.sistemacontable.control.EmpleadoDAO;
import com.sistemacontable.control.PlanillaDetalleDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.DepartamentoCmd;
import com.sistemacontable.control.service.cmd.EmpleadoCmd;
import com.sistemacontable.entity.Departamento;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.enums.EstadoEmpleado;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmpleadoDepartamentoServiceTest {

    @Mock
    DepartamentoDAO departamentoDAO;
    @Mock
    EmpleadoDAO empleadoDAO;
    @Mock
    CompraDAO compraDAO;
    @Mock
    VentaDAO ventaDAO;
    @Mock
    PlanillaDetalleDAO planillaDetalleDAO;
    @InjectMocks
    DepartamentoService departamentos;
    @InjectMocks
    EmpleadoService empleados;

    private final Departamento ventas = departamento(1L);
    private final Empleado ana = empleado(10L, "00000001-1", "ISSS-1");

    private static Departamento departamento(long id) {
        Departamento d = new Departamento();
        d.setId(id);
        d.setNombre("Ventas");
        return d;
    }

    private static Empleado empleado(long id, String dui, String isss) {
        Empleado e = new Empleado();
        e.setId(id);
        e.setNombre("Ana");
        e.setApellido("Lopez");
        e.setDui(dui);
        e.setNumeroIsss(isss);
        e.setSalario(new BigDecimal("500"));
        e.setEstado(EstadoEmpleado.Activo);
        return e;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void preparar() {
        when(departamentoDAO.buscarPorId(1L)).thenReturn(Optional.of(ventas));
        when(empleadoDAO.buscarPorId(10L)).thenReturn(Optional.of(ana));
        when(empleadoDAO.findByDui("00000001-1")).thenReturn(Optional.of(ana));
        when(empleadoDAO.findByNumeroIsss("ISSS-1")).thenReturn(Optional.of(ana));
    }

    // ------------------------------------------------------------------------------ departamentos

    @Test
    void departamentoSeCreaConNombreObligatorio() {
        Departamento d = departamentos.crear(new DepartamentoCmd(" Compras ", " "));

        verify(departamentoDAO).crear(d);
        assertEquals("Compras", d.getNombre());
        assertNull(d.getDescripcion());
        assertThrows(ReglaNegocioException.class, () -> departamentos.crear(new DepartamentoCmd(" ", null)));
        assertThrows(ReglaNegocioException.class, () -> departamentos.crear(null));
    }

    @Test
    void departamentoSeModificaYInexistenteFalla() {
        Departamento d = departamentos.modificar(1L, new DepartamentoCmd("Ventas y mercadeo", "Todo"));

        assertEquals("Ventas y mercadeo", d.getNombre());
        verify(departamentoDAO).modificar(ventas);
        when(departamentoDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> departamentos.modificar(99L, new DepartamentoCmd("x", null)));
    }

    @Test
    void departamentoConEmpleadosNoSeElimina() {
        when(empleadoDAO.existePorReferencia("departamento", ventas)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> departamentos.eliminar(1L));

        assertTrue(ex.getMessage().contains("empleados"), ex.getMessage());
        verify(departamentoDAO, never()).eliminar(any());
    }

    @Test
    void departamentoSinReferenciasSeElimina() {
        departamentos.eliminar(1L);

        verify(departamentoDAO).eliminar(1L);
        when(departamentoDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> departamentos.eliminar(99L));
    }

    @Test
    void departamentoConsultas() {
        when(departamentoDAO.findRange(0, 5)).thenReturn(List.of(ventas));

        assertSame(ventas, departamentos.buscarPorId(1L));
        assertEquals(List.of(ventas), departamentos.listar(0, 5));
    }

    // --------------------------------------------------------------------------------- empleados

    private EmpleadoCmd cmdEmpleado() {
        return new EmpleadoCmd(" Luis ", "Perez", null, 1L, bd("800.00"), null, " ISSS-2 ", " 00000002-2 ");
    }

    @Nested
    class Altas {

        @Test
        void creaEmpleadoActivoPorDefectoConDatosNormalizados() {
            Empleado e = empleados.crear(cmdEmpleado());

            verify(empleadoDAO).crear(e);
            assertEquals("Luis", e.getNombre());
            assertEquals("ISSS-2", e.getNumeroIsss());
            assertEquals("00000002-2", e.getDui());
            assertEquals(EstadoEmpleado.Activo, e.getEstado());
            assertSame(ventas, e.getDepartamento());
            assertEquals(bd("800.00"), e.getSalario());
        }

        @Test
        void aceptaSalarioCeroYEstadoExplicito() {
            Empleado e = empleados.crear(new EmpleadoCmd("Luis", "Perez", null, 1L, BigDecimal.ZERO,
                    EstadoEmpleado.Suspendido, "ISSS-2", "00000002-2"));

            assertEquals(EstadoEmpleado.Suspendido, e.getEstado());
        }

        @Test
        void rechazaSalarioNegativoONuloYCamposVacios() {
            for (EmpleadoCmd cmd : List.of(
                    new EmpleadoCmd("Luis", "Perez", null, 1L, bd("-1"), null, "ISSS-2", "00000002-2"),
                    new EmpleadoCmd("Luis", "Perez", null, 1L, null, null, "ISSS-2", "00000002-2"),
                    new EmpleadoCmd(" ", "Perez", null, 1L, bd("1"), null, "ISSS-2", "00000002-2"),
                    new EmpleadoCmd("Luis", null, null, 1L, bd("1"), null, "ISSS-2", "00000002-2"),
                    new EmpleadoCmd("Luis", "Perez", null, null, bd("1"), null, "ISSS-2", "00000002-2"),
                    new EmpleadoCmd("Luis", "Perez", null, 1L, bd("1"), null, " ", "00000002-2"),
                    new EmpleadoCmd("Luis", "Perez", null, 1L, bd("1"), null, "ISSS-2", null))) {
                assertThrows(ReglaNegocioException.class, () -> empleados.crear(cmd));
            }
            assertThrows(ReglaNegocioException.class, () -> empleados.crear(null));
            verify(empleadoDAO, never()).crear(any());
        }

        @Test
        void departamentoInexistenteFalla() {
            when(departamentoDAO.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThrows(NoEncontradoException.class, () -> empleados.crear(
                    new EmpleadoCmd("Luis", "Perez", null, 99L, bd("1"), null, "ISSS-2", "00000002-2")));
        }

        @Test
        void duiOIsssRepetidosSeRechazanConMensajeClaroAntesDeLaConstraint() {
            ReglaNegocioException dui = assertThrows(ReglaNegocioException.class, () -> empleados.crear(
                    new EmpleadoCmd("Luis", "Perez", null, 1L, bd("1"), null, "ISSS-2", "00000001-1")));
            ReglaNegocioException isss = assertThrows(ReglaNegocioException.class, () -> empleados.crear(
                    new EmpleadoCmd("Luis", "Perez", null, 1L, bd("1"), null, "ISSS-1", "00000002-2")));

            assertTrue(dui.getMessage().contains("DUI") && dui.getMessage().contains("00000001-1"), dui.getMessage());
            assertTrue(isss.getMessage().contains("ISSS") && isss.getMessage().contains("ISSS-1"), isss.getMessage());
            verify(empleadoDAO, never()).crear(any());
        }
    }

    @Nested
    class Cambios {

        @Test
        void modificarPuedeConservarSuPropioDuiEIsss() {
            Empleado e = empleados.modificar(10L, new EmpleadoCmd("Ana Maria", "Lopez", null, 1L, bd("900"), null,
                    "ISSS-1", "00000001-1"));

            assertEquals("Ana Maria", e.getNombre());
            assertEquals(bd("900"), e.getSalario());
            assertEquals(EstadoEmpleado.Activo, e.getEstado()); // estado nulo conserva el actual
            verify(empleadoDAO).modificar(ana);
        }

        @Test
        void modificarConElDuiDeOtroEmpleadoSeRechaza() {
            Empleado otro = empleado(11L, "00000002-2", "ISSS-2");
            when(empleadoDAO.findByDui("00000002-2")).thenReturn(Optional.of(otro));

            assertThrows(ReglaNegocioException.class, () -> empleados.modificar(10L, new EmpleadoCmd("Ana", "Lopez",
                    null, 1L, bd("1"), null, "ISSS-1", "00000002-2")));
            verify(empleadoDAO, never()).modificar(any());
        }

        @Test
        void cambiarEstadoDelEmpleado() {
            assertEquals(EstadoEmpleado.Inactivo, empleados.cambiarEstado(10L, EstadoEmpleado.Inactivo).getEstado());
            verify(empleadoDAO).modificar(ana);
            assertThrows(ReglaNegocioException.class, () -> empleados.cambiarEstado(10L, null));
        }

        @Test
        void modificarInexistenteFalla() {
            when(empleadoDAO.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThrows(NoEncontradoException.class, () -> empleados.modificar(99L, cmdEmpleado()));
        }
    }

    @Nested
    class Bajas {

        @Test
        void conComprasVentasOPlanillasNoSeElimina() {
            when(compraDAO.existePorReferencia("empleado", ana)).thenReturn(true);
            assertThrows(ReglaNegocioException.class, () -> empleados.eliminar(10L));

            when(compraDAO.existePorReferencia("empleado", ana)).thenReturn(false);
            when(ventaDAO.existePorReferencia("empleado", ana)).thenReturn(true);
            assertThrows(ReglaNegocioException.class, () -> empleados.eliminar(10L));

            when(ventaDAO.existePorReferencia("empleado", ana)).thenReturn(false);
            when(planillaDetalleDAO.existePorReferencia("empleado", ana)).thenReturn(true);
            assertThrows(ReglaNegocioException.class, () -> empleados.eliminar(10L));

            verify(empleadoDAO, never()).eliminar(any());
        }

        @Test
        void sinReferenciasSeElimina() {
            empleados.eliminar(10L);

            verify(empleadoDAO).eliminar(10L);
        }
    }

    @Test
    void empleadoConsultas() {
        when(empleadoDAO.findActivos()).thenReturn(List.of(ana));
        when(empleadoDAO.findRange(0, 5)).thenReturn(List.of(ana));

        assertSame(ana, empleados.buscarPorId(10L));
        assertEquals(List.of(ana), empleados.activos());
        assertEquals(List.of(ana), empleados.listar(0, 5));
    }
}
