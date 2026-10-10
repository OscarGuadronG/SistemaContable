package com.sistemacontable.control.service;

import com.sistemacontable.control.CuentaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.exception.ReglaNegocioException;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.LocalDate;

/**
 * Balanza de comprobacion por rango de fechas. Solo lee. Las filas incluyen a las cuentas padre con los
 * movimientos de sus hijas; ver {@link Balanza} y {@link CalculoBalanza}.
 */
@Stateless
@LocalBean
public class BalanzaService {

    @Inject
    private CuentaDAO cuentaDAO;
    @Inject
    private PartidaDetalleDAO partidaDetalleDAO;

    /** Balanza con las partidas cuya fecha esta en [desde, hasta] (ambos incluidos). */
    public Balanza balanza(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ReglaNegocioException("El rango de fechas de la balanza es obligatorio");
        }
        if (desde.isAfter(hasta)) {
            throw new ReglaNegocioException("La fecha inicial no puede ser posterior a la final");
        }
        return CalculoBalanza.calcular(desde, hasta, cuentaDAO.findTodas(),
                partidaDetalleDAO.sumasPorCuenta(desde, hasta));
    }
}
