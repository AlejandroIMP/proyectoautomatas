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
    @DisplayName("Sentencias simples sin llaves en rama then y else")
    void testSentenciasSimplesSinLlaves() throws Exception {
        String codigo = """
                if (x > 0)
                    print(x);
                else
                    console.log("no positivo");
                """;
        NodoAST.Programa prog = programa(parsear(codigo));
        assertEquals(1, prog.sentencias().size());

        NodoAST.SentenciaIf sentenciaIf = (NodoAST.SentenciaIf) prog.sentencias().get(0);
        assertInstanceOf(NodoAST.SentenciaImprimir.class, sentenciaIf.bloqueThen());
        assertInstanceOf(NodoAST.SentenciaImprimir.class, sentenciaIf.bloqueElse());
    }

    @Test
    @DisplayName("Condiciones lógicas y relacionales complejas combinadas con &&, || y !")
    void testCondicionLogicaCompleja() throws Exception {
        String codigo = """
                if ((edad >= 18 && !bloqueado) || saldo > 1000) {
                    let acceso = true;
                }
                """;
        NodoAST.Programa prog = programa(parsear(codigo));
        assertEquals(1, prog.sentencias().size());

        NodoAST.SentenciaIf sentenciaIf = (NodoAST.SentenciaIf) prog.sentencias().get(0);
        assertNotNull(sentenciaIf.condicion());
        assertInstanceOf(NodoAST.ExpresionBinaria.class, sentenciaIf.condicion());
        NodoAST.ExpresionBinaria orExp = (NodoAST.ExpresionBinaria) sentenciaIf.condicion();
        assertEquals("||", orExp.operador());
    }

    @Test
    @DisplayName("Múltiples else-if encadenados con declaraciones y asignaciones")
    void testElseIfMultipleConDeclaraciones() throws Exception {
        String codigo = """
                let calificacion = "F";
                if (nota >= 90) {
                    calificacion = "A";
                } else if (nota >= 80) {
                    calificacion = "B";
                } else if (nota >= 70) {
                    calificacion = "C";
                } else {
                    calificacion = "D";
                }
                """;
        NodoAST.Programa prog = programa(parsear(codigo));
        assertEquals(2, prog.sentencias().size());

        assertInstanceOf(NodoAST.DeclaracionVariable.class, prog.sentencias().get(0));
        assertInstanceOf(NodoAST.SentenciaIf.class, prog.sentencias().get(1));

        NodoAST.SentenciaIf if1 = (NodoAST.SentenciaIf) prog.sentencias().get(1);
        assertInstanceOf(NodoAST.SentenciaIf.class, if1.bloqueElse());

        NodoAST.SentenciaIf if2 = (NodoAST.SentenciaIf) if1.bloqueElse();
        assertInstanceOf(NodoAST.SentenciaIf.class, if2.bloqueElse());

        NodoAST.SentenciaIf if3 = (NodoAST.SentenciaIf) if2.bloqueElse();
        assertInstanceOf(NodoAST.Bloque.class, if3.bloqueElse());
    }

    @Test
    @DisplayName("Paréntesis sin cerrar en la condición genera error sintáctico")
    void testErrorCondicionSinCerrar() throws Exception {
        AnalizadorSintacticoJS parser = parsear("if (a > 1 { x = 1; }");
        assertFalse(parser.getErroresSintacticos().isEmpty(), "Debe reportarse un error sintáctico");
    }

    @Test
    @DisplayName("Condición vacía genera error sintáctico")
    void testErrorCondicionVacia() throws Exception {
        AnalizadorSintacticoJS parser = parsear("if () { x = 1; }");
        assertFalse(parser.getErroresSintacticos().isEmpty(), "Condición vacía debe arrojar error");
    }

    @Test
    @DisplayName("Else sin if previo genera error sintáctico")
    void testErrorElseSinIf() throws Exception {
        AnalizadorSintacticoJS parser = parsear("else { x = 1; }");
        assertFalse(parser.getErroresSintacticos().isEmpty(), "Else huérfano debe arrojar error");
    }
}
