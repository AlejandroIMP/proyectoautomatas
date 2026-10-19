package com.analizador.sintactico;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import java_cup.runtime.Symbol;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TokenStream {
    private final List<Token> buffer = new ArrayList<>();
    private int posicionActual;

    public TokenStream(AnalizadorLexicoJS lexer) throws IOException {
        Symbol s;
        do {
            s = lexer.yylex();
        } while (s != null && s.sym != sym.EOF);

        buffer.addAll(lexer.getTokens());
        if (buffer.isEmpty() || buffer.getLast().tipo() != TokenType.EOF) {
            buffer.add(new Token(TokenType.EOF, "EOF", 1, 1));
        }
    }

    public Token peek() {
        if (posicionActual < buffer.size()) {
            return buffer.get(posicionActual);
        }
        return new Token(TokenType.EOF, "EOF", 1, 1);
    }

    public Token next() {
        Token token = peek();
        if (token.tipo() != TokenType.EOF) {
            posicionActual++;
        }
        return token;
    }

    public Token match(TokenType esperado) {
        Token actual = peek();
        if (actual.tipo() != esperado) {
            throw new IllegalStateException("Se esperaba " + esperado + " pero se encontró " + actual.lexema());
        }
        return next();
    }

    public List<Token> getTodosLosTokens() {
        return List.copyOf(buffer);
    }
}
