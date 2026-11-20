package com.sistemacontable.boundary;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;

/** Conversión ISO uniforme de fechas query/path para los proveedores JAX-RS de Liberty y Jersey. */
@Provider
public class FechaParamConverter implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType != LocalDate.class) return null;
        return new ParamConverter<>() {
            @Override public T fromString(String value) {
                return value == null ? null : rawType.cast(Parametros.fecha(value,"fecha"));
            }
            @Override public String toString(T value) { return value == null ? null : value.toString(); }
        };
    }
}
