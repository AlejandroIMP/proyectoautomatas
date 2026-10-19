package com.analizador;

import com.analizador.reporte.GeneradorReporte;
import com.analizador.servicio.CompiladorJS;
import com.analizador.sintactico.NodoAST;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        assertTrue(html.contains("Declaraciones Variables"));

        // Validar Markdown completo
        String md = GeneradorReporte.generarMarkdown("prueba.js", codigo, res.tokens(),
                res.tablaSimbolos(), res.erroresLexicos(), res.erroresSintacticos(), res.astRaiz());
        assertTrue(md.contains("Reporte de Análisis Léxico y Sintáctico"));
        assertTrue(md.contains("## 4. Estructura Jerárquica del AST"));
        assertTrue(md.contains("SentenciaIf"));
    }
}
