package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.CuentaDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.CuentaCmd;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CuentaServiceTest {

    @Mock
    CuentaDAO cuentaDAO;
    @InjectMocks
    CuentaService servicio;

    // Arbol: 1 Activo (grupo) -> 11 Caja y Bancos (hojas); 4 Ingresos (acreedora, raiz sin hijos).
    private final Cuenta activo = cuenta(1, 1, NaturalezaCuenta.Deudora, true, false, null);
    private final Cuenta caja = cuenta(2, 11, NaturalezaCuenta.Deudora, true, true, activo);
    private final Cuenta ingresos = cuenta(4, 4, NaturalezaCuenta.Acreedora, true, false, null);

    private static Cuenta cuenta(long id, int codigo, NaturalezaCuenta nat, boolean activa, boolean imputable,
            Cuenta padre) {
        Cuenta c = Fabrica.cuenta(id, codigo, nat, activa, imputable);
        c.setPadre(padre);
        return c;
    }

    @BeforeEach
    void preparar() {
        for (Cuenta c : List.of(activo, caja, ingresos)) {
            when(cuentaDAO.buscarPorId(c.getId())).thenReturn(Optional.of(c));
            when(cuentaDAO.findByCodigo(c.getCodigo())).thenReturn(Optional.of(c));
        }
        when(cuentaDAO.findHijos(activo)).thenReturn(List.of(caja));
        when(cuentaDAO.findHijos(caja)).thenReturn(List.of());
        when(cuentaDAO.findHijos(ingresos)).thenReturn(List.of());
        when(cuentaDAO.findByCodigo(1104)).thenReturn(Optional.empty());
        when(cuentaDAO.findByCodigo(12)).thenReturn(Optional.empty());
        when(cuentaDAO.findByCodigo(5)).thenReturn(Optional.empty());
        when(cuentaDAO.findByCodigo(111)).thenReturn(Optional.empty());
        when(cuentaDAO.findByCodigo(41)).thenReturn(Optional.empty());
        when(cuentaDAO.findByCodigo(2)).thenReturn(Optional.empty());
    }

    private void noSeCreoNiModificoNada() {
        verify(cuentaDAO, never()).crear(any());
        verify(cuentaDAO, never()).modificar(any());
    }

    // ------------------------------------------------------------------------------------------ crear

    @Test
    void crearCuentaRaizActivaYNoImputablePorDefecto() {
        Cuenta c = servicio.crear(new CuentaCmd(5, " Pasivo ", NaturalezaCuenta.Acreedora, null, null));

        verify(cuentaDAO).crear(c);
        verify(cuentaDAO).flush();
        assertEquals(5, c.getCodigo());
        assertEquals("Pasivo", c.getNombre());
        assertEquals(NaturalezaCuenta.Acreedora, c.getNaturaleza());
        assertTrue(c.getActivo());
        assertFalse(c.getImputable());
        assertEquals(null, c.getPadre());
    }

    @Test
    void crearHijoHeredaLaNaturalezaDelPadreSiNoSeIndica() {
        Cuenta c = servicio.crear(new CuentaCmd(12, "Bancos", null, true, 1L));

        assertSame(activo, c.getPadre());
        assertEquals(NaturalezaCuenta.Deudora, c.getNaturaleza());
        assertTrue(c.getImputable());
    }

    @Test
    void crearHijoConLaMismaNaturalezaExplicitaEsValido() {
        Cuenta c = servicio.crear(new CuentaCmd(12, "Bancos", NaturalezaCuenta.Deudora, true, 1L));

        assertEquals(NaturalezaCuenta.Deudora, c.getNaturaleza());
    }

    @Test
    void crearHijoConNaturalezaDistintaALaDelPadreSeRechaza() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CuentaCmd(12, "Bancos", NaturalezaCuenta.Acreedora, true, 1L)));

        assertTrue(ex.getMessage().contains("naturaleza"), ex.getMessage());
        noSeCreoNiModificoNada();
    }

    @Test
    void crearRaizSinNaturalezaSeRechaza() {
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(new CuentaCmd(5, "Pasivo", null, false, null)));
        noSeCreoNiModificoNada();
    }

    @Test
    void crearConCodigoNombreOComandoVacioSeRechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CuentaCmd(null, "X", NaturalezaCuenta.Deudora, false, null)));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CuentaCmd(5, "  ", NaturalezaCuenta.Deudora, false, null)));
        assertThrows(ReglaNegocioException.class, () -> servicio.crear(null));
        noSeCreoNiModificoNada();
    }

    @Test
    void crearConCodigoYaExistenteSeRechaza() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CuentaCmd(11, "Duplicada", NaturalezaCuenta.Deudora, true, 1L)));

        assertTrue(ex.getMessage().contains("11"), ex.getMessage());
        noSeCreoNiModificoNada();
    }

    @Test
    void crearHijoConCodigoQueNoEmpiezaPorElDelPadreSeRechaza() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CuentaCmd(2, "Mal codigo", NaturalezaCuenta.Deudora, true, 1L)));

        assertTrue(ex.getMessage().contains("empezar"), ex.getMessage());
        noSeCreoNiModificoNada();
    }

    @Test
    void crearHijoDeUnPadreInexistenteFallaConNoEncontrado() {
        when(cuentaDAO.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.crear(new CuentaCmd(12, "X", NaturalezaCuenta.Deudora, true, 99L)));
    }

    @Test
    void crearHijoDeUnPadreInactivoSeRechaza() {
        activo.setActivo(false);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CuentaCmd(12, "X", NaturalezaCuenta.Deudora, true, 1L)));
        noSeCreoNiModificoNada();
    }

    @Test
    void crearHijoDeUnPadreImputableSeRechazaPorqueUnaCuentaConHijosNoEsImputable() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.crear(new CuentaCmd(111, "Subcaja", NaturalezaCuenta.Deudora, true, 2L)));

        assertTrue(ex.getMessage().contains("imputable"), ex.getMessage());
        noSeCreoNiModificoNada();
    }

    // -------------------------------------------------------------------------------------- modificar

    @Test
    void modificarCambiaNombreEImputableDeUnaHoja() {
        Cuenta c = servicio.modificar(2L, new CuentaCmd(11, "Caja general", null, true, 1L));

        verify(cuentaDAO).modificar(caja);
        assertSame(caja, c);
        assertEquals("Caja general", c.getNombre());
    }

    @Test
    void modificarPuedeConservarSuPropioCodigo() {
        servicio.modificar(2L, new CuentaCmd(11, "Caja", NaturalezaCuenta.Deudora, true, 1L));

        verify(cuentaDAO).modificar(caja);
    }

    @Test
    void modificarConElCodigoDeOtraCuentaSeRechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(4L, new CuentaCmd(11, "Ingresos", null, false, null)));
        noSeCreoNiModificoNada();
    }

    @Test
    void modificarComoPadreASiMismaOASuDescendienteSeRechazaPorCiclo() {
        ReglaNegocioException propio = assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(1L, new CuentaCmd(1, "Activo", null, false, 1L)));
        ReglaNegocioException descendiente = assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(1L, new CuentaCmd(1, "Activo", null, false, 2L)));

        assertTrue(propio.getMessage().contains("ciclo"), propio.getMessage());
        assertTrue(descendiente.getMessage().contains("ciclo"), descendiente.getMessage());
        noSeCreoNiModificoNada();
    }

    @Test
    void modificarUnaCuentaConHijosParaHacerlaImputableSeRechaza() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(1L, new CuentaCmd(1, "Activo", null, true, null)));

        assertTrue(ex.getMessage().contains("hijas"), ex.getMessage());
        noSeCreoNiModificoNada();
    }

    @Test
    void cambiarLaNaturalezaConHijosOConMovimientosSeRechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(1L, new CuentaCmd(1, "Activo", NaturalezaCuenta.Acreedora, false, null)));

        when(cuentaDAO.tieneMovimientos(ingresos)).thenReturn(true);
        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(4L, new CuentaCmd(4, "Ingresos", NaturalezaCuenta.Deudora, false, null)));
        noSeCreoNiModificoNada();
    }

    @Test
    void cambiarLaNaturalezaDeUnaRaizSinHijosNiMovimientosSePermite() {
        Cuenta c = servicio.modificar(4L, new CuentaCmd(4, "Ingresos", NaturalezaCuenta.Deudora, false, null));

        assertEquals(NaturalezaCuenta.Deudora, c.getNaturaleza());
    }

    @Test
    void cambiarElCodigoDeUnPadreQueDejaIncompatiblesASusHijosSeRechaza() {
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(1L, new CuentaCmd(2, "Activo", null, false, null)));

        assertTrue(ex.getMessage().contains("11"), ex.getMessage());
        noSeCreoNiModificoNada();
    }

    @Test
    void moverBajoUnPadreConCodigoIncompatibleSeRechaza() {
        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(2L, new CuentaCmd(11, "Caja", null, true, 4L)));
        noSeCreoNiModificoNada();
    }

    @Test
    void moverAUnPadreConOtraNaturalezaHeredaLaDelPadreSiNoHayMovimientos() {
        Cuenta pasivo = cuenta(7, 1, NaturalezaCuenta.Acreedora, true, false, null);
        Cuenta hoja = cuenta(8, 15, NaturalezaCuenta.Deudora, true, true, null);
        when(cuentaDAO.buscarPorId(7L)).thenReturn(Optional.of(pasivo));
        when(cuentaDAO.buscarPorId(8L)).thenReturn(Optional.of(hoja));
        when(cuentaDAO.findByCodigo(15)).thenReturn(Optional.of(hoja));
        when(cuentaDAO.findHijos(hoja)).thenReturn(List.of());

        Cuenta c = servicio.modificar(8L, new CuentaCmd(15, "Hoja", null, true, 7L));

        assertSame(pasivo, c.getPadre());
        assertEquals(NaturalezaCuenta.Acreedora, c.getNaturaleza());
    }

    @Test
    void moverAUnPadreConOtraNaturalezaSeRechazaSiLaCuentaYaTieneMovimientos() {
        Cuenta pasivo = cuenta(7, 1, NaturalezaCuenta.Acreedora, true, false, null);
        Cuenta hoja = cuenta(8, 15, NaturalezaCuenta.Deudora, true, true, null);
        when(cuentaDAO.buscarPorId(7L)).thenReturn(Optional.of(pasivo));
        when(cuentaDAO.buscarPorId(8L)).thenReturn(Optional.of(hoja));
        when(cuentaDAO.findByCodigo(15)).thenReturn(Optional.of(hoja));
        when(cuentaDAO.findHijos(hoja)).thenReturn(List.of());
        when(cuentaDAO.tieneMovimientos(hoja)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.modificar(8L, new CuentaCmd(15, "Hoja", null, true, 7L)));
        assertEquals(NaturalezaCuenta.Deudora, hoja.getNaturaleza());
        noSeCreoNiModificoNada();
    }

    @Test
    void modificarInexistenteFallaConNoEncontrado() {
        when(cuentaDAO.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class,
                () -> servicio.modificar(99L, new CuentaCmd(1, "X", NaturalezaCuenta.Deudora, false, null)));
    }

    // ------------------------------------------------------------------------------------- activar/baja

    @Test
    void desactivarUnaHojaSinMovimientosLaDaDeBajaLogica() {
        Cuenta c = servicio.desactivar(2L);

        assertFalse(c.getActivo());
        verify(cuentaDAO).modificar(caja);
        verify(cuentaDAO, never()).eliminar(any());
    }

    @Test
    void desactivarConHijosActivosSeRechaza() {
        when(cuentaDAO.existenHijosActivos(activo)).thenReturn(true);

        assertThrows(ReglaNegocioException.class, () -> servicio.desactivar(1L));
        assertTrue(activo.getActivo());
        noSeCreoNiModificoNada();
    }

    @Test
    void desactivarConMovimientosSeRechaza() {
        when(cuentaDAO.tieneMovimientos(caja)).thenReturn(true);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> servicio.desactivar(2L));

        assertTrue(ex.getMessage().contains("movimientos"), ex.getMessage());
        noSeCreoNiModificoNada();
    }

    @Test
    void desactivarUnaCuentaYaInactivaEsIdempotente() {
        caja.setActivo(false);

        Cuenta c = servicio.desactivar(2L);

        assertFalse(c.getActivo());
        verify(cuentaDAO, never()).modificar(any());
    }

    @Test
    void activarRequierePadreActivo() {
        caja.setActivo(false);
        activo.setActivo(false);
        assertThrows(ReglaNegocioException.class, () -> servicio.activar(2L));

        activo.setActivo(true);
        assertTrue(servicio.activar(2L).getActivo());
        verify(cuentaDAO).modificar(caja);
    }

    // ------------------------------------------------------------------------------------- consultas

    @Test
    void consultasDelegan() {
        when(cuentaDAO.findRange(0, 10)).thenReturn(List.of(activo));
        when(cuentaDAO.findTodas()).thenReturn(List.of(activo, caja));

        assertSame(activo, servicio.buscarPorId(1L));
        assertSame(caja, servicio.buscarPorCodigo(11));
        assertEquals(List.of(caja), servicio.hijos(1L));
        assertEquals(List.of(activo), servicio.listar(0, 10));
        assertEquals(List.of(activo, caja), servicio.todas());
        when(cuentaDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        when(cuentaDAO.findByCodigo(9999)).thenReturn(Optional.empty());
        assertThrows(NoEncontradoException.class, () -> servicio.buscarPorId(99L));
        assertThrows(NoEncontradoException.class, () -> servicio.buscarPorCodigo(9999));
    }

    @Test
    void noExisteBorradoFisicoDeCuentas() {
        for (var m : CuentaService.class.getDeclaredMethods()) {
            String nombre = m.getName().toLowerCase();
            assertFalse(nombre.contains("elimin") || nombre.contains("borr"), "baja logica solamente: " + m.getName());
        }
    }
}
