package com.analizador.reporte;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.sintactico.TokenStream;
import java.io.StringReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GeneradorReporteTest {

    @Test
    @DisplayName("Validar cálculo de métricas en un script limpio")
    void testMetricasScriptLimpio() throws Exception {
        String codigo = """
            const PI = 3.14159;
            let radio = 10;
            let area = PI * radio * radio;
            """;
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        var metricas = GeneradorReporte.calcularMetricas(codigo, stream.getTodosLosTokens(), tabla, lexer.getErrores());

        assertEquals(3, metricas.totalLineas());
        assertTrue(metricas.totalTokens() > 10);
        assertEquals(3, metricas.totalIdentificadoresUnicos()); // PI, radio, area
        assertEquals(3, metricas.palabrasReservadas()); // 1 const + 2 let = 3
        assertEquals(0, metricas.totalErrores());
    }

    @Test
    @DisplayName("Generar reporte HTML con estructura y estilos correctos")
    void testGenerarHTML() throws Exception {
        String codigo = "let x = 42; let y = x + 10;";
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        String html = GeneradorReporte.generarHTML("test.js", codigo, stream.getTodosLosTokens(), tabla, lexer.getErrores());

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Reporte Contextual de Análisis Léxico"));
        assertTrue(html.contains("test.js"));
        assertTrue(html.contains("Tokens Reconocidos"));
        assertTrue(html.contains("Tabla de Símbolos"));
        assertTrue(html.contains("x"));
        assertTrue(html.contains("y"));
    }

    @Test
    @DisplayName("Generar reporte Markdown estructurado")
    void testGenerarMarkdown() throws Exception {
        String codigo = "function sumar(a, b) { return a + b; }";
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        String md = GeneradorReporte.generarMarkdown("sumar.js", codigo, stream.getTodosLosTokens(), tabla, lexer.getErrores());

        assertNotNull(md);
        assertTrue(md.contains("# Reporte de Análisis Léxico - JavaScript"));
        assertTrue(md.contains("| Total de Tokens Reconocidos |"));
        assertTrue(md.contains("## 3. Tabla de Símbolos"));
        assertTrue(md.contains("sumar"));
    }

    @Test
    @DisplayName("Reporte con errores léxicos detectados")
    void testReporteConErrores() throws Exception {
        String codigo = "let @invalido = 100 # error;";
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        String html = GeneradorReporte.generarHTML("error.js", codigo, stream.getTodosLosTokens(), tabla, lexer.getErrores());
        assertTrue(html.contains("Incidentes Léxicos Detectados"));
        assertTrue(html.contains("@"));
        assertTrue(html.contains("#"));

        String md = GeneradorReporte.generarMarkdown("error.js", codigo, stream.getTodosLosTokens(), tabla, lexer.getErrores());
        assertTrue(md.contains("## 2. Errores Léxicos Detectados"));
        assertTrue(md.contains("@"));
        assertTrue(md.contains("#"));
    }

    @Test
    @DisplayName("Generar HTML ligero para Swing")
    void testGenerarHTMLParaSwing() throws Exception {
        String codigo = "let valor = 123;";
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        String swingHtml = GeneradorReporte.generarHTMLParaSwing("demo.js", codigo, stream.getTodosLosTokens(), tabla, lexer.getErrores());
        assertNotNull(swingHtml);
        assertTrue(swingHtml.contains("<html>"));
        assertTrue(swingHtml.contains("Informe de Análisis Léxico"));
        assertTrue(swingHtml.contains("demo.js"));
    }
}
