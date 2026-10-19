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
 * Servicio central del compilador/analizador (Módulo de Alejandro - Arquitectura).
 * Coordina la ejecución integrada del analizador léxico (JFlex) y del analizador sintáctico (JCUP).
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
            NodoAST astRaiz
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

        public int totalErrores() {
            return erroresLexicos.size() + erroresSintacticos.size();
        }
    }

    /**
     * Ejecuta el análisis léxico y sintáctico completo sobre el código fuente recibido.
     *
     * @param codigoFuente Código JavaScript o pseudocódigo a analizar.
     * @return Objeto con los tokens, tabla de símbolos, errores y el AST generado.
     */
    public static ResultadoCompilacion compilar(String codigoFuente) {
        String fuente = (codigoFuente == null) ? "" : codigoFuente;
        TablaSimbolos tablaSimbolos = new TablaSimbolos();

        // 1. Fase de Análisis Léxico
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

        return new ResultadoCompilacion(
                fuente,
                List.copyOf(tokens),
                tablaSimbolos,
                List.copyOf(erroresLexicos),
                List.copyOf(erroresSintacticos),
                astRaiz
        );
    }
}
