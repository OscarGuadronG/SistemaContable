package com.sistemacontable.boundary;

/**
 * Parametros de paginacion ya validados. {@code first} es el desplazamiento (&gt;= 0) y {@code max} el tamano de la
 * pagina (1..{@value #MAX_PERMITIDO}). Son independientes: {@code first = 50, max = 50} es la segunda pagina de 50
 * (el proyecto de referencia exigia {@code max > first} y la rechazaba).
 */
public record Paginacion(int first, int max) {

    public static final int PRIMERO_POR_DEFECTO = 0;
    public static final int MAX_POR_DEFECTO = 50;
    public static final int MAX_PERMITIDO = 100;

    /** Valores por defecto como texto, para {@code @DefaultValue} de JAX-RS. */
    public static final String PRIMERO_POR_DEFECTO_TEXTO = "0";
    public static final String MAX_POR_DEFECTO_TEXTO = "50";

    /**
     * @throws IllegalArgumentException si {@code first < 0} o {@code max} no esta entre 1 y {@value #MAX_PERMITIDO}
     *         (el traductor de excepciones lo convierte en 400)
     */
    public static Paginacion de(int first, int max) {
        if (first < 0) {
            throw new IllegalArgumentException("El parametro 'first' debe ser mayor o igual que 0");
        }
        if (max < 1 || max > MAX_PERMITIDO) {
            throw new IllegalArgumentException("El parametro 'max' debe estar entre 1 y " + MAX_PERMITIDO);
        }
        return new Paginacion(first, max);
    }
}
