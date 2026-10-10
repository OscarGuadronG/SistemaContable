package com.sistemacontable.control.service;

import com.sistemacontable.control.CuentaDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.CuentaCmd;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;

/**
 * Catalogo de cuentas contables: alta, modificacion y baja logica. No hay borrado fisico.
 *
 * <p>Reglas: {@code codigo} unico; el codigo de una hija empieza por el del padre (y es mas largo); la hija hereda la
 * naturaleza del padre (indicar otra es error); una cuenta con hijas no puede ser imputable, por eso tampoco se
 * puede colgar una hija de una cuenta imputable (hay que volverla no imputable antes); sin ciclos en la jerarquia;
 * el padre debe estar activo al asignarlo; no se desactiva con hijas activas ni con movimientos.</p>
 *
 * <p>Decisiones conservadoras (no estaban explicitas): la naturaleza de una cuenta con hijas o con movimientos no se
 * puede cambiar (alteraria el significado de los saldos); al cambiar el codigo o el padre, el codigo nuevo debe
 * seguir siendo compatible con el padre y con las hijas; reactivar una cuenta exige padre activo. Los errores de
 * validacion son {@link ReglaNegocioException}.</p>
 */
@Stateless
@LocalBean
public class CuentaService {

    @Inject
    private CuentaDAO cuentaDAO;

    /** Da de alta una cuenta (activa). */
    public Cuenta crear(CuentaCmd cmd) {
        requerirDatos(cmd);
        validarCodigoUnico(cmd.codigo(), null);
        Cuenta padre = cmd.padreId() == null ? null : buscarPorId(cmd.padreId());
        NaturalezaCuenta naturaleza = cmd.naturaleza();
        if (padre != null) {
            validarComoPadre(padre);
            if (!ReglasCuenta.codigoCompatibleConPadre(cmd.codigo(), padre)) {
                throw codigoIncompatible(cmd.codigo(), padre.getCodigo());
            }
            naturaleza = naturalezaHeredada(padre, cmd.naturaleza());
        } else if (naturaleza == null) {
            throw new ReglaNegocioException("La naturaleza de la cuenta es obligatoria cuando no tiene cuenta padre");
        }
        Cuenta cuenta = new Cuenta();
        cuenta.setCodigo(cmd.codigo());
        cuenta.setNombre(cmd.nombre().trim());
        cuenta.setNaturaleza(naturaleza);
        cuenta.setActivo(Boolean.TRUE);
        cuenta.setImputable(Boolean.TRUE.equals(cmd.imputable()));
        cuenta.setPadre(padre);
        cuentaDAO.crear(cuenta);
        cuentaDAO.flush();
        return cuenta;
    }

    /** Modifica nombre, codigo, naturaleza, imputable y padre de una cuenta (no su estado activo). */
    public Cuenta modificar(Long id, CuentaCmd cmd) {
        requerirDatos(cmd);
        Cuenta cuenta = buscarPorId(id);
        validarCodigoUnico(cmd.codigo(), cuenta.getId());

        Cuenta padre = cmd.padreId() == null ? null : buscarPorId(cmd.padreId());
        boolean padreCambia = !Objects.equals(cuenta.getPadre() == null ? null : cuenta.getPadre().getId(),
                cmd.padreId());
        if (padreCambia && padre != null) {
            if (Jerarquia.hayCiclo(cuenta, padre, Cuenta::getPadre)) {
                throw new ReglaNegocioException("La cuenta " + cuenta.getCodigo()
                        + " no puede colgar de la cuenta " + padre.getCodigo() + ": se formaria un ciclo");
            }
            validarComoPadre(padre);
        }
        boolean codigoCambia = !Objects.equals(cuenta.getCodigo(), cmd.codigo());
        if (padre != null && (padreCambia || codigoCambia)
                && !ReglasCuenta.codigoCompatibleConPadre(cmd.codigo(), padre)) {
            throw codigoIncompatible(cmd.codigo(), padre.getCodigo());
        }

        NaturalezaCuenta naturaleza = padre != null ? naturalezaHeredada(padre, cmd.naturaleza())
                : (cmd.naturaleza() != null ? cmd.naturaleza() : cuenta.getNaturaleza());
        List<Cuenta> hijos = cuentaDAO.findHijos(cuenta);
        if (naturaleza != cuenta.getNaturaleza() && (!hijos.isEmpty() || cuentaDAO.tieneMovimientos(cuenta))) {
            throw new ReglaNegocioException("No se puede cambiar la naturaleza de la cuenta " + cuenta.getCodigo()
                    + " porque tiene cuentas hijas o movimientos");
        }
        boolean imputable = cmd.imputable() != null ? cmd.imputable() : Boolean.TRUE.equals(cuenta.getImputable());
        if (imputable && !hijos.isEmpty()) {
            throw new ReglaNegocioException("La cuenta " + cuenta.getCodigo()
                    + " tiene cuentas hijas y no puede ser imputable");
        }
        if (codigoCambia) {
            for (Cuenta hija : hijos) {
                if (!ReglasCuenta.codigoCompatible(hija.getCodigo(), cmd.codigo())) {
                    throw new ReglaNegocioException("El nuevo codigo " + cmd.codigo() + " dejaria incompatible a la hija "
                            + hija.getCodigo() + ": su codigo debe empezar por el del padre");
                }
            }
        }

        cuenta.setCodigo(cmd.codigo());
        cuenta.setNombre(cmd.nombre().trim());
        cuenta.setNaturaleza(naturaleza);
        cuenta.setImputable(imputable);
        cuenta.setPadre(padre);
        cuentaDAO.modificar(cuenta);
        cuentaDAO.flush();
        return cuenta;
    }

