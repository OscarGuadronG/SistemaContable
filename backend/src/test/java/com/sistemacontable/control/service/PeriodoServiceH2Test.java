package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.*;
import com.sistemacontable.JpaTestSupport;
import com.sistemacontable.control.config.ConfigPruebas;
import com.sistemacontable.entity.Periodo;
import com.sistemacontable.entity.enums.TipoPartida;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PeriodoServiceH2Test {
    private EntityManager em;
    private EscenarioContable esc;
    private Cableado cableado;
    private PartidaService partidas;
    private PeriodoService periodos;

    @BeforeEach void abrir(){
        em=JpaTestSupport.nuevoEmConTransaccion();esc=new EscenarioContable(em);
        cableado=new Cableado(em,ConfigPruebas.porDefecto());partidas=cableado.obtener(PartidaService.class);periodos=cableado.obtener(PeriodoService.class);
    }
    @AfterEach void cerrar(){JpaTestSupport.cerrarConRollback(em);}

    @Test void hojaCuadraYCierreGeneraAperturaConUtilidadEnTrimestreSiguiente(){
        aperturaInicial();
        partidas.registrar(TipoPartida.Ingreso,LocalDate.of(2026,7,10),"Ingreso",null,null,null,
                List.of(LineaAsiento.cargo(esc.caja,bd("100")),LineaAsiento.abono(esc.ventas,bd("100"))));
        Map<?,?> hoja=periodos.hojaTrabajo(esc.periodoFiscal2026Q1.getId());
        assertEquals(Map.of("comprobacion",true,"ajustado",true,"balance",true),hoja.get("cuadrada"));
        assertEquals(0,bd("100.00").compareTo((BigDecimal)hoja.get("utilidad")));
        CierrePeriodo cierre=periodos.cerrar(esc.periodoFiscal2026Q1.getId());
        assertEquals(TipoPartida.Cierre,cierre.partidaCierre().getTipo());
        assertNotNull(cierre.partidaApertura());assertEquals(TipoPartida.Apertura,cierre.partidaApertura().getTipo());
        assertEquals(LocalDate.of(2026,10,1),cierre.partidaApertura().getFecha());
        assertEquals(bd("200.00"),cargo(cierre.partidaCierre()));assertEquals(bd("200.00"),abono(cierre.partidaCierre()));
        Periodo siguiente=em.find(Periodo.class,cierre.partidaApertura().getPeriodo().getId());
        assertEquals(cierre.partidaApertura().getId(),siguiente.getPartidaApertura().getId());
        em.flush();assertEquals(0,bd("200.00").compareTo(cargo(cierre.partidaApertura())));
        assertEquals(cargo(cierre.partidaApertura()),abono(cierre.partidaApertura()));
    }

    @Test void cierreYAperturaCuadranConPerdida(){
        aperturaInicial();
        partidas.registrar(TipoPartida.Egreso,LocalDate.of(2026,7,10),"Gasto",null,null,null,
                List.of(LineaAsiento.cargo(esc.otrosGastos,bd("20")),LineaAsiento.abono(esc.caja,bd("20"))));
        Map<?,?> hoja=periodos.hojaTrabajo(esc.periodoFiscal2026Q1.getId());
        assertEquals(0,bd("20.00").compareTo((BigDecimal)hoja.get("perdida")));
        assertEquals(Map.of("comprobacion",true,"ajustado",true,"balance",true),hoja.get("cuadrada"));
        CierrePeriodo cierre=periodos.cerrar(esc.periodoFiscal2026Q1.getId());
        assertNotNull(cierre.partidaApertura());
        em.flush();assertEquals(bd("100.00"),cargo(cierre.partidaApertura()));assertEquals(bd("100.00"),abono(cierre.partidaApertura()));
    }

    @Test void cierreDelCuartoTrimestreCreaLosCuatroPeriodosFiscalesSiguientes(){
        em.createQuery("UPDATE Periodo p SET p.estado=:cerrado WHERE p.anioFiscal<2026 OR (p.anioFiscal=2026 AND p.trimestre<4)")
                .setParameter("cerrado",com.sistemacontable.entity.enums.EstadoPeriodo.Cerrado).executeUpdate();
        Periodo q4=em.createQuery("SELECT p FROM Periodo p WHERE p.anioFiscal=2026 AND p.trimestre=4",Periodo.class).getSingleResult();
        LocalDate inicio=q4.getFechaInicio();
        partidas.registrar(TipoPartida.Apertura,inicio,"Apertura Q4",null,null,null,
                List.of(LineaAsiento.cargo(esc.caja,bd("100")),LineaAsiento.abono(esc.utilidadEjercicio,bd("100"))));
        partidas.registrar(TipoPartida.Ingreso,inicio.plusDays(5),"Ingreso Q4",null,null,null,
                List.of(LineaAsiento.cargo(esc.caja,bd("100")),LineaAsiento.abono(esc.ventas,bd("100"))));

        CierrePeriodo cierre=periodos.cerrar(q4.getId());
        assertNotNull(cierre.partidaApertura());
        assertEquals(LocalDate.of(2027,7,1),cierre.partidaApertura().getFecha());
        assertEquals(bd("200.00"),cargo(cierre.partidaApertura()));
        assertEquals(bd("200.00"),abono(cierre.partidaApertura()));
        List<Periodo> siguiente=em.createQuery("SELECT p FROM Periodo p WHERE p.anioFiscal=2027 ORDER BY p.trimestre",Periodo.class).getResultList();
        assertEquals(4,siguiente.size());
        assertEquals(List.of((short)1,(short)2,(short)3,(short)4),siguiente.stream().map(Periodo::getTrimestre).toList());
        assertEquals(cierre.partidaApertura().getId(),siguiente.get(0).getPartidaApertura().getId());
        assertEquals(LocalDate.of(2027,7,1),siguiente.get(0).getFechaInicio());
        assertEquals(LocalDate.of(2028,6,30),siguiente.get(3).getFechaFin());
        assertThrows(com.sistemacontable.control.exception.ReglaNegocioException.class,()->periodos.cerrar(q4.getId()));
    }

    private void aperturaInicial(){
        em.createQuery("UPDATE Periodo p SET p.estado=:cerrado WHERE p.anioFiscal=2025")
                .setParameter("cerrado",com.sistemacontable.entity.enums.EstadoPeriodo.Cerrado).executeUpdate();
        partidas.registrar(TipoPartida.Apertura,LocalDate.of(2026,7,1),"Apertura inicial",null,null,null,
                List.of(LineaAsiento.cargo(esc.caja,bd("100")),LineaAsiento.abono(esc.utilidadEjercicio,bd("100"))));
    }
    private BigDecimal cargo(com.sistemacontable.entity.Partida p){return lineas(p).stream().map(com.sistemacontable.entity.PartidaDetalle::getCargo).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private BigDecimal abono(com.sistemacontable.entity.Partida p){return lineas(p).stream().map(com.sistemacontable.entity.PartidaDetalle::getAbono).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private List<com.sistemacontable.entity.PartidaDetalle> lineas(com.sistemacontable.entity.Partida p){return em.createQuery("SELECT d FROM PartidaDetalle d WHERE d.partida.id=:id",com.sistemacontable.entity.PartidaDetalle.class).setParameter("id",p.getId()).getResultList();}
    private static BigDecimal bd(String s){return new BigDecimal(s);}
}
