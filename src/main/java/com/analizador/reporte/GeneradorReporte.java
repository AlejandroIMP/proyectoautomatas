package com.analizador.reporte;

import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.ErrorSintactico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import com.analizador.sintactico.NodoAST;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generador de reportes contextualizados del análisis léxico y sintáctico en HTML y Markdown.
 * Diseñado y mantenido por Alejandro (Módulo de Arquitectura y Reportes).
 */
public final class GeneradorReporte {

    private GeneradorReporte() {}

    public record MetricasReporte(
            int totalLineas,
            int totalCaracteres,
            int totalTokens,
            int totalIdentificadoresUnicos,
            int totalOcurrenciasIdentificadores,
            int totalErrores,
            int palabrasReservadas,
            int operadores,
            int delimitadores,
            int literales,
            Map<TokenType, Long> conteoPorTipo,
            List<TablaSimbolos.EntradaSimbolo> topIdentificadores
    ) {}

    public record MetricasSintacticas(
            int totalSentencias,
            int declaracionesVariables,
            int sentenciasIf,
            int sentenciasImpresion,
            int asignaciones,
            int sentenciasExpresion,
            int totalErroresSintacticos
    ) {}

    public static MetricasReporte calcularMetricas(String codigoFuente, List<Token> tokens,
                                                   TablaSimbolos tablaSimbolos, List<ErrorLexico> errores) {
        int lineas = codigoFuente.isEmpty() ? 0 : (int) codigoFuente.lines().count();
        int caracteres = codigoFuente.length();

        List<Token> tokensReales = tokens.stream()
                .filter(t -> t.tipo() != TokenType.EOF)
                .toList();

        Map<TokenType, Long> conteoPorTipo = tokensReales.stream()
                .collect(Collectors.groupingBy(Token::tipo, Collectors.counting()));

        int reservadas = 0;
        int operadores = 0;
        int delimitadores = 0;
        int literales = 0;

        for (Token t : tokensReales) {
            switch (t.tipo()) {
                case LET, CONST, VAR, FUNCTION, IF, ELSE, WHILE, FOR, RETURN, TRUE, FALSE, NULL,
                     PRINT, CONSOLE, LOG -> reservadas++;
                case MAS, MENOS, MULT, DIV, MOD, ASIGNACION, INCREMENTO, DECREMENTO,
                     IGUAL_IGUAL, IGUAL_ESTRICTO, DIFERENTE, DIFERENTE_ESTRICTO,
                     MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL, AND, OR, NOT -> operadores++;
                case PAREN_IZQ, PAREN_DER, LLAVE_IZQ, LLAVE_DER, CORCHETE_IZQ, CORCHETE_DER,
                     PUNTO_COMA, COMA, DOS_PUNTOS, PUNTO -> delimitadores++;
                case LIT_ENTERO, LIT_FLOTANTE, LIT_CADENA, LIT_TEMPLATE -> literales++;
            }
        }

        var simbolos = new ArrayList<>(tablaSimbolos.obtenerTodos());
        simbolos.sort((a, b) -> Integer.compare(b.ocurrencias(), a.ocurrencias()));
        List<TablaSimbolos.EntradaSimbolo> top5 = simbolos.stream().limit(5).toList();

        int totalOcurrencias = simbolos.stream().mapToInt(TablaSimbolos.EntradaSimbolo::ocurrencias).sum();

        return new MetricasReporte(
                lineas,
                caracteres,
                tokensReales.size(),
                simbolos.size(),
                totalOcurrencias,
                errores.size(),
                reservadas,
                operadores,
                delimitadores,
                literales,
                conteoPorTipo,
                top5
        );
    }

    public static MetricasSintacticas calcularMetricasSintacticas(NodoAST ast, List<ErrorSintactico> erroresSintacticos) {
        int decl = 0;
        int ifs = 0;
        int prints = 0;
        int asigs = 0;
        int exps = 0;
        int total = 0;

        if (ast instanceof NodoAST.Programa prog) {
            total = prog.sentencias().size();
            for (NodoAST s : prog.sentencias()) {
                if (s instanceof NodoAST.DeclaracionVariable) decl++;
                else if (s instanceof NodoAST.SentenciaIf) ifs++;
                else if (s instanceof NodoAST.SentenciaImprimir) prints++;
                else if (s instanceof NodoAST.Asignacion) asigs++;
                else if (s instanceof NodoAST.SentenciaExpresion) exps++;
            }
        }

        int errSint = (erroresSintacticos != null) ? erroresSintacticos.size() : 0;
        return new MetricasSintacticas(total, decl, ifs, prints, asigs, exps, errSint);
    }

