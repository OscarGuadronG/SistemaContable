package com.sistemacontable.control.service;

import com.sistemacontable.control.ClienteDAO;
import com.sistemacontable.control.VentaDAO;
import com.sistemacontable.control.exception.NoEncontradoException;
import com.sistemacontable.control.exception.ReglaNegocioException;
import com.sistemacontable.control.service.cmd.ClienteCmd;
import com.sistemacontable.entity.Cliente;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;

/** Clientes: nombre y NIT obligatorios, NIT unico (validado antes de la constraint); no se elimina con ventas. */
@Stateless
@LocalBean
public class ClienteService {

    @Inject
    private ClienteDAO clienteDAO;
    @Inject
    private VentaDAO ventaDAO;

    public Cliente crear(ClienteCmd cmd) {
        Cliente cliente = new Cliente();
        aplicar(cliente, cmd);
        clienteDAO.crear(cliente);
        clienteDAO.flush();
        return cliente;
    }

    public Cliente modificar(Long id, ClienteCmd cmd) {
        Cliente cliente = buscarPorId(id);
        aplicar(cliente, cmd);
        clienteDAO.modificar(cliente);
        return cliente;
    }

    public void eliminar(Long id) {
        Cliente cliente = buscarPorId(id);
        if (ventaDAO.existePorReferencia("cliente", cliente)) {
            throw new ReglaNegocioException("No se puede eliminar el cliente " + id + " porque tiene ventas");
        }
        clienteDAO.eliminar(id);
    }

    public Cliente buscarPorId(Long id) {
        return clienteDAO.buscarPorId(id).orElseThrow(() -> new NoEncontradoException("Cliente", id));
    }

    public Cliente buscarPorNit(String nit) {
        return clienteDAO.findByNit(nit).orElseThrow(() -> new NoEncontradoException("Cliente con NIT", nit));
    }

    public List<Cliente> listar(int first, int max) {
        return clienteDAO.findRange(first, max);
    }

    private void aplicar(Cliente cliente, ClienteCmd cmd) {
        if (cmd == null) {
            throw new ReglaNegocioException("Los datos del cliente son obligatorios");
        }
        String nombre = Validaciones.texto(cmd.nombre(), "nombre");
        String nit = Validaciones.texto(cmd.nit(), "NIT");
        clienteDAO.findByNit(nit).ifPresent(otro -> {
            if (!otro.equals(cliente)) {
                throw new ReglaNegocioException("Ya existe un cliente con el NIT " + nit);
            }
        });
        cliente.setNombre(nombre);
        cliente.setDescripcion(Validaciones.textoOpcional(cmd.descripcion()));
        cliente.setNit(nit);
    }
}
