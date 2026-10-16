package com.analizador.reporte;

import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generador de reportes contextualizados del análisis léxico en formatos HTML y Markdown.
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
                case LET, CONST, VAR, FUNCTION, IF, ELSE, WHILE, FOR, RETURN, TRUE, FALSE, NULL -> reservadas++;
                case MAS, MENOS, MULT, DIV, MOD, ASIGNACION, INCREMENTO, DECREMENTO,
                     IGUAL_IGUAL, IGUAL_ESTRICTO, DIFERENTE, DIFERENTE_ESTRICTO,
                     MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL, AND, OR, NOT -> operadores++;
                case PAREN_IZQ, PAREN_DER, LLAVE_IZQ, LLAVE_DER, CORCHETE_IZQ, CORCHETE_DER,
                     PUNTO_COMA, COMA, DOS_PUNTOS, PUNTO -> delimitadores++;
                case LIT_ENTERO, LIT_FLOTANTE, LIT_CADENA, LIT_TEMPLATE -> literales++;
                default -> {}
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

    /**
     * Genera un reporte en HTML 3.2 ligero y compatible con el componente JEditorPane de Java Swing.
     */
    public static String generarHTMLParaSwing(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                             TablaSimbolos tablaSimbolos, List<ErrorLexico> errores) {
        MetricasReporte m = calcularMetricas(codigoFuente, tokens, tablaSimbolos, errores);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        boolean tieneErrores = !errores.isEmpty();

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: sans-serif; margin: 12px; color: #1e293b;'>");

        sb.append("<table width='100%' cellpadding='8' cellspacing='0' bgcolor='#f8fafc' style='border: 1px solid #cbd5e1;'>");
        sb.append("<tr><td>");
        sb.append("<h2 style='color: #1d4ed8; margin: 0 0 4px 0;'>Informe de Análisis Léxico</h2>");
        sb.append("<font size='3' color='#64748b'><b>Archivo:</b> ").append(escaparHtml(tituloArchivo));
        sb.append(" | <b>Fecha:</b> ").append(fecha).append("</font><br>");
        if (tieneErrores) {
            sb.append("<font color='#b91c1c'><b>ESTADO:</b> CON ERRORES LÉXICOS (").append(m.totalErrores()).append(" detectados)</font>");
        } else {
            sb.append("<font color='#15803d'><b>ESTADO:</b> ANÁLISIS LÉXICO EXITOSO (0 errores)</font>");
        }
        sb.append("</td></tr></table><br>");

        sb.append("<h3>1. Resumen Cuantitativo</h3>");
        sb.append("<table width='100%' cellpadding='6' cellspacing='0' border='1' bordercolor='#e2e8f0'>");
        sb.append("<tr bgcolor='#f1f5f9'><th>Métrica</th><th>Valor</th><th>Categoría de Tokens</th><th>Cantidad</th></tr>");
        sb.append("<tr><td>Líneas de Código:</td><td><b>").append(m.totalLineas()).append("</b></td><td>Palabras Reservadas:</td><td><b>").append(m.palabrasReservadas()).append("</b></td></tr>");
        sb.append("<tr><td>Caracteres Totales:</td><td><b>").append(m.totalCaracteres()).append("</b></td><td>Identificadores Totales:</td><td><b>").append(m.totalOcurrenciasIdentificadores()).append("</b></td></tr>");
        sb.append("<tr><td>Tokens Reconocidos:</td><td><b>").append(m.totalTokens()).append("</b></td><td>Operadores:</td><td><b>").append(m.operadores()).append("</b></td></tr>");
        sb.append("<tr><td>Símbolos Únicos:</td><td><b>").append(m.totalIdentificadoresUnicos()).append("</b></td><td>Delimitadores:</td><td><b>").append(m.delimitadores()).append("</b></td></tr>");
        sb.append("<tr><td>Errores Léxicos:</td><td><b>").append(tieneErrores ? "<font color='red'>" + m.totalErrores() + "</font>" : "<font color='green'>0</font>").append("</b></td><td>Literales:</td><td><b>").append(m.literales()).append("</b></td></tr>");
        sb.append("</table><br>");

        if (tieneErrores) {
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

        sb.append("<h3>").append(tieneErrores ? "3" : "2").append(". Diagnóstico para la Fase Sintáctica</h3>");
        sb.append("<table width='100%' cellpadding='8' cellspacing='0' bgcolor='").append(tieneErrores ? "#fffbeb" : "#f0fdf4").append("' style='border: 1px solid ").append(tieneErrores ? "#fde68a" : "#bbf7d0").append(";'>");
        sb.append("<tr><td>");
        if (tieneErrores) {
            sb.append("<font color='#92400e'><b>Atención:</b> Existen caracteres fuera del alfabeto. Corrija los errores léxicos para que el flujo de tokens sea consumido limpiamente por el Parser sintáctico.</font>");
        } else {
            sb.append("<font color='#166534'><b>Excelente:</b> El código fuente fue tokenizado en su totalidad sin infracciones al alfabeto léxico. El flujo de ").append(m.totalTokens()).append(" tokens se encuentra listo para el análisis sintáctico (Parser LL(1)).</font>");
        }
        sb.append("</td></tr></table>");

        sb.append("<br><font size='2' color='#94a3b8'>Haga clic en 'Exportar Reporte' en la barra superior para guardar el informe completo con gráficos y código en formato HTML o Markdown.</font>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * Genera un reporte completo en HTML5 moderno con estilos CSS incrustados.
     */
    public static String generarHTML(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                     TablaSimbolos tablaSimbolos, List<ErrorLexico> errores) {
        MetricasReporte m = calcularMetricas(codigoFuente, tokens, tablaSimbolos, errores);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        boolean tieneErrores = !errores.isEmpty();

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"es\">\n<head>\n");
        sb.append("<meta charset=\"UTF-8\">\n");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("<title>Reporte de Análisis Léxico - ").append(escaparHtml(tituloArchivo)).append("</title>\n");
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
        sb.append("    <h1>Reporte Contextual de Análisis Léxico</h1>\n");
        sb.append("    <div class=\"meta\">\n");
        sb.append("      <div><strong>Archivo/Origen:</strong> ").append(escaparHtml(tituloArchivo)).append("</div>\n");
        sb.append("      <div><strong>Fecha de Análisis:</strong> ").append(fecha).append("</div>\n");
        sb.append("      <div><strong>Motor Léxico:</strong> JFlex 1.9.1 (Java 21 LTS)</div>\n");
        sb.append("    </div>\n");
        if (tieneErrores) {
            sb.append("    <div class=\"status-badge status-err\">Estado: Con Errores Léxicos (").append(m.totalErrores()).append(")</div>\n");
        } else {
            sb.append("    <div class=\"status-badge status-ok\">Estado: Análisis Léxico Exitoso (0 Errores)</div>\n");
        }
        sb.append("  </div>\n");

        // Métricas en tarjetas
        sb.append("  <div class=\"grid-cards\">\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Tokens Reconocidos</div><div class=\"value\">").append(m.totalTokens()).append("</div></div>\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Identificadores Únicos</div><div class=\"value\">").append(m.totalIdentificadoresUnicos()).append("</div></div>\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Líneas de Código</div><div class=\"value\">").append(m.totalLineas()).append("</div></div>\n");
        sb.append("    <div class=\"card\"><div class=\"title\">Palabras Reservadas</div><div class=\"value\">").append(m.palabrasReservadas()).append("</div></div>\n");
        sb.append("    <div class=\"card").append(tieneErrores ? " danger" : "").append("\"><div class=\"title\">Errores Léxicos</div><div class=\"value\">").append(m.totalErrores()).append("</div></div>\n");
        sb.append("  </div>\n");

        // Sección de Errores (si los hay)
        if (tieneErrores) {
            sb.append("  <div class=\"section\">\n");
            sb.append("    <h2 style=\"color: var(--danger);\">⚠️ Incidentes Léxicos Detectados</h2>\n");
            sb.append("    <p>Se detectaron caracteres o secuencias que no forman parte del alfabeto del lenguaje JavaScript:</p>\n");
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
            sb.append("    <div class=\"callout callout-warning\">\n");
            sb.append("      <strong>Nota para la siguiente fase:</strong> El analizador sintáctico (Parser) requiere que estos símbolos sean corregidos para construir el árbol de sintaxis abstracta (AST) sin fallos estructurales.\n");
            sb.append("    </div>\n");
            sb.append("  </div>\n");
        }

        // Distribución léxica
        sb.append("  <div class=\"section\">\n");
        sb.append("    <h2>Distribución y Clasificación de Tokens</h2>\n");
        sb.append("    <table>\n");
        sb.append("      <thead><tr><th>Categoría</th><th>Cantidad</th><th>Porcentaje</th><th>Descripción</th></tr></thead>\n");
        sb.append("      <tbody>\n");
        int totalT = Math.max(1, m.totalTokens());
        agregarFilaDistribucion(sb, "Palabras Reservadas", m.palabrasReservadas(), totalT, "Palabras clave del lenguaje (let, const, function, if, while...)");
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

        // Código Fuente Analizado
        sb.append("  <div class=\"section\">\n");
        sb.append("    <h2>Código Fuente Analizado</h2>\n");
        sb.append("    <pre class=\"code-snippet\"><code>").append(escaparHtml(codigoFuente)).append("</code></pre>\n");
        sb.append("  </div>\n");

        // Conclusión del análisis
        sb.append("  <div class=\"callout callout-info\">\n");
        sb.append("    <strong>Diagnóstico del Nivel Léxico:</strong> ");
        if (tieneErrores) {
            sb.append("El código contiene ").append(m.totalErrores()).append(" símbolo(s) ilegales que deben corregirse. La fase léxica completó la recuperación y aisló los componentes gramaticales restantes.");
        } else {
            sb.append("El archivo presenta consistencia léxica total. Se reconocieron ").append(m.totalTokens()).append(" tokens válidos y ").append(m.totalIdentificadoresUnicos()).append(" identificadores únicos, encontrándose apto para la fase sintáctica (Parser).");
        }
        sb.append("  </div>\n");

        sb.append("</div>\n</body>\n</html>");
        return sb.toString();
    }

    /**
     * Genera un reporte estructurado en formato Markdown (.md).
     */
    public static String generarMarkdown(String tituloArchivo, String codigoFuente, List<Token> tokens,
                                         TablaSimbolos tablaSimbolos, List<ErrorLexico> errores) {
        MetricasReporte m = calcularMetricas(codigoFuente, tokens, tablaSimbolos, errores);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        boolean tieneErrores = !errores.isEmpty();

        StringBuilder sb = new StringBuilder();
        sb.append("# Reporte de Análisis Léxico - JavaScript\n\n");
        sb.append("**Archivo/Origen:** `").append(tituloArchivo).append("`  \n");
        sb.append("**Fecha de Generación:** ").append(fecha).append("  \n");
        sb.append("**Estado:** ").append(tieneErrores ? "⚠️ CON ERRORES LÉXICOS (" + m.totalErrores() + ")" : "✅ ANÁLISIS LÉXICO EXITOSO").append("  \n");
        sb.append("**Herramienta:** JFlex 1.9.1 / Java 21 (LTS)\n\n");
        sb.append("---\n\n");

        sb.append("## 1. Resumen Estadístico\n\n");
        sb.append("| Métrica | Valor |\n");
        sb.append("|---|---:|\n");
        sb.append("| Total de Líneas de Código | ").append(m.totalLineas()).append(" |\n");
        sb.append("| Total de Caracteres | ").append(m.totalCaracteres()).append(" |\n");
        sb.append("| Total de Tokens Reconocidos | ").append(m.totalTokens()).append(" |\n");
        sb.append("| Identificadores Únicos en Tabla de Símbolos | ").append(m.totalIdentificadoresUnicos()).append(" |\n");
        sb.append("| Ocurrencias Totales de Identificadores | ").append(m.totalOcurrenciasIdentificadores()).append(" |\n");
        sb.append("| Palabras Reservadas Identificadas | ").append(m.palabrasReservadas()).append(" |\n");
        sb.append("| Operadores (Aritméticos, Lógicos, Relacionales) | ").append(m.operadores()).append(" |\n");
        sb.append("| Delimitadores y Puntuación | ").append(m.delimitadores()).append(" |\n");
        sb.append("| Literales (Números, Cadenas, Booleanos) | ").append(m.literales()).append(" |\n");
        sb.append("| Errores Léxicos Detectados | **").append(m.totalErrores()).append("** |\n\n");

        if (tieneErrores) {
            sb.append("## 2. Errores Léxicos Detectados\n\n");
            sb.append("| # | Lexema Inválido | Línea | Columna | Descripción |\n");
            sb.append("|---|---|---:|---:|---|\n");
            int eIdx = 1;
            for (ErrorLexico err : errores) {
                sb.append("| ").append(eIdx++).append(" | `").append(err.lexemaInvalido()).append("` | ")
                  .append(err.linea()).append(" | ").append(err.columna()).append(" | ")
                  .append(err.descripcion()).append(" |\n");
            }
            sb.append("\n> [!WARNING]\n");
            sb.append("> Los símbolos listados no forman parte del alfabeto regular de JavaScript. Deben corregirse antes de proceder a la fase de análisis sintáctico.\n\n");
        }

        sb.append("## 3. Tabla de Símbolos\n\n");
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

        sb.append("## 4. Flujo Detallado de Tokens\n\n");
        sb.append("| # | Token | Lexema | Línea | Columna | Valor Literal |\n");
        sb.append("|---|---|---|---:|---:|---|\n");
        int tokIdx = 1;
        for (Token t : tokens) {
            sb.append("| ").append(tokIdx++).append(" | `").append(t.tipo()).append("` | `")
              .append(t.lexema().replace("`", "\\`")).append("` | ").append(t.linea()).append(" | ")
              .append(t.columna()).append(" | ").append(t.valorLiteral() != null ? t.valorLiteral() : "-").append(" |\n");
        }
        sb.append("\n");

        sb.append("## 5. Código Fuente Analizado\n\n");
        sb.append("```javascript\n").append(codigoFuente).append("\n```\n\n");

        sb.append("---\n*Reporte generado automáticamente por Analizador Léxico JS (Java 21)*\n");
        return sb.toString();
    }

    /**
     * Guarda el reporte generado en el archivo destino especificado.
     */
    public static void guardarEnArchivo(File archivoDestino, String contenido) throws IOException {
        Files.writeString(archivoDestino.toPath(), contenido, StandardCharsets.UTF_8);
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
            case LET, CONST, VAR, FUNCTION, IF, ELSE, WHILE, FOR, RETURN, TRUE, FALSE, NULL -> "tag-kw";
            case ID -> "tag-id";
            case LIT_ENTERO, LIT_FLOTANTE, LIT_CADENA, LIT_TEMPLATE -> "tag-lit";
            case MAS, MENOS, MULT, DIV, MOD, ASIGNACION, INCREMENTO, DECREMENTO,
                 IGUAL_IGUAL, IGUAL_ESTRICTO, DIFERENTE, DIFERENTE_ESTRICTO,
                 MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL, AND, OR, NOT -> "tag-op";
            default -> "";
        };
    }

    private static String escaparHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