    public static String generarHTMLParaSwing(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                             TablaSimbolos tablaSimbolos, List<ErrorLexico> errores) {
        return generarHTMLParaSwing(tituloArchivo, codigoFuente, tokens, tablaSimbolos, errores, List.of(), null);
    }

    /**
     * Genera un reporte en HTML ligero compatible con JEditorPane que incluye métricas léxicas y sintácticas.
     */
    public static String generarHTMLParaSwing(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                             TablaSimbolos tablaSimbolos, List<ErrorLexico> errores,
                                             List<ErrorSintactico> erroresSintacticos, NodoAST ast) {
        MetricasReporte m = calcularMetricas(codigoFuente, tokens, tablaSimbolos, errores);
        MetricasSintacticas ms = calcularMetricasSintacticas(ast, erroresSintacticos);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        boolean tieneErroresLexicos = !errores.isEmpty();
        boolean tieneErroresSintacticos = erroresSintacticos != null && !erroresSintacticos.isEmpty();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: sans-serif; margin: 12px; color: #1e293b;'>");

        // Cabecera
        sb.append("<table width='100%' cellpadding='8' cellspacing='0' bgcolor='#f8fafc' style='border: 1px solid #cbd5e1;'>");
        sb.append("<tr><td>");
        sb.append("<h2 style='color: #1d4ed8; margin: 0 0 4px 0;'>Informe de Análisis Léxico y Sintáctico</h2>");
        sb.append("<font size='3' color='#64748b'><b>Archivo:</b> ").append(escaparHtml(tituloArchivo));
        sb.append(" | <b>Fecha:</b> ").append(fecha).append(" | <b>Generador:</b> JFlex 1.9.1 & JCUP 11b</font><br>");

        if (!tieneErroresLexicos && !tieneErroresSintacticos) {
            sb.append("<font color='#15803d'><b>ESTADO GLOBAL:</b> COMPILACIÓN EXITOSA (0 errores léxicos, 0 errores sintácticos)</font>");
        } else {
            sb.append("<font color='#b91c1c'><b>ESTADO GLOBAL:</b> DETECTADOS INCIDENTES (")
              .append(m.totalErrores()).append(" léxicos, ")
              .append(ms.totalErroresSintacticos()).append(" sintácticos)</font>");
        }
        sb.append("</td></tr></table><br>");

        // Módulo 1 y 2: Métricas Cuantitativas
        sb.append("<h3>1. Resumen Cuantitativo del Programa</h3>");
        sb.append("<table width='100%' cellpadding='6' cellspacing='0' border='1' bordercolor='#e2e8f0'>");
        sb.append("<tr bgcolor='#f1f5f9'><th>Métrica Léxica</th><th>Valor</th><th>Métrica Sintáctica (AST)</th><th>Valor</th></tr>");
        sb.append("<tr><td>Líneas de Código:</td><td><b>").append(m.totalLineas()).append("</b></td>");
        sb.append("<td>Total Sentencias Raíz:</td><td><b>").append(ms.totalSentencias()).append("</b></td></tr>");

        sb.append("<tr><td>Tokens Reconocidos:</td><td><b>").append(m.totalTokens()).append("</b></td>");
        sb.append("<td>Declaraciones de Variables:</td><td><b>").append(ms.declaracionesVariables()).append("</b></td></tr>");

        sb.append("<tr><td>Símbolos Únicos:</td><td><b>").append(m.totalIdentificadoresUnicos()).append("</b></td>");
        sb.append("<td>Instrucciones IF / ELSE:</td><td><b>").append(ms.sentenciasIf()).append("</b></td></tr>");

        sb.append("<tr><td>Palabras Reservadas:</td><td><b>").append(m.palabrasReservadas()).append("</b></td>");
        sb.append("<td>Instrucciones de Impresión:</td><td><b>").append(ms.sentenciasImpresion()).append("</b></td></tr>");

        sb.append("<tr><td>Errores Léxicos:</td><td><b>").append(tieneErroresLexicos ? "<font color='red'>" + m.totalErrores() + "</font>" : "<font color='green'>0</font>").append("</b></td>");
        sb.append("<td>Errores Sintácticos:</td><td><b>").append(tieneErroresSintacticos ? "<font color='red'>" + ms.totalErroresSintacticos() + "</font>" : "<font color='green'>0</font>").append("</b></td></tr>");
        sb.append("</table><br>");

        // Errores Léxicos
        if (tieneErroresLexicos) {
            sb.append("<h3 style='color: #b91c1c;'>2. Errores Léxicos Identificados</h3>");
            sb.append("<table width='100%' cellpadding='6' cellspacing='0' border='1' bordercolor='#fca5a5'>");
            sb.append("<tr bgcolor='#fee2e2'><th>#</th><th>Lexema Inválido</th><th>Línea</th><th>Columna</th><th>Descripción</th></tr>");
            int i = 1;
            for (ErrorLexico err : errores) {
                sb.append("<tr><td>").append(i++).append("</td>");
                sb.append("<td><font color='red'><b>").append(escaparHtml(err.lexemaInvalido())).append("</b></font></td>");
                sb.append("<td>").append(err.linea()).append("</td>");
                sb.append("<td>").append(err.columna()).append("</td>");
                sb.append("<td>").append(escaparHtml(err.descripcion())).append("</td></tr>");
            }
            sb.append("</table><br>");
        }

        // Errores Sintácticos
        if (tieneErroresSintacticos) {
            sb.append("<h3 style='color: #b91c1c;'>3. Errores Sintácticos Identificados (JCUP)</h3>");
            sb.append("<table width='100%' cellpadding='6' cellspacing='0' border='1' bordercolor='#fca5a5'>");
            sb.append("<tr bgcolor='#fee2e2'><th>#</th><th>Token Inesperado</th><th>Línea</th><th>Columna</th><th>Descripción</th></tr>");
            int j = 1;
            for (ErrorSintactico err : erroresSintacticos) {
                sb.append("<tr><td>").append(j++).append("</td>");
                sb.append("<td><font color='red'><b>").append(escaparHtml(err.tokenEncontrado())).append("</b></font></td>");
                sb.append("<td>").append(err.linea()).append("</td>");
                sb.append("<td>").append(err.columna()).append("</td>");
                sb.append("<td>").append(escaparHtml(err.descripcion())).append("</td></tr>");
            }
            sb.append("</table><br>");
        }

        // Diagnóstico Final
        sb.append("<h3>Diagnóstico Integral del Compilador</h3>");
        sb.append("<table width='100%' cellpadding='8' cellspacing='0' bgcolor='")
          .append((tieneErroresLexicos || tieneErroresSintacticos) ? "#fffbeb" : "#f0fdf4")
          .append("' style='border: 1px solid ")
          .append((tieneErroresLexicos || tieneErroresSintacticos) ? "#fde68a" : "#bbf7d0")
          .append(";'>");
        sb.append("<tr><td>");
        if (tieneErroresLexicos || tieneErroresSintacticos) {
            sb.append("<font color='#92400e'><b>Atención:</b> El código contiene incidentes gramaticales. Revise las tablas inferiores para ubicar línea y columna.</font>");
        } else {
            sb.append("<font color='#166534'><b>Excelente:</b> El código fuente cumple al 100% las especificaciones léxicas y la gramática LALR(1) definida en JCUP. El Árbol Sintáctico (AST) se generó correctamente.</font>");
        }
        sb.append("</td></tr></table>");

        sb.append("<br><font size='2' color='#94a3b8'>Puede consultar el árbol jerárquico en la pestaña 'Árbol Sintáctico (AST)' o exportar el reporte completo en HTML/Markdown.</font>");
        sb.append("</body></html>");
        return sb.toString();
    }

