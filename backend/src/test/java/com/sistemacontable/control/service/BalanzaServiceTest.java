package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sistemacontable.control.CuentaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.PartidaDetalleDAO.SumaCuenta;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Cuenta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BalanzaServiceTest {

    @Mock
    CuentaDAO cuentaDAO;
    @Mock
    PartidaDetalleDAO detalleDAO;
    @InjectMocks
    BalanzaService servicio;

    private final LocalDate desde = LocalDate.of(2026, 1, 1);
    private final LocalDate hasta = LocalDate.of(2026, 1, 31);

    @Test
    void calculaLaBalanzaConElCatalogoYLasSumasDelRango() {
        Cuenta caja = Fabrica.cuenta(1, 1101);
        Cuenta ventas = Fabrica.cuenta(2, 4101);
        when(cuentaDAO.findTodas()).thenReturn(List.of(caja, ventas));
        when(detalleDAO.sumasPorCuenta(desde, hasta)).thenReturn(List.of(
                new SumaCuenta(1L, new BigDecimal("10"), BigDecimal.ZERO),
                new SumaCuenta(2L, BigDecimal.ZERO, new BigDecimal("10"))));

        Balanza b = servicio.balanza(desde, hasta);

        assertEquals(2, b.filas().size());
        assertTrue(b.cuadra());
        assertEquals(desde, b.desde());
        assertEquals(hasta, b.hasta());
    }

    @Test
    void rechazaRangosInvalidosSinConsultar() {
        assertThrows(ReglaNegocioException.class, () -> servicio.balanza(hasta, desde));
        assertThrows(ReglaNegocioException.class, () -> servicio.balanza(null, hasta));
        assertThrows(ReglaNegocioException.class, () -> servicio.balanza(desde, null));

        verify(detalleDAO, never()).sumasPorCuenta(desde, hasta);
    }
}
