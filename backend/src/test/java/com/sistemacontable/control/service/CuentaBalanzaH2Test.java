package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.config.ConfigPruebas;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.CuentaCmd;
import com.sistemacontable.control.service.cmd.LineaPartidaCmd;
import com.sistemacontable.control.service.cmd.PartidaCmd;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CuentaService, PartidaService y BalanzaService con DAOs reales sobre H2; todo termina en rollback. */
class CuentaBalanzaH2Test {

    private EntityManager em;
    private EscenarioContable esc;
    private CuentaService cuentas;
    private PartidaService partidas;
    private BalanzaService balanza;

    @BeforeEach
    void abrir() {
        em = JpaTestSupport.nuevoEmConTransaccion();
        esc = new EscenarioContable(em);
        Cableado cableado = new Cableado(em, ConfigPruebas.porDefecto());
        cuentas = cableado.obtener(CuentaService.class);
        partidas = cableado.obtener(PartidaService.class);
        balanza = cableado.obtener(BalanzaService.class);
    }

    @AfterEach
    void cerrar() {
        JpaTestSupport.cerrarConRollback(em);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void jerarquiaCiclosYBajaLogicaContraLaBaseReal() {
        Cuenta grupo = cuentas.crear(new CuentaCmd(9, "Grupo", NaturalezaCuenta.Deudora, false, null));
        Cuenta hijaA = cuentas.crear(new CuentaCmd(91, "Hija A", null, true, grupo.getId()));
        em.clear();

        assertEquals(NaturalezaCuenta.Deudora, cuentas.buscarPorCodigo(91).getNaturaleza());
        assertEquals(List.of(91), cuentas.hijos(grupo.getId()).stream().map(Cuenta::getCodigo).toList());
        // el grupo no puede colgar de su propia hija (ciclo) ni volverse imputable teniendo hijas
        assertThrows(ReglaNegocioException.class,
                () -> cuentas.modificar(grupo.getId(), new CuentaCmd(9, "Grupo", null, false, hijaA.getId())));
        assertThrows(ReglaNegocioException.class,
                () -> cuentas.modificar(grupo.getId(), new CuentaCmd(9, "Grupo", null, true, null)));
        // no se desactiva el grupo con hijas activas, pero si la hija y luego el grupo
        assertThrows(ReglaNegocioException.class, () -> cuentas.desactivar(grupo.getId()));
        assertFalse(cuentas.desactivar(hijaA.getId()).getActivo());
        assertFalse(cuentas.desactivar(grupo.getId()).getActivo());
    }

    @Test
    void unaCuentaConMovimientosNoSePuedeDesactivar() {
        Cuenta mov = cuentas.crear(new CuentaCmd(8, "Con movimientos", NaturalezaCuenta.Deudora, true, null));
        partidas.crear(new PartidaCmd(LocalDate.of(2026, 1, 10), "Apertura", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(mov.getId(), bd("10"), null),
                        new LineaPartidaCmd(esc.ventas.getId(), null, bd("10")))));
        em.flush();

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> cuentas.desactivar(mov.getId()));

        assertTrue(ex.getMessage().contains("movimientos"), ex.getMessage());
    }

    @Test
    void balanzaAgregaHijasAlPadreYCuadraConPartidasReales() {
        Cuenta grupo = cuentas.crear(new CuentaCmd(9, "Grupo", NaturalezaCuenta.Deudora, false, null));
        Cuenta hijaA = cuentas.crear(new CuentaCmd(91, "Hija A", null, true, grupo.getId()));
        Cuenta hijaB = cuentas.crear(new CuentaCmd(92, "Hija B", null, true, grupo.getId()));
        LocalDate dia = LocalDate.of(2026, 2, 10);
        partidas.crear(new PartidaCmd(dia, "Uno", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(hijaA.getId(), bd("100"), null),
                        new LineaPartidaCmd(esc.ventas.getId(), null, bd("100")))));
        partidas.crear(new PartidaCmd(dia, "Dos", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(hijaB.getId(), bd("40"), null),
                        new LineaPartidaCmd(hijaA.getId(), null, bd("15")),
                        new LineaPartidaCmd(esc.ventas.getId(), null, bd("25")))));
        // fuera del rango: no debe contar
        partidas.crear(new PartidaCmd(LocalDate.of(2026, 3, 10), "Marzo", TipoPartida.Diario, null, null, null,
                List.of(new LineaPartidaCmd(hijaA.getId(), bd("999"), null),
                        new LineaPartidaCmd(esc.ventas.getId(), null, bd("999")))));
        em.flush();
        em.clear();

        Balanza b = balanza.balanza(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

        FilaBalanza filaGrupo = b.filas().stream().filter(f -> f.codigo() == 9).findFirst().orElseThrow();
        assertEquals(bd("140.00"), filaGrupo.cargos());
        assertEquals(bd("15.00"), filaGrupo.abonos());
        assertEquals(bd("125.00"), filaGrupo.saldo());
        FilaBalanza filaVentas = b.filas().stream().filter(f -> f.codigo() == 4101).findFirst().orElseThrow();
        assertEquals(bd("125.00"), filaVentas.saldo());
        assertEquals(bd("140.00"), b.totalCargos());
        assertTrue(b.cuadra());
    }
}
