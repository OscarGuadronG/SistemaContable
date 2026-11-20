package com.sistemacontable.control.service;

import com.sistemacontable.control.CuentaDAO;
import com.sistemacontable.control.PartidaDetalleDAO;
import com.sistemacontable.control.PeriodoDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Partida;
import com.sistemacontable.entity.Periodo;
import com.sistemacontable.entity.enums.EstadoPeriodo;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/** Gestión de trimestres fiscales, hoja de trabajo y asientos de cierre/apertura. */
@Stateless @LocalBean
public class PeriodoService {
    @Inject private PeriodoDAO periodoDAO;
    @Inject private CuentaDAO cuentaDAO;
    @Inject private PartidaDetalleDAO detalleDAO;
    @Inject private PartidaService partidaService;

    public List<Periodo> listar() { return periodoDAO.findAllOrdenados(); }
    public Periodo buscar(Long id) { return periodoDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Periodo", id)); }
    public Periodo actual(LocalDate fecha) { return periodoDAO.findByFecha(fecha).orElseThrow(() -> new NoEncontradoException("Periodo actual", fecha)); }

    public Map<String,Object> hojaTrabajo(Long periodoId) {
        Periodo p = buscar(periodoId);
        if (p.getEstado() == EstadoPeriodo.Cerrado && p.getHojaTrabajo() != null) {
            Jsonb jsonb = JsonbBuilder.create();
            return jsonb.fromJson(p.getHojaTrabajo(), Map.class);
        }
        return calcularHoja(p.getFechaInicio(), p.getFechaFin(), p);
    }

    public Map<String,Object> hojaTrabajo(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return calcularHoja(desde, hasta, null);
    }

    private Map<String,Object> calcularHoja(LocalDate desde, LocalDate hasta, Periodo periodo) {
        Map<Long,PartidaDetalleDAO.SumaCuenta> base = index(detalleDAO.sumasComprobacion(desde, hasta));
        Map<Long,PartidaDetalleDAO.SumaCuenta> ajustes = index(detalleDAO.sumasPorCuenta(desde, hasta, TipoPartida.Ajuste));
        Map<Long,Cuenta> cuentas = new LinkedHashMap<>();
        cuentaDAO.findTodas().forEach(c -> cuentas.put(c.getId(), c));
        List<Map<String,Object>> filas = new ArrayList<>();
        BigDecimal[] totComp = pair(), totAdj = pair(), totAjust = pair(), totRes = pair(), totBal = pair();
        BigDecimal utilidad = BigDecimal.ZERO, perdida = BigDecimal.ZERO;
        for (Cuenta c : cuentas.values()) {
            PartidaDetalleDAO.SumaCuenta b = base.get(c.getId()), a = ajustes.get(c.getId());
            BigDecimal bd = val(b == null ? null : b.totalCargo()), bh = val(b == null ? null : b.totalAbono());
            BigDecimal ad = val(a == null ? null : a.totalCargo()), ah = val(a == null ? null : a.totalAbono());
            if (bd.signum() + bh.signum() + ad.signum() + ah.signum() == 0) continue;
            BigDecimal netComprobacion = bd.subtract(bh);
            BigDecimal net = netComprobacion.add(ad).subtract(ah);
            BigDecimal compD = netComprobacion.max(BigDecimal.ZERO), compH = netComprobacion.negate().max(BigDecimal.ZERO);
            BigDecimal ajD = net.max(BigDecimal.ZERO), ajH = net.negate().max(BigDecimal.ZERO);
            int clase = String.valueOf(c.getCodigo()).charAt(0) - '0';
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("cuentaId", c.getId()); row.put("codigo", c.getCodigo()); row.put("nombre", c.getNombre()); row.put("clase", clase);
            row.put("comprobacion", col(compD, compH)); row.put("ajustes", col(ad, ah)); row.put("ajustado", col(ajD, ajH));
            row.put("resultados", clase == 4 || clase == 5 ? col(ajD, ajH) : col(BigDecimal.ZERO, BigDecimal.ZERO));
            row.put("balance", clase >= 1 && clase <= 3 ? col(ajD, ajH) : col(BigDecimal.ZERO, BigDecimal.ZERO));
            filas.add(row); add(totComp, compD, compH); add(totAdj, ad, ah); add(totAjust, ajD, ajH);
            if (clase == 4 || clase == 5) { add(totRes, ajD, ajH); utilidad = utilidad.add(clase == 5 ? ajH.subtract(ajD) : BigDecimal.ZERO); perdida = perdida.add(clase == 4 ? ajD.subtract(ajH) : BigDecimal.ZERO); }
            if (clase >= 1 && clase <= 3) add(totBal, ajD, ajH);
        }
        BigDecimal resultadoNeto = utilidad.subtract(perdida);
        utilidad = resultadoNeto.max(BigDecimal.ZERO); perdida = resultadoNeto.negate().max(BigDecimal.ZERO);
        boolean cuadComp = eq(totComp), cuadAjust = eq(totAjust);
        if (utilidad.signum() > 0) { totRes[0]=totRes[0].add(utilidad); totBal[1] = totBal[1].add(utilidad); }
        if (perdida.signum() > 0) { totRes[1]=totRes[1].add(perdida); totBal[0] = totBal[0].add(perdida); }
        Map<String,Object> out = new LinkedHashMap<>(); out.put("periodo", periodo == null ? null : new LinkedHashMap<>(Map.of("id",periodo.getId(),"anioFiscal",periodo.getAnioFiscal(),"trimestre",periodo.getTrimestre(),"fechaInicio",periodo.getFechaInicio(),"fechaFin",periodo.getFechaFin(),"estado",periodo.getEstado()))); out.put("desde", desde); out.put("hasta", hasta); out.put("filas", filas);
        Map<String,Object> totales = new LinkedHashMap<>(); totales.put("comprobacion", col(totComp[0],totComp[1])); totales.put("ajustes", col(totAdj[0],totAdj[1]));
        totales.put("ajustado", col(totAjust[0],totAjust[1])); totales.put("resultados", col(totRes[0],totRes[1])); totales.put("balance", col(totBal[0],totBal[1])); out.put("totales",totales);
        out.put("utilidad", Dinero.redondear(utilidad)); out.put("perdida", Dinero.redondear(perdida));
        out.put("cuadrada", Map.of("comprobacion",cuadComp,"ajustado",cuadAjust,"balance",eq(totBal)));
        return out;
    }

    public CierrePeriodo cerrar(Long id) {
        Periodo p = periodoDAO.buscarParaActualizar(id).orElseThrow(() -> new NoEncontradoException("Periodo", id));
        if (p.getEstado() != EstadoPeriodo.Abierto) throw new ReglaNegocioException("El periodo ya está cerrado");
        if (periodoDAO.existenAnterioresAbiertos(p)) throw new ReglaNegocioException("Cierre primero los periodos anteriores");
        Map<String,Object> hoja = calcularHoja(p.getFechaInicio(),p.getFechaFin(),p);
        Map<?,?> q = (Map<?,?>) hoja.get("cuadrada");
        if (!Boolean.TRUE.equals(q.get("comprobacion")) || !Boolean.TRUE.equals(q.get("ajustado")) || !Boolean.TRUE.equals(q.get("balance")))
            throw new ReglaNegocioException("La hoja de trabajo debe estar cuadrada antes del cierre");
        Map<Long,PartidaDetalleDAO.SumaCuenta> sums = index(detalleDAO.sumasPorCuenta(p.getFechaInicio(),p.getFechaFin()));
        Map<Long,Cuenta> cuentas = new HashMap<>(); cuentaDAO.findTodas().forEach(c -> cuentas.put(c.getId(),c));
        Cuenta pyG = partidaService.cuentaConfigurada("cuenta.perdidas.ganancias");
        Cuenta utilidadEj = partidaService.cuentaConfigurada("cuenta.utilidad.ejercicio");
        Cuenta perdidaEj = partidaService.cuentaConfigurada("cuenta.perdida.ejercicio");
        List<LineaAsiento> cierre = new ArrayList<>(); BigDecimal saldoPyG = BigDecimal.ZERO;
        for (var entry : sums.entrySet()) {
            Cuenta c = cuentas.get(entry.getKey()); int clase = String.valueOf(c.getCodigo()).charAt(0)-'0';
            if (clase != 4 && clase != 5) continue;
            BigDecimal net = val(entry.getValue().totalCargo()).subtract(val(entry.getValue().totalAbono()));
            if (net.signum() == 0) continue;
            cierre.add(new LineaAsiento(c, net.signum() < 0 ? net.abs() : BigDecimal.ZERO, net.signum() > 0 ? net : BigDecimal.ZERO));
            saldoPyG = saldoPyG.add(net);
        }
        if (cierre.isEmpty()) throw new ReglaNegocioException("No hay movimientos de resultados para cerrar en el periodo");
        if (saldoPyG.signum() > 0) cierre.add(new LineaAsiento(pyG, saldoPyG, BigDecimal.ZERO));
        else if (saldoPyG.signum() < 0) cierre.add(new LineaAsiento(pyG, BigDecimal.ZERO, saldoPyG.abs()));
        BigDecimal saldoPyGAbsoluto = saldoPyG.abs();
        if (saldoPyG.signum() > 0) {
            cierre.add(new LineaAsiento(pyG, BigDecimal.ZERO, saldoPyGAbsoluto));
            cierre.add(new LineaAsiento(perdidaEj, saldoPyGAbsoluto, BigDecimal.ZERO));
        } else if (saldoPyG.signum() < 0) {
            cierre.add(new LineaAsiento(pyG, saldoPyGAbsoluto, BigDecimal.ZERO));
            cierre.add(new LineaAsiento(utilidadEj, BigDecimal.ZERO, saldoPyGAbsoluto));
        }
        Partida pc = partidaService.registrar(TipoPartida.Cierre,p.getFechaFin(),"Cierre contable " + p.getFechaFin(),null,null,null,cierre);
        Periodo siguiente = p.getFechaFin().getMonthValue()==6 ? crearSiguiente(p)
                : periodoDAO.findSiguiente(p.getFechaFin()).orElseGet(() -> crearSiguiente(p));
        List<LineaAsiento> apertura = new ArrayList<>();
        for (var entry : sums.entrySet()) { Cuenta c=cuentas.get(entry.getKey()); int clase=String.valueOf(c.getCodigo()).charAt(0)-'0'; if(clase<1||clase>3) continue;
            BigDecimal net=val(entry.getValue().totalCargo()).subtract(val(entry.getValue().totalAbono())); if(net.signum()!=0) apertura.add(new LineaAsiento(c,net.max(BigDecimal.ZERO),net.negate().max(BigDecimal.ZERO))); }
        if(saldoPyG.signum()>0) apertura.add(new LineaAsiento(perdidaEj,saldoPyG,BigDecimal.ZERO));
        else if(saldoPyG.signum()<0) apertura.add(new LineaAsiento(utilidadEj,BigDecimal.ZERO,saldoPyG.abs()));
        Partida pa = apertura.isEmpty() ? null : partidaService.registrar(TipoPartida.Apertura,siguiente.getFechaInicio(),"Apertura contable " + siguiente.getFechaInicio(),null,null,null,apertura);
        if (pa != null) siguiente.setPartidaApertura(pa);
        p.setEstado(EstadoPeriodo.Cerrado); p.setFechaCierre(Instant.now()); p.setPartidaCierre(pc);
        ((Map<String,Object>)hoja.get("periodo")).put("estado",EstadoPeriodo.Cerrado);
        p.setHojaTrabajo(json(hoja)); p.setPartidaCierre(pc);
        return new CierrePeriodo(p,pc,pa);
    }

    private Periodo crearSiguiente(Periodo p) {
        LocalDate inicio=p.getFechaFin().plusDays(1);
        if (inicio.getMonthValue()==7) {
            for(int i=0;i<4;i++) { LocalDate fecha=inicio.plusMonths(3L*i); if(periodoDAO.findByFecha(fecha).isEmpty()) crear(fecha); }
            return periodoDAO.findByFecha(inicio).orElseThrow();
        }
        return periodoDAO.findByFecha(inicio).orElseGet(() -> crear(inicio));
    }
    private Periodo crear(LocalDate inicio) { Periodo p=new Periodo(); p.setFechaInicio(inicio); p.setFechaFin(inicio.plusMonths(3).minusDays(1)); p.setEstado(EstadoPeriodo.Abierto);
        int quarter = switch (inicio.getMonthValue()) { case 7 -> 1; case 10 -> 2; case 1 -> 3; case 4 -> 4; default -> throw new ReglaNegocioException("El siguiente periodo debe iniciar un trimestre fiscal"); };
        p.setTrimestre((short) quarter); p.setAnioFiscal(inicio.getMonthValue() >= 7 ? inicio.getYear() : inicio.getYear()-1);
        return periodoDAO.crear(p); }
    private static String json(Object o) { return JsonbBuilder.create().toJson(o); }
    private static void validarRango(LocalDate d,LocalDate h) { if(d==null||h==null||d.isAfter(h)) throw new ReglaNegocioException("El rango de fechas es obligatorio y válido"); }
    private static Map<Long,PartidaDetalleDAO.SumaCuenta> index(List<PartidaDetalleDAO.SumaCuenta> in) { Map<Long,PartidaDetalleDAO.SumaCuenta> m=new HashMap<>(); in.forEach(s->m.put(s.cuentaId(),s)); return m; }
    private static BigDecimal val(BigDecimal b) { return b==null?BigDecimal.ZERO:b; }
    private static BigDecimal[] pair() { return new BigDecimal[]{BigDecimal.ZERO,BigDecimal.ZERO}; }
    private static Map<String,BigDecimal> col(BigDecimal d,BigDecimal h) { return Map.of("debe",Dinero.redondear(d),"haber",Dinero.redondear(h)); }
    private static void add(BigDecimal[] p,BigDecimal d,BigDecimal h) { p[0]=p[0].add(d);p[1]=p[1].add(h); }
    private static boolean eq(BigDecimal[] p) { return Dinero.redondear(p[0]).compareTo(Dinero.redondear(p[1]))==0; }
}