    public static String generarHTML(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                     TablaSimbolos tablaSimbolos, List<ErrorLexico> errores) {
        return generarHTML(tituloArchivo, codigoFuente, tokens, tablaSimbolos, errores, List.of(), null);
    }

    public static String generarHTML(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                     TablaSimbolos tablaSimbolos, List<ErrorLexico> errores,
                                     List<ErrorSintactico> erroresSintacticos, NodoAST ast) {
        MetricasReporte m = calcularMetricas(codigoFuente, tokens, tablaSimbolos, errores);
        MetricasSintacticas ms = calcularMetricasSintacticas(ast, erroresSintacticos);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        boolean tieneErroresLexicos = !errores.isEmpty();
        boolean tieneErroresSintacticos = erroresSintacticos != null && !erroresSintacticos.isEmpty();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>Reporte de Análisis Léxico y Sintáctico - ").append(escaparHtml(tituloArchivo)).append("</title>\n");
        sb.append("<style>\n");
        sb.append(":root { --primary: #2563eb; --primary-dark: #1d4ed8; --success: #16a34a; --danger: #dc2626; --warning: #d97706; --bg: #f8fafc; --card: #ffffff; --text: #1e293b; --border: #e2e8f0; }\n");
        sb.append("* { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n");
        sb.append("body { background: var(--bg); color: var(--text); padding: 30px; line-height: 1.5; }\n");
        sb.append(".container { max-width: 1200px; margin: 0 auto; }\n");
        sb.append(".header { background: var(--card); border-radius: 12px; padding: 25px; margin-bottom: 25px; border: 1px solid var(--border); box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }\n");
        sb.append(".header h1 { font-size: 26px; color: var(--primary); margin-bottom: 8px; }\n");
        sb.append(".header .meta { font-size: 14px; color: #64748b; display: flex; gap: 20px; flex-wrap: wrap; }\n");
        sb.append(".status-badge { display: inline-block; padding: 6px 14px; border-radius: 20px; font-weight: 600; font-size: 13px; text-transform: uppercase; margin-top: 10px; }\n");
        sb.append(".status-ok { background: #dcfce7; color: var(--success); border: 1px solid #86efac; }\n");
        sb.append(".status-err { background: #fee2e2; color: var(--danger); border: 1px solid #fca5a5; }\n");
        sb.append(".grid-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 25px; }\n");
        sb.append(".card { background: var(--card); border: 1px solid var(--border); border-radius: 10px; padding: 18px; box-shadow: 0 2px 4px rgba(0,0,0,0.03); }\n");
        sb.append(".card .title { font-size: 13px; font-weight: 600; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }\n");
        sb.append(".card .value { font-size: 28px; font-weight: 700; color: var(--primary-dark); margin-top: 5px; }\n");
        sb.append(".card.danger .value { color: var(--danger); }\n");
        sb.append(".section { background: var(--card); border-radius: 12px; border: 1px solid var(--border); padding: 22px; margin-bottom: 25px; box-shadow: 0 2px 4px rgba(0,0,0,0.03); }\n");
        sb.append(".section h2 { font-size: 18px; margin-bottom: 15px; padding-bottom: 8px; border-bottom: 2px solid var(--border); color: var(--text); }\n");
        sb.append("table { width: 100%; border-collapse: collapse; font-size: 14px; margin-top: 10px; }\n");
        sb.append("th, td { padding: 10px 14px; text-align: left; border-bottom: 1px solid var(--border); }\n");
        sb.append("th { background: #f1f5f9; color: #475569; font-weight: 600; text-transform: uppercase; font-size: 12px; letter-spacing: 0.5px; }\n");
        sb.append("tr:hover { background: #f8fafc; }\n");
        sb.append(".code-snippet { font-family: 'Consolas', 'Courier New', monospace; background: #0f172a; color: #f8fafc; padding: 15px; border-radius: 8px; overflow-x: auto; font-size: 13px; line-height: 1.6; max-height: 300px; }\n");
        sb.append(".tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; font-family: monospace; font-weight: 600; }\n");
        sb.append(".tag-kw { background: #e0e7ff; color: #4338ca; }\n");
        sb.append(".tag-id { background: #fef3c7; color: #b45309; }\n");
        sb.append(".tag-op { background: #f3e8ff; color: #7e22ce; }\n");
        sb.append(".tag-lit { background: #dbeafe; color: #1d4ed8; }\n");
        sb.append(".tag-err { background: #fee2e2; color: #b91c1c; font-weight: bold; }\n");
        sb.append(".callout { padding: 15px; border-radius: 8px; margin-top: 15px; font-size: 14px; }\n");
        sb.append(".callout-info { background: #eff6ff; border-left: 4px solid var(--primary); color: #1e40af; }\n");
        sb.append(".callout-warning { background: #fffbeb; border-left: 4px solid var(--warning); color: #92400e; }\n");
        sb.append("</style>\n</head>\n<body>\n");

        sb.append("<div class=\"container\">\n");

        // Encabezado
        sb.append("  <div class=\"header\">\n");
        sb.append("    <h1>Reporte Contextual de Análisis Léxico y Sintáctico</h1>\n");
        sb.append("    <div class=\"meta\">\n");
        sb.append("      <div><strong>Archivo/Origen:</strong> ").append(escaparHtml(tituloArchivo)).append("</div>\n");
        sb.append("      <div><strong>Fecha de Análisis:</strong> ").append(fecha).append("</div>\n");
        sb.append("      <div><strong>Herramientas:</strong> JFlex 1.9.1 & Java CUP 11b (Java 21 LTS)</div>\n");
        sb.append("    </div>\n");
        if (tieneErroresLexicos || tieneErroresSintacticos) {
            sb.append("    <div class=\"status-badge status-err\">Estado: Con Incidentes (")
              .append(m.totalErrores()).append(" Léxicos, ").append(ms.totalErroresSintacticos()).append(" Sintácticos)</div>\n");
        } else {
            sb.append("    <div class=\"status-badge status-ok\">Estado: Compilación Exitosa (0 Errores)</div>\n");
        }
        sb.append("  </div>\n");

        // Tarjetas
        sb.append("  <div class=\"grid-cards\">\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Tokens Reconocidos</div><div class=\"value\">").append(m.totalTokens()).append("</div></div>\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Sentencias AST</div><div class=\"value\">").append(ms.totalSentencias()).append("</div></div>\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Declaraciones Variables</div><div class=\"value\">").append(ms.declaracionesVariables()).append("</div></div>\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Instrucciones IF</div><div class=\"value\">").append(ms.sentenciasIf()).append("</div></div>\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Impresiones Pantalla</div><div class=\"value\">").append(ms.sentenciasImpresion()).append("</div></div>\n");
        sb.append("    <div class=\"card").append(tieneErroresLexicos || tieneErroresSintacticos ? " danger" : "").append("\"><div class=\"title\">Errores Totales</div><div class=\"value\">").append(m.totalErrores() + ms.totalErroresSintacticos()).append("</div></div>\n");
        sb.append("  </div>\n");

        // Errores Léxicos
        if (tieneErroresLexicos) {
            sb.append("  <div class=\"section\">\n");
            sb.append("    <h2 style=\"color: var(--danger);\">⚠️ Incidentes Léxicos Detectados</h2>\n");
            sb.append("    <table>\n");
            sb.append("      <thead><tr><th>#</th><th>Carácter Inválido</th><th>Línea</th><th>Columna</th><th>Descripción</th></tr></thead>\n");
            sb.append("      <tbody>\n");
            int errIdx = 1;
            for (ErrorLexico err : errores) {
                sb.append("        <tr>\n");
                sb.append("          <td>").append(errIdx++).append("</td>\n");
                sb.append("          <td><span class=\"tag tag-err\">").append(escaparHtml(err.lexemaInvalido())).append("</span></td>\n");
                sb.append("          <td>").append(err.linea()).append("</td>\n");
                sb.append("          <td>").append(err.columna()).append("</td>\n");
                sb.append("          <td>").append(escaparHtml(err.descripcion())).append("</td>\n");
                sb.append("        </tr>\n");
            }
            sb.append("      </tbody>\n");
            sb.append("    </table>\n");
            sb.append("  </div>\n");
        }

        // Errores Sintácticos
        if (tieneErroresSintacticos) {
            sb.append("  <div class=\"section\">\n");
            sb.append("    <h2 style=\"color: var(--danger);\">⚠️ Incidentes Sintácticos Detectados (JCUP)</h2>\n");
            sb.append("    <table>\n");
            sb.append("      <thead><tr><th>#</th><th>Token Inesperado</th><th>Línea</th><th>Columna</th><th>Descripción</th></tr></thead>\n");
            sb.append("      <tbody>\n");
            int errSIdx = 1;
            for (ErrorSintactico err : erroresSintacticos) {
                sb.append("        <tr>\n");
                sb.append("          <td>").append(errSIdx++).append("</td>\n");
                sb.append("          <td><span class=\"tag tag-err\">").append(escaparHtml(err.tokenEncontrado())).append("</span></td>\n");
                sb.append("          <td>").append(err.linea()).append("</td>\n");
                sb.append("          <td>").append(err.columna()).append("</td>\n");
                sb.append("          <td>").append(escaparHtml(err.descripcion())).append("</td>\n");
                sb.append("        </tr>\n");
            }
            sb.append("      </tbody>\n");
            sb.append("    </table>\n");
            sb.append("  </div>\n");
        }

        // Distribución Léxica
        sb.append("  <div class=\"section\">\n");
        sb.append("    <h2>Distribución y Clasificación de Tokens</h2>\n");
        sb.append("    <table>\n");
        sb.append("      <thead><tr><th>Categoría</th><th>Cantidad</th><th>Porcentaje</th><th>Descripción</th></tr></thead>\n");
        sb.append("      <tbody>\n");
        int totalT = Math.max(1, m.totalTokens());
        agregarFilaDistribucion(sb, "Palabras Reservadas", m.palabrasReservadas(), totalT, "Palabras clave del lenguaje (let, const, function, if, print...)");
        agregarFilaDistribucion(sb, "Identificadores", m.totalOcurrenciasIdentificadores(), totalT, "Nombres de variables, funciones y referencias");
        agregarFilaDistribucion(sb, "Operadores", m.operadores(), totalT, "Operadores aritméticos, relacionales, lógicos y asignación");
        agregarFilaDistribucion(sb, "Delimitadores", m.delimitadores(), totalT, "Signos de puntuación, paréntesis, llaves, corchetes y punto y coma");
        agregarFilaDistribucion(sb, "Literales", m.literales(), totalT, "Números enteros, flotantes, notación científica y cadenas de texto");
        sb.append("      </tbody>\n");
        sb.append("    </table>\n");
        sb.append("  </div>\n");

        // Tabla de Símbolos
        sb.append("  <div class=\"section\">\n");
        sb.append("    <h2>Tabla de Símbolos (Identificadores del Programa)</h2>\n");
        var simbolos = tablaSimbolos.obtenerTodos();
        if (simbolos.isEmpty()) {
            sb.append("    <p><em>No se registraron identificadores en el código analizado.</em></p>\n");
        } else {
            sb.append("    <table>\n");
            sb.append("      <thead><tr><th>Identificador</th><th>Categoría</th><th>1ra Línea</th><th>1ra Columna</th><th>Ocurrencias</th></tr></thead>\n");
            sb.append("      <tbody>\n");
            for (var s : simbolos) {
                sb.append("        <tr>\n");
                sb.append("          <td><strong>").append(escaparHtml(s.nombre())).append("</strong></td>\n");
                sb.append("          <td><span class=\"tag tag-id\">").append(escaparHtml(s.categoria())).append("</span></td>\n");
                sb.append("          <td>").append(s.primeraLinea()).append("</td>\n");
                sb.append("          <td>").append(s.primeraColumna()).append("</td>\n");
                sb.append("          <td>").append(s.ocurrencias()).append("</td>\n");
                sb.append("        </tr>\n");
            }
            sb.append("      </tbody>\n");
            sb.append("    </table>\n");
        }
        sb.append("  </div>\n");

        // Estructura del AST si existe
        if (ast != null) {
            sb.append("  <div class=\"section\">\n");
            sb.append("    <h2>Representación Textual del Árbol Sintáctico (AST)</h2>\n");
            sb.append("    <pre class=\"code-snippet\"><code>").append(escaparHtml(formatearAST(ast, 0))).append("</code></pre>\n");
            sb.append("  </div>\n");
        }

        // Flujo Completo de Tokens
        sb.append("  <div class=\"section\">\n");
        sb.append("    <h2>Flujo Completo de Tokens Reconocidos</h2>\n");
        sb.append("    <table>\n");
        sb.append("      <thead><tr><th>#</th><th>Tipo de Token</th><th>Lexema</th><th>Línea</th><th>Columna</th><th>Valor Literal</th></tr></thead>\n");
        sb.append("      <tbody>\n");
        int tIdx = 1;
        for (Token t : tokens) {
            sb.append("        <tr>\n");
            sb.append("          <td>").append(tIdx++).append("</td>\n");
            sb.append("          <td><span class=\"tag ").append(obtenerClaseTag(t.tipo())).append("\">").append(t.tipo()).append("</span></td>\n");
            sb.append("          <td><code>").append(escaparHtml(t.lexema())).append("</code></td>\n");
            sb.append("          <td>").append(t.linea()).append("</td>\n");
            sb.append("          <td>").append(t.columna()).append("</td>\n");
            sb.append("          <td>").append(t.valorLiteral() != null ? escaparHtml(t.valorLiteral().toString()) : "-").append("</td>\n");
            sb.append("        </tr>\n");
        }
        sb.append("      </tbody>\n");
        sb.append("    </table>\n");
        sb.append("  </div>\n");

        // Código Fuente
        sb.append("  <div class=\"section\">\n");
        sb.append("    <h2>Código Fuente Analizado</h2>\n");
        sb.append("    <pre class=\"code-snippet\"><code>").append(escaparHtml(codigoFuente)).append("</code></pre>\n");
        sb.append("  </div>\n");

        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    public static String generarMarkdown(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                         TablaSimbolos tablaSimbolos, List<ErrorLexico> errores) {
        return generarMarkdown(tituloArchivo, codigoFuente, tokens, tablaSimbolos, errores, List.of(), null);
    }

    public static String generarMarkdown(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                         TablaSimbolos tablaSimbolos, List<ErrorLexico> errores,
                                         List<ErrorSintactico> erroresSintacticos, NodoAST ast) {
        MetricasReporte m = calcularMetricas(codigoFuente, tokens, tablaSimbolos, errores);
        MetricasSintacticas ms = calcularMetricasSintacticas(ast, erroresSintacticos);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        boolean tieneErroresLexicos = !errores.isEmpty();
        boolean tieneErroresSintacticos = erroresSintacticos != null && !erroresSintacticos.isEmpty();

        StringBuilder sb = new StringBuilder();
        if (ast != null || tieneErroresSintacticos) {
            sb.append("# Reporte de Análisis Léxico y Sintáctico - JavaScript\n\n");
        } else {
            sb.append("# Reporte de Análisis Léxico - JavaScript\n\n");
        }
        sb.append("**Archivo/Origen:** `").append(tituloArchivo).append("`  \n");
        sb.append("**Fecha de Generación:** ").append(fecha).append("  \n");
        sb.append("**Estado:** ").append(tieneErroresLexicos || tieneErroresSintacticos
                ? "⚠️ CON INCIDENTES (" + (m.totalErrores() + ms.totalErroresSintacticos()) + ")"
                : "✅ COMPILACIÓN EXITOSA").append("  \n");
        sb.append("**Herramientas:** JFlex 1.9.1 / JCUP 11b / Java 21 (LTS)\n\n");
        sb.append("---\n\n");

        sb.append("## 1. Resumen Estadístico Integral\n\n");
        sb.append("| Métrica | Valor |\n");
        sb.append("|---|---:|\n");
        sb.append("| Total de Líneas de Código | ").append(m.totalLineas()).append(" |\n");
        sb.append("| Total de Caracteres | ").append(m.totalCaracteres()).append(" |\n");
        sb.append("| Total de Tokens Reconocidos | ").append(m.totalTokens()).append(" |\n");
        sb.append("| Identificadores Únicos en Tabla de Símbolos | ").append(m.totalIdentificadoresUnicos()).append(" |\n");
        sb.append("| Total Sentencias Sintácticas (AST) | ").append(ms.totalSentencias()).append(" |\n");
        sb.append("| Declaraciones de Variables (`let`, `const`, `var`) | ").append(ms.declaracionesVariables()).append(" |\n");
        sb.append("| Sentencias Condicionales `IF` / `ELSE` | ").append(ms.sentenciasIf()).append(" |\n");
        sb.append("| Instrucciones de Impresión (`console.log`, `print`) | ").append(ms.sentenciasImpresion()).append(" |\n");
        sb.append("| Errores Léxicos Detectados | **").append(m.totalErrores()).append("** |\n");
        sb.append("| Errores Sintácticos Detectados (JCUP) | **").append(ms.totalErroresSintacticos()).append("** |\n\n");

        if (tieneErroresLexicos) {
            sb.append("## 2. Errores Léxicos Detectados\n\n");
            sb.append("| # | Lexema Inválido | Línea | Columna | Descripción |\n");
            sb.append("|---|---|---:|---:|---|\n");
            int eIdx = 1;
            for (ErrorLexico err : errores) {
                sb.append("| ").append(eIdx++).append(" | `").append(err.lexemaInvalido()).append("` | ")
                  .append(err.linea()).append(" | ").append(err.columna()).append(" | ")
                  .append(err.descripcion()).append(" |\n");
            }
            sb.append("\n");
        }

        if (tieneErroresSintacticos) {
            sb.append("### Errores Sintácticos Detectados (JCUP)\n\n");
            sb.append("| # | Token Inesperado | Línea | Columna | Descripción |\n");
            sb.append("|---|---|---:|---:|---|\n");
            int esIdx = 1;
            for (ErrorSintactico err : erroresSintacticos) {
                sb.append("| ").append(esIdx++).append(" | `").append(err.tokenEncontrado()).append("` | ")
                  .append(err.linea()).append(" | ").append(err.columna()).append(" | ")
                  .append(err.descripcion()).append(" |\n");
            }
            sb.append("\n");
        }

        sb.append("## 3. Tabla de Símbolos\n\n");

        if (ast != null) {
            sb.append("## 4. Estructura Jerárquica del AST\n\n");
            sb.append("```text\n").append(formatearAST(ast, 0)).append("\n```\n\n");
        }

        sb.append("## 5. Tabla de Símbolos\n\n");
        var simbolos = tablaSimbolos.obtenerTodos();
        if (simbolos.isEmpty()) {
            sb.append("_No se registraron identificadores._\n\n");
        } else {
            sb.append("| Identificador | Categoría | 1ra Línea | 1ra Columna | Ocurrencias |\n");
            sb.append("|---|---|---:|---:|---:|\n");
            for (var s : simbolos) {
                sb.append("| `").append(s.nombre()).append("` | ").append(s.categoria()).append(" | ")
                  .append(s.primeraLinea()).append(" | ").append(s.primeraColumna()).append(" | ")
                  .append(s.ocurrencias()).append(" |\n");
            }
            sb.append("\n");
        }

        sb.append("## 6. Código Fuente Analizado\n\n");
        sb.append("```javascript\n").append(codigoFuente).append("\n```\n\n");

        sb.append("---\n*Reporte generado automáticamente por Analizador Léxico y Sintáctico JS (Java 21)*\n");
        return sb.toString();
    }

    public static void guardarEnArchivo(File archivoDestino, String contenido) throws IOException {
        Files.writeString(archivoDestino.toPath(), contenido, StandardCharsets.UTF_8);
    }

    private static String formatearAST(NodoAST nodo, int nivel) {
        if (nodo == null) return "";
        String indent = "  ".repeat(nivel);
        StringBuilder sb = new StringBuilder();

        if (nodo instanceof NodoAST.Programa prog) {
            sb.append(indent).append("Programa (").append(prog.sentencias().size()).append(" sentencias)\n");
            for (NodoAST s : prog.sentencias()) {
                sb.append(formatearAST(s, nivel + 1));
            }
        } else if (nodo instanceof NodoAST.DeclaracionVariable decl) {
            sb.append(indent).append("DeclaracionVariable [").append(decl.tipoDeclaracion()).append(" ").append(decl.identificador()).append("]\n");
            if (decl.valorInicial() != null) {
                sb.append(indent).append("  └── ValorInicial:\n");
                sb.append(formatearAST(decl.valorInicial(), nivel + 2));
            }
        } else if (nodo instanceof NodoAST.Asignacion asig) {
            sb.append(indent).append("Asignacion [").append(asig.identificador()).append(" = ]\n");
            if (asig.valor() != null) {
                sb.append(formatearAST(asig.valor(), nivel + 1));
            }
        } else if (nodo instanceof NodoAST.SentenciaIf sif) {
            sb.append(indent).append("SentenciaIf\n");
            sb.append(indent).append("  ├── Condicion:\n");
            sb.append(formatearAST(sif.condicion(), nivel + 2));
            sb.append(indent).append("  ├── RamaThen:\n");
            sb.append(formatearAST(sif.bloqueThen(), nivel + 2));
            if (sif.bloqueElse() != null) {
                sb.append(indent).append("  └── RamaElse:\n");
                sb.append(formatearAST(sif.bloqueElse(), nivel + 2));
            }
        } else if (nodo instanceof NodoAST.SentenciaImprimir simp) {
            sb.append(indent).append("SentenciaImprimir [").append(simp.funcion()).append("] (").append(simp.argumentos().size()).append(" args)\n");
            for (NodoAST arg : simp.argumentos()) {
                sb.append(formatearAST(arg, nivel + 1));
            }
        } else if (nodo instanceof NodoAST.Bloque blk) {
            sb.append(indent).append("Bloque { ... } (").append(blk.sentencias().size()).append(" sentencias)\n");
            for (NodoAST s : blk.sentencias()) {
                sb.append(formatearAST(s, nivel + 1));
            }
        } else if (nodo instanceof NodoAST.ExpresionBinaria bin) {
            sb.append(indent).append("ExpresionBinaria (").append(bin.operador()).append(")\n");
            sb.append(formatearAST(bin.izquierda(), nivel + 1));
            sb.append(formatearAST(bin.derecha(), nivel + 1));
        } else if (nodo instanceof NodoAST.ExpresionUnaria un) {
            sb.append(indent).append("ExpresionUnaria (").append(un.operador()).append(")\n");
            sb.append(formatearAST(un.expresion(), nivel + 1));
        } else if (nodo instanceof NodoAST.Literal lit) {
            sb.append(indent).append("Literal (").append(lit.tipo()).append("): ").append(lit.valor()).append("\n");
        } else if (nodo instanceof NodoAST.Identificador id) {
            sb.append(indent).append("Identificador: ").append(id.nombre()).append("\n");
        } else if (nodo instanceof NodoAST.SentenciaExpresion sexp) {
            sb.append(indent).append("SentenciaExpresion:\n");
            sb.append(formatearAST(sexp.expresion(), nivel + 1));
        }
        return sb.toString();
    }

    private static void agregarFilaDistribucion(StringBuilder sb, String categoria, int cant, int total, String desc) {
        double pct = (cant * 100.0) / total;
        sb.append("<tr>\n");
        sb.append("  <td><strong>").append(categoria).append("</strong></td>\n");
        sb.append("  <td>").append(cant).append("</td>\n");
        sb.append("  <td>").append(String.format(Locale.US, "%.1f%%", pct)).append("</td>\n");
        sb.append("  <td><span style=\"color: #64748b;\">").append(desc).append("</span></td>\n");
        sb.append("</tr>\n");
    }

    private static String obtenerClaseTag(TokenType tipo) {
        return switch (tipo) {
            case LET, CONST, VAR, FUNCTION, IF, ELSE, WHILE, FOR, RETURN, TRUE, FALSE, NULL,
                 PRINT, CONSOLE, LOG -> "tag-kw";
            case ID -> "tag-id";
            case MAS, MENOS, MULT, DIV, MOD, ASIGNACION, INCREMENTO, DECREMENTO,
                 IGUAL_IGUAL, IGUAL_ESTRICTO, DIFERENTE, DIFERENTE_ESTRICTO,
                 MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL, AND, OR, NOT -> "tag-op";
            case LIT_ENTERO, LIT_FLOTANTE, LIT_CADENA, LIT_TEMPLATE -> "tag-lit";
            default -> "";
        };
    }

    private static String escaparHtml(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
