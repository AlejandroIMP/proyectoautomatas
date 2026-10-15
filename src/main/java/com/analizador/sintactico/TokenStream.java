package com.analizador.sintactico;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TokenStream {
    private final List<Token> buffer = new ArrayList<>();
    private int posicionActual;
    public TokenStream(AnalizadorLexicoJS lexer) throws IOException {
        Token token;
        do { token = lexer.yylex(); buffer.add(token); } while (token.tipo() != TokenType.EOF);
    }
    public Token peek() { return buffer.get(posicionActual); }
    public Token next() {
        Token token = peek();
        if (token.tipo() != TokenType.EOF) posicionActual++;
        return token;
    }
    public Token match(TokenType esperado) {
        Token actual = peek();
        if (actual.tipo() != esperado)
            throw new IllegalStateException("Se esperaba " + esperado + " pero se encontró " + actual.lexema());
        return next();
    }
    public List<Token> getTodosLosTokens() { return List.copyOf(buffer); }
}
