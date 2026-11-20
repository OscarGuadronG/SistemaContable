package com.sistemacontable.control.service;

import com.sistemacontable.control.FinanzasDAO;
import com.sistemacontable.boundary.dto.*;
import com.sistemacontable.control.service.cmd.*;
import com.sistemacontable.boundary.mapper.ContabilidadMapper;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.entity.*;
import com.sistemacontable.entity.enums.*;
import com.sistemacontable.security.UsuarioActual;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Stateless
@LocalBean
public class CreditoService {
    @Inject private FinanzasDAO dao;
    @Inject private ParametroService parametros;
    @Inject private PartidaService partidas;
    @Inject private UsuarioActual usuario;

    public List<CreditoDto> listar(String tipo, String estado, Long terceroId) {
        boolean venta = tipo(tipo);
        autorizar(venta);
        String filtro = estado == null ? "pendiente" : estado;
        if (!Set.of("pendiente", "cancelado", "todos").contains(filtro)) throw new IllegalArgumentException("Estado de crédito inválido");
        List<CreditoDto> resultados = new ArrayList<>();
        if (venta) for (Venta d : dao.ventasCredito()) resultados.add(dto(d, null));
        else for (Compra d : dao.comprasCredito()) resultados.add(dto(d, null));
        return resultados.stream().filter(d -> terceroId == null || terceroId.equals(d.contraparte().id()))
                .filter(d -> filtro.equals("todos") || (filtro.equals("pendiente") ? d.saldo().signum() > 0 : d.saldo().signum() <= 0)).toList();
    }
    public CreditoDto obtener(String tipo, Long id, LocalDate fecha) {
        boolean venta = tipo(tipo); autorizar(venta);
        Object doc = documento(venta, id, false);
        return venta ? dto((Venta)doc, fecha == null ? LocalDate.now() : fecha) : dto((Compra)doc, fecha == null ? LocalDate.now() : fecha);
    }
    public CreditoDto.ResultadoPago pagar(PagoCreditoCmd cmd) {
        if (cmd == null) throw new ReglaNegocioException("El pago es obligatorio");
        boolean venta = tipo(cmd.tipo()); autorizar(venta);
        Object doc = documento(venta, cmd.documentoId(), true);
        CreditoDto credito = venta ? dto((Venta)doc, cmd.fecha()) : dto((Compra)doc, cmd.fecha());
        if (cmd.capital() == null || cmd.capital().signum() <= 0) throw new ReglaNegocioException("El capital debe ser mayor que cero");
        BigDecimal capital = Dinero.redondear(cmd.capital());
        if (capital.signum() <= 0 || capital.compareTo(credito.saldo()) > 0) throw new ReglaNegocioException("El capital excede el saldo pendiente o no alcanza un centavo");
        validarPago(cmd.formaPago(), cmd.numeroCheque());
        BigDecimal interes = InteresCredito.calcular(credito.fecha(), cmd.fecha(), capital, credito.tasaInteres(), parametros.entero("credito.dias_exoneracion")).interes();
        PagoCredito pago = new PagoCredito();
        if (venta) pago.setVenta((Venta)doc); else pago.setCompra((Compra)doc);
        pago.setFecha(cmd.fecha()); pago.setCapital(capital); pago.setInteres(interes);
        pago.setFormaPago(cmd.formaPago()); pago.setNumeroCheque(cmd.numeroCheque()); pago.setDescripcion(cmd.descripcion());
        List<LineaAsiento> lineas = new ArrayList<>();
        Cuenta efectivo = partidas.cuentaConfigurada(cmd.formaPago() == FormaPago.Cheque ? "cuenta.bancos" : "cuenta.caja");
        Cuenta contraparte = partidas.cuentaConfigurada(venta ? "cuenta.clientes" : "cuenta.proveedores");
        if (venta) {
            lineas.add(LineaAsiento.cargo(efectivo, capital.add(interes)));
            lineas.add(LineaAsiento.abono(contraparte, capital));
            if (interes.signum() > 0) lineas.add(LineaAsiento.abono(partidas.cuentaConfigurada("cuenta.productos.financieros"), interes));
        } else {
            lineas.add(LineaAsiento.cargo(contraparte, capital));
            if (interes.signum() > 0) lineas.add(LineaAsiento.cargo(partidas.cuentaConfigurada("cuenta.gastos.financieros"), interes));
            lineas.add(LineaAsiento.abono(efectivo, capital.add(interes)));
        }
        dao.guardar(pago); dao.flush();
        Partida p = partidas.registrar(venta ? TipoPartida.Ingreso : TipoPartida.Egreso, cmd.fecha(),
                "Pago de " + cmd.tipo() + " #" + cmd.documentoId(), venta ? null : (Compra)doc, venta ? (Venta)doc : null, null, lineas);
        p.setIdPagoCredito(pago.getId()); dao.flush();
        return new CreditoDto.ResultadoPago(pagoDto(pago), ContabilidadMapper.partida(p, partidas.lineas(p.getId())));
    }
    private Object documento(boolean venta, Long id, boolean lock) {
        Object doc = venta ? dao.buscar(Venta.class, id, lock) : dao.buscar(Compra.class, id, lock);
        FormaPago forma = venta ? ((Venta)doc).getFormaPago() : ((Compra)doc).getFormaPago();
        if (forma != FormaPago.Credito) throw new ReglaNegocioException("El documento no es al crédito");
        return doc;
    }
    private CreditoDto dto(Venta d, LocalDate fecha) {
        BigDecimal devoluciones = dao.devueltoVenta(d.getId());
        BigDecimal saldoDev = devoluciones.signum() == 0 ? BigDecimal.ZERO
                : dao.reintegroVenta(d.getId(), partidas.cuentaConfigurada("cuenta.clientes").getId());
        return dto("venta", d.getId(), d.getFecha(), d.getNumeroDocumento(), new CreditoDto.Contraparte(d.getCliente().getId(), d.getCliente().getNombre()), d.getTotal(), d.getTasaInteres(), d.getPlazoMeses(), d.getFechaVencimiento(), saldoDev, fecha);
    }
    private CreditoDto dto(Compra d, LocalDate fecha) {
        return dto("compra", d.getId(), d.getFecha(), d.getNumeroDocumento(), new CreditoDto.Contraparte(d.getProveedor().getId(), d.getProveedor().getNombre()), d.getTotal(), d.getTasaInteres(), d.getPlazoMeses(), d.getFechaVencimiento(), dao.compensadoCompra(d.getId()), fecha);
    }
    private CreditoDto dto(String tipo, Long id, LocalDate origen, String numero, CreditoDto.Contraparte contraparte,
            BigDecimal total, BigDecimal tasa, Integer plazo, LocalDate vencimiento, BigDecimal devuelto, LocalDate fecha) {
        List<PagoCredito> pagos = dao.pagos(tipo.equals("venta"), id);
        BigDecimal pagado = Dinero.redondear(pagos.stream().map(PagoCredito::getCapital).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal saldo = Dinero.redondear(total.subtract(pagado).subtract(devuelto));
        return new CreditoDto(tipo, id, origen, numero, contraparte, total, pagado, saldo, tasa, plazo, vencimiento,
                saldo.signum() > 0 && vencimiento != null && vencimiento.isBefore(LocalDate.now()),
                fecha == null ? null : pagos.stream().map(CreditoService::pagoDto).toList(),
                fecha == null ? null : InteresCredito.calcular(origen, fecha, saldo.max(BigDecimal.ZERO), tasa, parametros.entero("credito.dias_exoneracion")));
    }
    public static CreditoDto.Pago pagoDto(PagoCredito p) {
        return new CreditoDto.Pago(p.getId(), p.getFecha(), p.getCapital(), p.getInteres(), p.getFormaPago(), p.getNumeroCheque(), p.getDescripcion());
    }
    public static boolean tipo(String tipo) {
        if (!"venta".equals(tipo) && !"compra".equals(tipo)) throw new IllegalArgumentException("El tipo debe ser venta o compra");
        return tipo.equals("venta");
    }
    public static void validarPago(FormaPago forma, String cheque) {
        if (forma == null || forma == FormaPago.Credito) throw new ReglaNegocioException("El pago debe ser Contado o Cheque");
        if (forma == FormaPago.Cheque && (cheque == null || cheque.isBlank())) throw new ReglaNegocioException("El número de cheque es obligatorio");
    }
    private void autorizar(boolean venta) {
        // La autorización por tipo se realiza también en el servicio para impedir pagos de compras desde Ventas.
        usuario.exigirCredito(venta);
    }
}
