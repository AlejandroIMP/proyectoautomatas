package com.analizador.modelo;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TablaSimbolos {
    public record EntradaSimbolo(String nombre, String categoria, int primeraLinea,
                                 int primeraColumna, int ocurrencias) {}
    private final Map<String, EntradaSimbolo> tabla = new LinkedHashMap<>();

    public void registrarIdentificador(String nombre, int linea, int columna) {
        tabla.compute(nombre, (k, v) -> v == null
                ? new EntradaSimbolo(nombre, "IDENTIFICADOR", linea, columna, 1)
                : new EntradaSimbolo(nombre, v.categoria(), v.primeraLinea(),
                    v.primeraColumna(), v.ocurrencias() + 1));
    }
    public Collection<EntradaSimbolo> obtenerTodos() { return tabla.values(); }
    public void limpiar() { tabla.clear(); }
}
