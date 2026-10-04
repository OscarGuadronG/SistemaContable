package com.sistemacontable.boundary.mapper;

import com.sistemacontable.boundary.dto.AlmacenDto;
import com.sistemacontable.boundary.dto.ClienteDto;
import com.sistemacontable.boundary.dto.CuentaDto;
import com.sistemacontable.boundary.dto.DepartamentoDto;
import com.sistemacontable.boundary.dto.EmpleadoDto;
import com.sistemacontable.boundary.dto.LoteDto;
import com.sistemacontable.boundary.dto.ProductoDto;
import com.sistemacontable.boundary.dto.ProveedorDto;
import com.sistemacontable.boundary.dto.TerceroDto;
import com.sistemacontable.boundary.dto.TipoProductoDto;
import com.sistemacontable.boundary.dto.UnidadMedidaDto;
import com.sistemacontable.entity.Almacen;
import com.sistemacontable.entity.Cliente;
import com.sistemacontable.entity.Cuenta;
import com.sistemacontable.entity.Departamento;
import com.sistemacontable.entity.Empleado;
import com.sistemacontable.entity.Lote;
import com.sistemacontable.entity.Producto;
import com.sistemacontable.entity.Proveedor;
import com.sistemacontable.entity.Tercero;
import com.sistemacontable.entity.TipoProducto;
import com.sistemacontable.entity.UnidadMedida;

/** Entidad a DTO de los catalogos. Una entidad nula se mapea a nulo. */
public final class CatalogoMapper {

    private CatalogoMapper() {
    }

    public static DepartamentoDto departamento(Departamento d) {
        return d == null ? null : new DepartamentoDto(d.getId(), d.getNombre(), d.getDescripcion());
    }

    public static EmpleadoDto empleado(Empleado e) {
        return e == null ? null : new EmpleadoDto(e.getId(), e.getNombre(), e.getApellido(), e.getFechNac(),
                Mapeo.id(e.getDepartamento()), e.getSalario(), e.getEstado(), e.getNumeroIsss(), e.getDui());
    }

    public static CuentaDto cuenta(Cuenta c) {
        return c == null ? null : new CuentaDto(c.getId(), c.getCodigo(), c.getNombre(), c.getNaturaleza(),
                c.getActivo(), c.getImputable(), Mapeo.id(c.getPadre()));
    }

    public static TerceroDto tercero(Tercero t) {
        return t == null ? null : new TerceroDto(t.getId(), t.getNombre(), t.getDescripcion());
    }

    public static ClienteDto cliente(Cliente c) {
        return c == null ? null : new ClienteDto(c.getId(), c.getNombre(), c.getDescripcion(), c.getNit());
    }

    public static ProveedorDto proveedor(Proveedor p) {
        return p == null ? null : new ProveedorDto(p.getId(), p.getNombre(), p.getDescripcion(), p.getNit());
    }

    public static UnidadMedidaDto unidadMedida(UnidadMedida u) {
        return u == null ? null : new UnidadMedidaDto(u.getId(), u.getNombre(), u.getAbrev());
    }

    public static TipoProductoDto tipoProducto(TipoProducto t) {
        return t == null ? null : new TipoProductoDto(t.getId(), t.getNombre(), Mapeo.id(t.getPadre()));
    }

    public static ProductoDto producto(Producto p) {
        return p == null ? null : new ProductoDto(p.getId(), p.getNombre(), Mapeo.id(p.getUnidadMedida()),
                Mapeo.id(p.getTipo()), p.getDescripcion());
    }

    public static LoteDto lote(Lote l) {
        return l == null ? null : new LoteDto(l.getId(), Mapeo.id(l.getProducto()), l.getNumLote(),
                l.getFechCaducidad());
    }

    public static AlmacenDto almacen(Almacen a) {
        return a == null ? null : new AlmacenDto(a.getId(), a.getNombre(), a.getDescripcion(), a.getEstado());
    }
}
