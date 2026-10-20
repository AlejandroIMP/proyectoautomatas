package com.analizador.sintactico;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Jerarquía de nodos del Árbol de Sintaxis Abstracta (AST) en Java 21.
 * Módulo de Alejandro: Arquitectura, AST y serialización Mermaid/Tree.
 */
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

    String getEtiqueta();

    List<NodoAST> getHijos();

    default DefaultMutableTreeNode toTreeNode() {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(getEtiqueta());
        for (NodoAST hijo : getHijos()) {
            if (hijo != null) {
                treeNode.add(hijo.toTreeNode());
            }
        }
        return treeNode;
    }

    default void recorrer(Consumer<NodoAST> visitador) {
        visitador.accept(this);
        for (NodoAST hijo : getHijos()) {
            if (hijo != null) {
                hijo.recorrer(visitador);
            }
        }
    }

    default String toMermaid() {
        StringBuilder sb = new StringBuilder("flowchart TD\n");
        int[] contador = new int[]{1};
        generarMermaidRec(this, sb, contador, 0);
        return sb.toString();
    }

    private static int generarMermaidRec(NodoAST nodo, StringBuilder sb, int[] contador, int padreId) {
        if (nodo == null) return 0;
        int miId = contador[0]++;
        String textoLimpio = nodo.getEtiqueta().replace("\"", "'").replace("\n", " ");
        sb.append("    n").append(miId).append("[\"").append(textoLimpio).append("\"]\n");
        if (padreId > 0) {
            sb.append("    n").append(padreId).append(" --> n").append(miId).append("\n");
        }
        for (NodoAST hijo : nodo.getHijos()) {
            if (hijo != null) {
                generarMermaidRec(hijo, sb, contador, miId);
            }
        }
        return miId;
    }

    record Programa(List<NodoAST> sentencias) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Programa (" + sentencias.size() + " sentencias)";
        }

        @Override
        public List<NodoAST> getHijos() {
            return sentencias != null ? sentencias : List.of();
        }
    }

    record DeclaracionVariable(String tipoDeclaracion, String identificador, NodoAST valorInicial) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Declaración: " + tipoDeclaracion + " " + identificador;
        }

        @Override
        public List<NodoAST> getHijos() {
            return valorInicial != null ? List.of(valorInicial) : List.of();
        }
    }

    record Asignacion(String identificador, NodoAST valor) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Asignación: " + identificador + " =";
        }

        @Override
        public List<NodoAST> getHijos() {
            return valor != null ? List.of(valor) : List.of();
        }
    }

    record SentenciaIf(NodoAST condicion, NodoAST bloqueThen, NodoAST bloqueElse) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Instrucción IF" + (bloqueElse != null ? " / ELSE" : "");
        }

        @Override
        public List<NodoAST> getHijos() {
            List<NodoAST> hijos = new ArrayList<>();
            if (condicion != null) hijos.add(condicion);
            if (bloqueThen != null) hijos.add(bloqueThen);
            if (bloqueElse != null) hijos.add(bloqueElse);
            return hijos;
        }
    }

    record SentenciaImprimir(String funcion, List<NodoAST> argumentos) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Impresión: " + funcion + " (" + (argumentos != null ? argumentos.size() : 0) + " args)";
        }

        @Override
        public List<NodoAST> getHijos() {
            return argumentos != null ? argumentos : List.of();
        }
    }

    record Bloque(List<NodoAST> sentencias) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Bloque { ... } (" + (sentencias != null ? sentencias.size() : 0) + " sentencias)";
        }

        @Override
        public List<NodoAST> getHijos() {
            return sentencias != null ? sentencias : List.of();
        }
    }

    record ExpresionBinaria(NodoAST izquierda, String operador, NodoAST derecha) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Operación Binaria: " + operador;
        }

        @Override
        public List<NodoAST> getHijos() {
            List<NodoAST> hijos = new ArrayList<>();
            if (izquierda != null) hijos.add(izquierda);
            if (derecha != null) hijos.add(derecha);
            return hijos;
        }
    }

    record ExpresionUnaria(String operador, NodoAST expresion) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Operación Unaria: " + operador;
        }

        @Override
        public List<NodoAST> getHijos() {
            return expresion != null ? List.of(expresion) : List.of();
        }
    }

    record Literal(Object valor, String tipo) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Literal (" + tipo + "): " + valor;
        }

        @Override
        public List<NodoAST> getHijos() {
            return List.of();
        }
    }

    record Identificador(String nombre) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Identificador: " + nombre;
        }

        @Override
        public List<NodoAST> getHijos() {
            return List.of();
        }
    }

    record SentenciaExpresion(NodoAST expresion) implements NodoAST {
        @Override
        public String getEtiqueta() {
            return "Sentencia de Expresión";
        }

        @Override
        public List<NodoAST> getHijos() {
            return expresion != null ? List.of(expresion) : List.of();
        }
    }
}
