package com.sistemacontable.control.service;

import static org.junit.jupiter.api.Assertions.*;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.enums.FormaPago;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AsientosTest {
    private Cuenta cuenta(long id) { Cuenta c = new Cuenta(); c.setId(id); return c; }
    private BigDecimal bd(String s) { return new BigDecimal(s); }
    @Test void compraChequeConsideraDescuentoYGastos() {
        var lines = Asientos.compraCompleta(cuenta(1), cuenta(2), cuenta(3), null, cuenta(4), null,
                FormaPago.Cheque, bd("100.00"), bd("10.00"), bd("20.00"), bd("11.70"), bd("121.70"));
        assertEquals(4, lines.size());
        assertEquals(bd("120.00"), lines.get(0).cargo());
        assertEquals(bd("10.00"), lines.get(2).abono());
        assertEquals(bd("121.70"), lines.get(3).abono());
    }
    @Test void ventaCreditoIncluyeRebajaCostoEInventario() {
        var lines = Asientos.ventaCompleta(null, null, cuenta(1), cuenta(2), cuenta(3), cuenta(4), cuenta(5), cuenta(6),
                FormaPago.Credito, bd("100.00"), bd("5.00"), bd("12.35"), bd("107.35"), bd("60.00"));
        assertEquals(6, lines.size());
        assertEquals(bd("107.35"), lines.get(0).cargo());
        assertEquals(bd("5.00"), lines.get(1).cargo());
        assertEquals(bd("60.00"), lines.get(4).cargo());
    }
}
