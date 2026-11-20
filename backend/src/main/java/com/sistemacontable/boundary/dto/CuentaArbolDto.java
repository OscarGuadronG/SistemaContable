package com.sistemacontable.boundary.dto;
import com.sistemacontable.entity.enums.NaturalezaCuenta;
import java.util.List;
public record CuentaArbolDto(Long id,Integer codigo,String nombre,NaturalezaCuenta naturaleza,Boolean activo,Boolean imputable,int nivel,List<CuentaArbolDto> hijos) { }
