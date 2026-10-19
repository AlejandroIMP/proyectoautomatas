package com.analizador.sintactico;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public sealed interface NodoAST permits
        NodoAST.Programa,
        NodoAST.DeclaracionVariable,
        NodoAST.Asignacion,
        NodoAST.SentenciaIf,
        NodoAST.SentenciaImprimir,
        NodoAST.Bloque,
        NodoAST.ExpresionBinaria,
        NodoAST.ExpresionUnaria,
        NodoAST.Literal,
        NodoAST.Identificador,
        NodoAST.SentenciaExpresion {

    DefaultMutableTreeNode toTreeNode();

    record Programa(List<NodoAST> sentencias) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Programa (" + sentencias.size() + " sentencias)");
            for (NodoAST s : sentencias) {
                if (s != null) {
                    nodo.add(s.toTreeNode());
                }
            }
            return nodo;
        }
    }

    record DeclaracionVariable(String tipoDeclaracion, String identificador, NodoAST valorInicial) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Declaración: " + tipoDeclaracion + " " + identificador);
            if (valorInicial != null) {
                DefaultMutableTreeNode inicializacion = new DefaultMutableTreeNode("Valor Inicial (=)");
                inicializacion.add(valorInicial.toTreeNode());
                nodo.add(inicializacion);
            }
            return nodo;
        }
    }

    record Asignacion(String identificador, NodoAST valor) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Asignación: " + identificador + " =");
            if (valor != null) {
                nodo.add(valor.toTreeNode());
            }
            return nodo;
        }
    }

    record SentenciaIf(NodoAST condicion, NodoAST bloqueThen, NodoAST bloqueElse) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Instrucción IF");
            if (condicion != null) {
                DefaultMutableTreeNode cond = new DefaultMutableTreeNode("Condición");
                cond.add(condicion.toTreeNode());
                nodo.add(cond);
            }
            if (bloqueThen != null) {
                DefaultMutableTreeNode thenNode = new DefaultMutableTreeNode("Rama THEN");
                thenNode.add(bloqueThen.toTreeNode());
                nodo.add(thenNode);
            }
            if (bloqueElse != null) {
                DefaultMutableTreeNode elseNode = new DefaultMutableTreeNode("Rama ELSE");
                elseNode.add(bloqueElse.toTreeNode());
                nodo.add(elseNode);
            }
            return nodo;
        }
    }

    record SentenciaImprimir(String funcion, List<NodoAST> argumentos) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Impresión: " + funcion);
            DefaultMutableTreeNode argsNode = new DefaultMutableTreeNode("Argumentos (" + argumentos.size() + ")");
            for (NodoAST arg : argumentos) {
                if (arg != null) {
                    argsNode.add(arg.toTreeNode());
                }
            }
            nodo.add(argsNode);
            return nodo;
        }
    }

    record Bloque(List<NodoAST> sentencias) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Bloque { ... } (" + sentencias.size() + ")");
            for (NodoAST s : sentencias) {
                if (s != null) {
                    nodo.add(s.toTreeNode());
                }
            }
            return nodo;
        }
    }

    record ExpresionBinaria(NodoAST izquierda, String operador, NodoAST derecha) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Operación Binaria: " + operador);
            if (izquierda != null) nodo.add(izquierda.toTreeNode());
            if (derecha != null) nodo.add(derecha.toTreeNode());
            return nodo;
        }
    }

    record ExpresionUnaria(String operador, NodoAST expresion) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Operación Unaria: " + operador);
            if (expresion != null) nodo.add(expresion.toTreeNode());
            return nodo;
        }
    }

    record Literal(Object valor, String tipo) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            return new DefaultMutableTreeNode("Literal (" + tipo + "): " + valor);
        }
    }

    record Identificador(String nombre) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            return new DefaultMutableTreeNode("Identificador: " + nombre);
        }
    }

    record SentenciaExpresion(NodoAST expresion) implements NodoAST {
        @Override
        public DefaultMutableTreeNode toTreeNode() {
            DefaultMutableTreeNode nodo = new DefaultMutableTreeNode("Sentencia de Expresión");
            if (expresion != null) nodo.add(expresion.toTreeNode());
            return nodo;
        }
    }
}
