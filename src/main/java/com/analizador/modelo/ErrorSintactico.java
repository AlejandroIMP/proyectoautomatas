package com.analizador.modelo;

public record ErrorSintactico(String tokenEncontrado, int linea, int columna, String descripcion) {}
