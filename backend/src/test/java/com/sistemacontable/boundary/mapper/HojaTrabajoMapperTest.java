package com.sistemacontable.boundary.mapper;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.Test;
class HojaTrabajoMapperTest {
    @Test void mismaRespuestaParaCalculoEnMemoriaEInstantaneaGuardada() throws Exception {
        var columna=Map.of("debe",new BigDecimal("100.00"),"haber",new BigDecimal("100.00"));
        Map<String,Object> hoja=Map.of("periodo",Map.of("id",1L,"anioFiscal",2026,"trimestre",1,"fechaInicio",LocalDate.of(2026,7,1),"fechaFin",LocalDate.of(2026,9,30),"estado","Cerrado"),
                "desde",LocalDate.of(2026,7,1),"hasta",LocalDate.of(2026,9,30),
                "filas",List.of(Map.of("cuentaId",1L,"codigo",1101,"nombre","Caja","clase",1,"comprobacion",columna,"ajustes",columna,"ajustado",columna,"resultados",columna,"balance",columna)),
                "totales",Map.of("comprobacion",columna,"ajustes",columna,"ajustado",columna,"resultados",columna,"balance",columna),
                "utilidad",BigDecimal.ZERO,"perdida",BigDecimal.ZERO,"cuadrada",Map.of("comprobacion",true,"ajustado",true,"balance",true));
        try(var json=JsonbBuilder.create()) {
            var vivo=HojaTrabajoMapper.hoja(hoja);
            var guardado=HojaTrabajoMapper.hoja(json.fromJson(json.toJson(hoja),Map.class));
            assertEquals(json.toJson(vivo),json.toJson(guardado));
            assertEquals(LocalDate.of(2026,7,1),guardado.desde());
            assertEquals(1101,guardado.filas().getFirst().codigo());
            assertTrue(guardado.cuadrada().balance());
        }
    }
}
