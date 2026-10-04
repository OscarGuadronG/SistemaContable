package com.sistemacontable.control.service;

import com.sistemacontable.control.AlmacenDAO;
import com.sistemacontable.control.LoteDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.LineaTrasladoCmd;
import com.sistemacontable.control.service.cmd.TrasladoCmd;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.enums.TipoMovimiento;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traslado de stock entre dos almacenes (distintos y habilitados) en una sola transaccion JTA. No genera partida.
 *
 * <p>Primero valida y comprueba el stock acumulado del origen para todas las lineas (si falla no se escribe nada);
 * luego, por cada linea, resta en el origen y suma en el destino, y registra UN {@code kardex} {@code Traslado}
 * (origen, destino) con una fila por linea. El {@code precio_costo} del kardex es informativo: el costo promedio
 * ponderado del lote, o cero si el lote nunca se compro.</p>
 */
@Stateless
@LocalBean
public class TrasladoService {

    @Inject
    private AlmacenDAO almacenDAO;
    @Inject
    private LoteDAO loteDAO;
    @Inject
    private ExistenciasService existenciasService;
    @Inject
    private KardexService kardexService;

    public MovimientoKardex registrar(TrasladoCmd cmd) {
        // ---- Fase 1: validar sin escribir nada
        if (cmd == null) {
            throw new ReglaNegocioException("El traslado es obligatorio");
        }
        if (cmd.almacenOrigenId() == null || cmd.almacenDestinoId() == null) {
            throw new ReglaNegocioException("El traslado debe indicar almacen de origen y de destino");
        }
        if (cmd.almacenOrigenId().equals(cmd.almacenDestinoId())) {
            throw new ReglaNegocioException("El almacen de origen y el de destino deben ser distintos");
        }
        if (cmd.lineas().isEmpty()) {
            throw new ReglaNegocioException("El traslado debe tener al menos una linea");
        }
        Almacen origen = habilitado(cmd.almacenOrigenId(), "de origen");
        Almacen destino = habilitado(cmd.almacenDestinoId(), "de destino");

        List<Lote> lotes = new ArrayList<>();
        List<BigDecimal> cantidades = new ArrayList<>();
        Map<ClaveStock, BigDecimal> requeridos = new LinkedHashMap<>();
        int numero = 0;
        for (LineaTrasladoCmd l : cmd.lineas()) {
            numero++;
            if (l == null || l.loteId() == null) {
                throw new ReglaNegocioException("La linea " + numero + " del traslado debe indicar el lote");
            }
            if (l.cantidad() == null || l.cantidad().signum() <= 0) {
                throw new ReglaNegocioException("La linea " + numero + " del traslado debe tener cantidad mayor que cero");
            }
            Lote lote = loteDAO.buscarPorId(l.loteId()).orElseThrow(() -> new NoEncontradoException("Lote", l.loteId()));
            lotes.add(lote);
            cantidades.add(l.cantidad());
            requeridos.merge(new ClaveStock(lote.getId(), origen.getId()), l.cantidad(), BigDecimal::add);
        }
        existenciasService.verificarDisponible(requeridos);

        List<DetalleKardex> detalles = new ArrayList<>();
        for (int i = 0; i < lotes.size(); i++) {
            BigDecimal costo = existenciasService.costoPromedio(lotes.get(i).getId()).orElse(BigDecimal.ZERO);
            detalles.add(new DetalleKardex(lotes.get(i), cantidades.get(i), costo));
        }

        // ---- Fase 2: escribir
        for (int i = 0; i < lotes.size(); i++) {
            existenciasService.disminuir(lotes.get(i), origen, cantidades.get(i));
            existenciasService.aumentar(lotes.get(i), destino, cantidades.get(i));
        }
        String descripcion = cmd.descripcion() != null && !cmd.descripcion().isBlank() ? cmd.descripcion().trim()
                : "Traslado entre almacenes";
        return kardexService.registrar(TipoMovimiento.Traslado, origen, destino, null, null, descripcion, detalles);
    }

    private Almacen habilitado(Long id, String rol) {
        Almacen almacen = almacenDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Almacen", id));
        if (!Boolean.TRUE.equals(almacen.getEstado())) {
            throw new ReglaNegocioException("El almacen " + rol + " " + id + " esta deshabilitado");
        }
        return almacen;
    }
}
