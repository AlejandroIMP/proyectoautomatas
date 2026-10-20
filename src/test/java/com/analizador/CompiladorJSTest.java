package com.analizador;

import com.analizador.reporte.GeneradorReporte;
import com.analizador.servicio.CompiladorJS;
import com.analizador.sintactico.NodoAST;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class CompiladorJSTest {

    @Test
    @DisplayName("Alejandro - Pipeline: Compilación integral limpia de código JavaScript")
    void testCompilacionExitosa() {
        String codigo = """
                let x = 10;
                let y = 20;
                if (x < y) {
                    print("x es menor que y:", x);
                } else {
                    console.log("x no es menor");
                }
                """;

        CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(codigo);

        assertTrue(res.esExitoso(), "La compilación integral debe ser exitosa");
        assertFalse(res.tieneErroresLexicos(), "No deben existir errores léxicos");
        assertFalse(res.tieneErroresSintacticos(), "No deben existir errores sintácticos");
        assertEquals(0, res.totalErrores());
        assertFalse(res.tokens().isEmpty());
        assertNotNull(res.astRaiz());
        assertTrue(res.astRaiz() instanceof NodoAST.Programa);
        assertTrue(res.tiempoCompilacionMs() >= 0);
    }

    @Test
    @DisplayName("Alejandro - Pipeline: Detección y contención de errores sintácticos")
    void testCompilacionConErrorSintactico() {
        String codigoInvalido = """
                let x = ;
                print(x);
                """;

        CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(codigoInvalido);

        assertFalse(res.esExitoso());
        assertTrue(res.tieneErroresSintacticos());
        assertFalse(res.erroresSintacticos().isEmpty());
    }

    @Test
    @DisplayName("Alejandro - Reportes: Generación de reporte completo con métricas sintácticas y AST")
    void testReporteConSintacticoYAST() {
        String codigo = """
                let edad = 18;
                if (edad >= 18) {
                    console.log("Mayor de edad");
                }
                """;

        CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(codigo);

        // Validar cálculo de métricas sintácticas
        var metricasSintacticas = GeneradorReporte.calcularMetricasSintacticas(res.astRaiz(), res.erroresSintacticos());
        assertEquals(2, metricasSintacticas.totalSentencias());
        assertEquals(1, metricasSintacticas.declaracionesVariables());
        assertEquals(1, metricasSintacticas.sentenciasIf());
        assertEquals(0, metricasSintacticas.totalErroresSintacticos());

        // Validar HTML completo
        String html = GeneradorReporte.generarHTML("prueba.js", codigo, res.tokens(),
                res.tablaSimbolos(), res.erroresLexicos(), res.erroresSintacticos(), res.astRaiz());
        assertTrue(html.contains("Informe de Análisis Léxico y Sintáctico") || html.contains("Reporte Contextual de Análisis Léxico y Sintáctico"));
        assertTrue(html.contains("Representación Textual del Árbol Sintáctico (AST)"));
        assertTrue(html.contains("Diagrama Visual Interactivo del AST (Mermaid)"));
        assertTrue(html.contains("Declaraciones Variables"));

        // Validar Markdown completo
        String md = GeneradorReporte.generarMarkdown("prueba.js", codigo, res.tokens(),
                res.tablaSimbolos(), res.erroresLexicos(), res.erroresSintacticos(), res.astRaiz());
        assertTrue(md.contains("Reporte de Análisis Léxico y Sintáctico"));
        assertTrue(md.contains("## 4. Estructura Jerárquica del AST"));
        assertTrue(md.contains("```mermaid"));
        assertTrue(md.contains("SentenciaIf"));
    }

    @Test
    @DisplayName("Alejandro - AST: Generación de Diagrama Mermaid y recorrido con Visitor")
    void testMermaidYRecorridoAST() {
        String codigo = """
                let total = 100;
                print(total);
                """;
        CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(codigo);
        assertNotNull(res.astRaiz());

        // Validar formato Mermaid
        String mermaid = res.astRaiz().toMermaid();
        assertNotNull(mermaid);
        assertTrue(mermaid.startsWith("flowchart TD\n"));
        assertTrue(mermaid.contains("Programa (2 sentencias)"));
        assertTrue(mermaid.contains("Declaración: let total"));
        assertTrue(mermaid.contains("Impresión: print (1 args)"));

        // Validar recorrido con Consumer/Visitor
        AtomicInteger nodosVisitados = new AtomicInteger(0);
        res.astRaiz().recorrer(nodo -> nodosVisitados.incrementAndGet());
        assertTrue(nodosVisitados.get() >= 4, "Debe visitar al menos Programa, Declaracion, Literal, Impresion");
    }

    @Test
    @DisplayName("Alejandro - Análisis Estático: Advertencias de variables no inicializadas o de un solo uso")
    void testAdvertenciasSemanticas() {
        String codigoConAvisos = """
                let sinUso;
                let usado = 10;
                print(usado);
                """;
        CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(codigoConAvisos);

        assertTrue(res.tieneAdvertencias(), "Debe emitir advertencias semánticas");
        assertTrue(res.advertencias().stream().anyMatch(a -> a.contains("sin valor inicial")),
                "Debe advertir sobre 'sinUso' sin valor inicial");
        assertTrue(res.advertencias().stream().anyMatch(a -> a.contains("sinUso") && a.contains("1 vez")),
                "Debe advertir sobre 'sinUso' referenciada solo una vez");
    }

    @Test
    @DisplayName("Alejandro - GUI: Validación del componente de números de línea sin excepciones")
    void testNumeroLineaGutter() {
        javax.swing.JTextArea textArea = new javax.swing.JTextArea("linea 1\nlinea 2\nlinea 3\nlinea 4\nlinea 5");
        com.analizador.gui.NumeroLinea gutter = new com.analizador.gui.NumeroLinea(textArea);
        assertNotNull(gutter.getPreferredSize());
        assertTrue(gutter.getPreferredSize().width > 0);

        // Simular renderizado gráfico en BufferedImage
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(100, 200, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        assertDoesNotThrow(() -> gutter.paint(img.getGraphics()), "El renderizado del gutter de líneas no debe arrojar excepciones");
    }
}
