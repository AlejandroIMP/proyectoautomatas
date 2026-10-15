package com.analizador.modelo;

public record ErrorLexico(String lexemaInvalido, int linea, int columna, String descripcion) {}
