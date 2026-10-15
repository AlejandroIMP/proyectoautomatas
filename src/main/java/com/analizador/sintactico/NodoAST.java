package com.analizador.sintactico;

import com.analizador.modelo.Token;
import java.util.List;

public sealed interface NodoAST permits NodoAST.ExpresionBinaria, NodoAST.Literal,
        NodoAST.DeclaracionVariable, NodoAST.Programa {
    record Programa(List<NodoAST> sentencias) implements NodoAST {}
    record ExpresionBinaria(NodoAST izquierda, Token operador, NodoAST derecha) implements NodoAST {}
    record Literal(Token token) implements NodoAST {}
    record DeclaracionVariable(Token tipoDeclaracion, Token identificador,
                               NodoAST valorInicial) implements NodoAST {}
}
