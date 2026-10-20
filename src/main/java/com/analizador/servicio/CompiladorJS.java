package com.analizador.servicio;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.ErrorSintactico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.sintactico.AnalizadorSintacticoJS;
import com.analizador.sintactico.NodoAST;
import com.analizador.sintactico.TokenStream;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio central del compilador/analizador (Módulo de Alejandro - Arquitectura y Pipeline).
 * Coordina la ejecución integrada del analizador léxico (JFlex), analizador sintáctico (JCUP),
 * métricas de rendimiento y análisis estático de advertencias.
 */
public final class CompiladorJS {

    private CompiladorJS() {}

    /**
     * Resultado inmutable que encapsula todo el ciclo de compilación.
     */
    public record ResultadoCompilacion(
            String codigoFuente,
            List<Token> tokens,
            TablaSimbolos tablaSimbolos,
            List<ErrorLexico> erroresLexicos,
            List<ErrorSintactico> erroresSintacticos,
            List<String> advertencias,
            NodoAST astRaiz,
            long tiempoCompilacionMs
    ) {
        public boolean esExitoso() {
            return erroresLexicos.isEmpty() && erroresSintacticos.isEmpty() && astRaiz != null;
        }

        public boolean tieneErroresLexicos() {
            return !erroresLexicos.isEmpty();
        }

        public boolean tieneErroresSintacticos() {
            return !erroresSintacticos.isEmpty();
        }

        public boolean tieneAdvertencias() {
            return !advertencias.isEmpty();
        }

        public int totalErrores() {
            return erroresLexicos.size() + erroresSintacticos.size();
        }
    }

    /**
     * Ejecuta el pipeline completo de compilación sobre el código fuente.
     *
     * @param codigoFuente Código JavaScript o pseudocódigo a analizar.
     * @return Objeto con los tokens, tabla de símbolos, errores, advertencias, AST y tiempo de ejecución.
     */
    public static ResultadoCompilacion compilar(String codigoFuente) {
        long inicio = System.currentTimeMillis();
        String fuente = (codigoFuente == null) ? "" : codigoFuente;
        TablaSimbolos tablaSimbolos = new TablaSimbolos();

        // 1. Fase de Análisis Léxico (JFlex)
        List<Token> tokens = new ArrayList<>();
        List<ErrorLexico> erroresLexicos = new ArrayList<>();
        try {
            AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(fuente), tablaSimbolos);
            TokenStream stream = new TokenStream(lexer);
            tokens = stream.getTodosLosTokens();
            erroresLexicos = lexer.getErrores();
        } catch (Exception ex) {
            erroresLexicos.add(new ErrorLexico("EXCEPCION_LEXER", 1, 1,
                    "Fallo durante análisis léxico: " + ex.getMessage()));
        }

        // 2. Fase de Análisis Sintáctico con JCUP
        List<ErrorSintactico> erroresSintacticos = new ArrayList<>();
        NodoAST astRaiz = null;
        try {
            AnalizadorLexicoJS lexerCup = new AnalizadorLexicoJS(new StringReader(fuente), new TablaSimbolos());
            AnalizadorSintacticoJS parser = new AnalizadorSintacticoJS(lexerCup);
            parser.parse();

            astRaiz = parser.getAstRaiz();
            erroresSintacticos = new ArrayList<>(parser.getErroresSintacticos());
        } catch (Exception ex) {
            erroresSintacticos.add(new ErrorSintactico("PARSER_EXCEPTION", 1, 1,
                    "Fallo durante análisis sintáctico: " + ex.getMessage()));
        }

        // 3. Fase de Análisis Estático de Advertencias (Alejandro)
        List<String> advertencias = new ArrayList<>();
        if (astRaiz != null) {
            analizarAdvertencias(astRaiz, tablaSimbolos, advertencias);
        }

        long tiempoTotal = Math.max(1, System.currentTimeMillis() - inicio);

        return new ResultadoCompilacion(
                fuente,
                List.copyOf(tokens),
                tablaSimbolos,
                List.copyOf(erroresLexicos),
                List.copyOf(erroresSintacticos),
                List.copyOf(advertencias),
                astRaiz,
                tiempoTotal
        );
    }

    private static void analizarAdvertencias(NodoAST ast, TablaSimbolos tabla, List<String> advertencias) {
        ast.recorrer(nodo -> {
            if (nodo instanceof NodoAST.DeclaracionVariable decl) {
                // Advertencia: variable declarada sin inicializar
                if (decl.valorInicial() == null) {
                    advertencias.add("Variable '" + decl.identificador() + "' declarada (" + decl.tipoDeclaracion() + ") sin valor inicial.");
                }
                // Advertencia: variable declarada pero nunca leída/modificada posteriormente
                var entrada = tabla.buscar(decl.identificador());
                if (entrada != null && entrada.ocurrencias() == 1) {
                    advertencias.add("Variable '" + decl.identificador() + "' declarada pero solo referenciada 1 vez (posible variable no utilizada).");
                }
            }
        });
    }
}
