package com.analizador.modelo;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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

    public boolean existe(String nombre) {
        return tabla.containsKey(nombre);
    }

    public EntradaSimbolo buscar(String nombre) {
        return tabla.get(nombre);
    }

    public Optional<EntradaSimbolo> buscarOpcional(String nombre) {
        return Optional.ofNullable(tabla.get(nombre));
    }

    public int cantidad() {
        return tabla.size();
    }

    public boolean estaVacia() {
        return tabla.isEmpty();
    }

    public Map<String, EntradaSimbolo> obtenerMapa() {
        return Collections.unmodifiableMap(tabla);
    }

    public void mostrar() {
        System.out.println("=== TABLA DE SIMBOLOS ===");
        if (tabla.isEmpty()) {
            System.out.println("(sin identificadores)");
            return;
        }
        for (EntradaSimbolo s : tabla.values()) {
            System.out.printf("%s | categoria: %s | primera aparicion: linea %d, columna %d | ocurrencias: %d%n",
                    s.nombre(), s.categoria(), s.primeraLinea(), s.primeraColumna(), s.ocurrencias());
        }
    }

    public Collection<EntradaSimbolo> obtenerTodos() { return tabla.values(); }
    public void limpiar() { tabla.clear(); }
}
