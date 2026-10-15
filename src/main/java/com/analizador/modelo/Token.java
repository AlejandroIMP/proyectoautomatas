package com.analizador.modelo;

public record Token(TokenType tipo, String lexema, int linea, int columna, Object valorLiteral) {
    public Token(TokenType tipo, String lexema, int linea, int columna) {
        this(tipo, lexema, linea, columna, null);
    }
}
