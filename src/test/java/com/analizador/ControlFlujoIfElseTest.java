package com.analizador;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.sintactico.AnalizadorSintacticoJS;
import com.analizador.sintactico.NodoAST;
import java.io.StringReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del Módulo 2 (Pablo): control de flujo IF / ELSE.
 * Clase aparte para no chocar con AnalizadorSintacticoTest al unir ramas.
 */
public class ControlFlujoIfElseTest {

    private AnalizadorSintacticoJS parsear(String codigo) throws Exception {
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), new TablaSimbolos());
        AnalizadorSintacticoJS parser = new AnalizadorSintacticoJS(lexer);
        parser.parse();
        return parser;
    }

    private NodoAST.Programa programa(AnalizadorSintacticoJS parser) {
        assertTrue(parser.getErroresSintacticos().isEmpty(), "No deben existir errores sintácticos");
        assertInstanceOf(NodoAST.Programa.class, parser.getAstRaiz());
        return (NodoAST.Programa) parser.getAstRaiz();
    }

    @Test
    @DisplayName("Dangling else: el else se asocia al if más cercano")
    void testDanglingElse() throws Exception {
        String codigo = """
                if (a) if (b) x = 1; else x = 2;
                """;
        NodoAST.Programa prog = programa(parsear(codigo));
        assertEquals(1, prog.sentencias().size());

        NodoAST.SentenciaIf externo = (NodoAST.SentenciaIf) prog.sentencias().get(0);
        assertNull(externo.bloqueElse(), "El if externo no debe tener else");

        NodoAST.SentenciaIf interno = (NodoAST.SentenciaIf) externo.bloqueThen();
        assertNotNull(interno.bloqueElse(), "El else debe pertenecer al if interno");
    }

    @Test
    @DisplayName("Cadena else-if con else final")
    void testElseIfEncadenado() throws Exception {
        String codigo = """
                if (a) { x = 1; } else if (b) { x = 2; } else { x = 3; }
                """;
        NodoAST.Programa prog = programa(parsear(codigo));
        assertEquals(1, prog.sentencias().size());

        NodoAST.SentenciaIf primero = (NodoAST.SentenciaIf) prog.sentencias().get(0);
        assertInstanceOf(NodoAST.Bloque.class, primero.bloqueThen());
        NodoAST.SentenciaIf segundo = (NodoAST.SentenciaIf) primero.bloqueElse();
        assertInstanceOf(NodoAST.Bloque.class, segundo.bloqueElse());
    }

    @Test
    @DisplayName("Bloques anidados y bloque vacío")
    void testBloquesAnidados() throws Exception {
        String codigo = """
                if (a) {
                    if (b) { x = 1; }
                    if (c) { }
                }
                """;
        NodoAST.Programa prog = programa(parsear(codigo));
        NodoAST.SentenciaIf externo = (NodoAST.SentenciaIf) prog.sentencias().get(0);
        NodoAST.Bloque cuerpo = (NodoAST.Bloque) externo.bloqueThen();
        assertEquals(2, cuerpo.sentencias().size());

        NodoAST.SentenciaIf segundo = (NodoAST.SentenciaIf) cuerpo.sentencias().get(1);
        assertEquals(0, ((NodoAST.Bloque) segundo.bloqueThen()).sentencias().size());
    }

    @Test
    @DisplayName("Paréntesis sin cerrar en la condición genera error sintáctico")
    void testErrorCondicionSinCerrar() throws Exception {
        AnalizadorSintacticoJS parser = parsear("if (a > 1 { x = 1; }");
        assertFalse(parser.getErroresSintacticos().isEmpty(), "Debe reportarse un error sintáctico");
    }
}