    /** Baja logica: no se permite con hijas activas ni con movimientos. Idempotente si ya esta inactiva. */
    public Cuenta desactivar(Long id) {
        Cuenta cuenta = buscarPorId(id);
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            return cuenta;
        }
        if (cuentaDAO.existenHijosActivos(cuenta)) {
            throw new ReglaNegocioException("No se puede desactivar la cuenta " + cuenta.getCodigo()
                    + " porque tiene cuentas hijas activas");
        }
        if (cuentaDAO.tieneMovimientos(cuenta)) {
            throw new ReglaNegocioException("No se puede desactivar la cuenta " + cuenta.getCodigo()
                    + " porque tiene movimientos");
        }
        cuenta.setActivo(Boolean.FALSE);
        cuentaDAO.modificar(cuenta);
        return cuenta;
    }

    /** Reactiva una cuenta; su padre (si lo tiene) debe estar activo. */
    public Cuenta activar(Long id) {
        Cuenta cuenta = buscarPorId(id);
        if (Boolean.TRUE.equals(cuenta.getActivo())) {
            return cuenta;
        }
        Cuenta padre = cuenta.getPadre();
        if (padre != null && !Boolean.TRUE.equals(padre.getActivo())) {
            throw new ReglaNegocioException("No se puede activar la cuenta " + cuenta.getCodigo()
                    + " porque su cuenta padre " + padre.getCodigo() + " esta inactiva");
        }
        cuenta.setActivo(Boolean.TRUE);
        cuentaDAO.modificar(cuenta);
        return cuenta;
    }

    public Cuenta buscarPorId(Long id) {
        return cuentaDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Cuenta", id));
    }

    public Cuenta buscarPorCodigo(Integer codigo) {
        return cuentaDAO.findByCodigo(codigo).orElseThrow(() -> new NoEncontradoException("Cuenta con codigo", codigo));
    }

    /** Hijas directas de una cuenta, ordenadas por codigo. */
    public List<Cuenta> hijos(Long id) {
        return cuentaDAO.findHijos(buscarPorId(id));
    }

    public List<Cuenta> listar(int first, int max) {
        return cuentaDAO.findRange(first, max);
    }

    /** Total de registros (cabecera Total-Records de la paginacion REST). */
    public long contar() {
        return cuentaDAO.count();
    }

    /** Todo el catalogo ordenado por codigo. */
    public List<Cuenta> todas() {
        return cuentaDAO.findTodas();
    }

    // ------------------------------------------------------------------------------------- privados

    private static void requerirDatos(CuentaCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos de la cuenta son obligatorios");
        }
        if (cmd.codigo() == null) {
            throw new ReglaNegocioException("El codigo de la cuenta es obligatorio");
        }
        if (cmd.nombre() == null || cmd.nombre().isBlank()) {
            throw new ReglaNegocioException("El nombre de la cuenta es obligatorio");
        }
    }

    private void validarCodigoUnico(Integer codigo, Long idPropio) {
        cuentaDAO.findByCodigo(codigo).ifPresent(existente -> {
            if (!existente.getId().equals(idPropio)) {
                throw new ReglaNegocioException("Ya existe una cuenta con el codigo " + codigo);
            }
        });
    }

    /** El padre debe estar activo y no ser imputable (una cuenta con hijas es de agrupacion). */
    private static void validarComoPadre(Cuenta padre) {
        if (!Boolean.TRUE.equals(padre.getActivo())) {
            throw new ReglaNegocioException("La cuenta padre " + padre.getCodigo() + " esta inactiva");
        }
        if (Boolean.TRUE.equals(padre.getImputable())) {
            throw new ReglaNegocioException("La cuenta padre " + padre.getCodigo()
                    + " es imputable y no puede tener cuentas hijas; conviertala en no imputable primero");
        }
    }

    private static NaturalezaCuenta naturalezaHeredada(Cuenta padre, NaturalezaCuenta indicada) {
        if (indicada != null && indicada != padre.getNaturaleza()) {
            throw new ReglaNegocioException("La naturaleza de una cuenta hija debe ser la de su padre ("
                    + padre.getNaturaleza() + "), no " + indicada);
        }
        return padre.getNaturaleza();
    }

    private static ReglaNegocioException codigoIncompatible(Integer codigo, Integer codigoPadre) {
        return new ReglaNegocioException("El codigo " + codigo + " debe empezar por el codigo del padre "
                + codigoPadre + " y ser mas largo");
    }
}
