package com.sistemacontable.control.service;

import com.sistemacontable.control.ParametroDAO;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Parametro;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Stateless @LocalBean
public class ParametroService {
    private static final Map<String, String[]> DEFAULTS = defaults();
    @Inject private ParametroDAO dao;

    private static Map<String, String[]> defaults() {
        Map<String, String[]> m = new LinkedHashMap<>();
        m.put("credito.tasa_mensual", new String[]{"0.02", "Interés mensual de documentos al crédito"});
        m.put("credito.plazo_meses", new String[]{"3", "Plazo de crédito en meses"});
        m.put("credito.dias_exoneracion", new String[]{"30", "Días sin interés"});
        m.put("venta.margen", new String[]{"0.30", "Margen sobre costo promedio"});
        m.put("iva.tasa", new String[]{"0.13", "Tasa de IVA"});
        m.put("devolucion_compra.dias_max", new String[]{"30", "Plazo máximo para devolución de compra"});
        m.put("devolucion_venta.dias_max", new String[]{"30", "Plazo máximo para devolución de venta"});
        m.put("ciclo.mes_inicio", new String[]{"7", "Mes de inicio del ciclo contable"});
        return Map.copyOf(m);
    }
    public List<Parametro> listar() {
        Map<String, Parametro> existentes = new LinkedHashMap<>();
        dao.listar().forEach(p -> existentes.put(p.getClave(), p));
        List<Parametro> resultado = new ArrayList<>();
        DEFAULTS.keySet().stream().sorted().forEach(clave -> {
            String[] def = DEFAULTS.get(clave);
            Parametro p = existentes.get(clave);
            if (p == null) { p = new Parametro(); p.setClave(clave); p.setValor(def[0]); p.setDescripcion(def[1]); }
            resultado.add(p);
        });
        existentes.values().stream().filter(p -> !DEFAULTS.containsKey(p.getClave())).forEach(resultado::add);
        return resultado;
    }
    public Parametro actualizar(String clave, String valor) {
        if (!DEFAULTS.containsKey(clave)) throw new ReglaNegocioException("La clave de parámetro no está permitida: " + clave);
        if (valor == null || valor.isBlank()) throw new ReglaNegocioException("El valor del parámetro es obligatorio");
        validar(clave, valor);
        Parametro p = dao.buscar(clave).orElseGet(() -> { Parametro n = new Parametro(); n.setClave(clave); n.setDescripcion(DEFAULTS.get(clave)[1]); return n; });
        p.setValor(valor.trim());
        if (dao.buscar(clave).isPresent()) dao.modificar(p); else dao.crear(p);
        return p;
    }
    public BigDecimal decimal(String clave) { return new BigDecimal(valor(clave)); }
    public int entero(String clave) { return Integer.parseInt(valor(clave)); }
    public String valor(String clave) { return dao.buscar(clave).map(Parametro::getValor).orElseGet(() -> {
        String[] d = DEFAULTS.get(clave); if (d == null) throw new ReglaNegocioException("Parámetro desconocido: " + clave); return d[0];
    }); }
    private static void validar(String clave, String valor) {
        try {
            BigDecimal n = new BigDecimal(valor);
            if (n.signum() < 0) throw new NumberFormatException();
            if ((clave.equals("ciclo.mes_inicio") || clave.endsWith("dias_max") || clave.equals("credito.plazo_meses") || clave.equals("credito.dias_exoneracion")) && n.stripTrailingZeros().scale() > 0) throw new NumberFormatException();
            if (clave.equals("ciclo.mes_inicio") && (n.intValueExact() < 1 || n.intValueExact() > 12)) throw new NumberFormatException();
            if ((clave.endsWith("dias_max") || clave.equals("credito.dias_exoneracion")) && n.intValueExact() < 0) throw new NumberFormatException();
            if (clave.equals("credito.plazo_meses") && n.intValueExact() < 1) throw new NumberFormatException();
        } catch (NumberFormatException | ArithmeticException e) { throw new ReglaNegocioException("El valor del parámetro " + clave + " no es válido"); }
    }
}
