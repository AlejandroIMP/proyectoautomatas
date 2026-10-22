package com.analizador;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import com.analizador.servicio.CompiladorJS;
import com.analizador.sintactico.AnalizadorSintacticoJS;
import com.analizador.sintactico.NodoAST;
import com.analizador.sintactico.TokenStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite de pruebas y blindaje según plan.md:
 * "PLAN DE PRUEBAS Y BLINDAJE (A PRUEBA DE BALAS) PARA PRESENTACION DE ANALIZADOR LEXICO Y SINTACTICO".
 * Cubre las 5 fases de comprobación exhaustiva para defender el proyecto ante cualquier evaluación.
 */
@DisplayName("Plan de Pruebas y Blindaje - A Prueba de Balas (plan.md)")
public class PlanPruebasBlindajeTest {

    // =========================================================================
    // FASE 1: CASOS LÍMITE DEL AUTÓMATA (ENTRADAS EXTREMAS)
    // =========================================================================
    @Nested
    @DisplayName("Fase 1: Casos Límite del Autómata")
    class Fase1CasosLimite {

        @Test
        @DisplayName("1.1. Prueba de cadena vacía (épsilon) y nula")
        void test1_1_CadenaVacia() {
            // Caso cadena vacía ""
            CompiladorJS.ResultadoCompilacion resVacio = CompiladorJS.compilar("");
            assertNotNull(resVacio, "El resultado de compilar cadena vacía no debe ser nulo");
            assertNotNull(resVacio.astRaiz(), "El AST de cadena vacía debe ser un Programa válido");
            assertInstanceOf(NodoAST.Programa.class, resVacio.astRaiz());
            NodoAST.Programa progVacio = (NodoAST.Programa) resVacio.astRaiz();
            assertTrue(progVacio.sentencias().isEmpty(), "No debe contener sentencias");
            assertFalse(resVacio.tieneErroresLexicos(), "No deben existir errores léxicos");
            assertFalse(resVacio.tieneErroresSintacticos(), "No deben existir errores sintácticos");

            // Caso nulo
            CompiladorJS.ResultadoCompilacion resNull = CompiladorJS.compilar(null);
            assertNotNull(resNull);
            assertFalse(resNull.tieneErroresLexicos());
            assertFalse(resNull.tieneErroresSintacticos());
        }

        @Test
        @DisplayName("1.2. Prueba de espacios y comentarios puros")
        void test1_2_EspaciosYComentariosPuros() {
            String codigo = """
                       \t
                    // Primer comentario de una sola línea
                        /*
                         * Comentario multilínea de bloque
                         * con caracteres especiales: !@#$%^&*()_+{}[]
                         */
                    \r\n\t
                    // Otro comentario final
                    """;

            CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(codigo);
            assertTrue(res.esExitoso(), "Debe completarse limpiamente");
            assertTrue(res.erroresLexicos().isEmpty(), "Los comentarios deben filtrarse sin dejar errores léxicos");
            assertTrue(res.erroresSintacticos().isEmpty(), "No deben existir errores sintácticos");

            // Todos los tokens generados antes de EOF deben ser filtrados
            List<Token> noEofTokens = res.tokens().stream()
                    .filter(t -> t.tipo() != TokenType.EOF)
                    .toList();
            assertTrue(noEofTokens.isEmpty(), "No debe haber tokens residuales además de EOF");

            NodoAST.Programa prog = (NodoAST.Programa) res.astRaiz();
            assertTrue(prog.sentencias().isEmpty(), "El programa con solo comentarios debe tener 0 sentencias");
        }

        @Test
        @DisplayName("1.3. Prueba de un solo símbolo (válido e inválido)")
        void test1_3_UnSoloSimboloValidoEInvalido() {
            // Símbolo válido: ";" (sentencia vacía)
            CompiladorJS.ResultadoCompilacion resPuntoYComa = CompiladorJS.compilar(";");
            assertFalse(resPuntoYComa.tieneErroresLexicos(), "';' es un símbolo léxico válido");
            assertFalse(resPuntoYComa.tieneErroresSintacticos(), "';' es una sentencia vacía permitida");
            assertNotNull(resPuntoYComa.astRaiz());

            // Símbolo inválido: "@"
            CompiladorJS.ResultadoCompilacion resArroba = CompiladorJS.compilar("@");
            assertTrue(resArroba.tieneErroresLexicos(), "Debe detectar '@' como símbolo no perteneciente al alfabeto");
            assertEquals("@", resArroba.erroresLexicos().get(0).lexemaInvalido());
            // No debe cerrar ni explotar el compilador
            assertNotNull(resArroba.tablaSimbolos());
        }

        @Test
        @DisplayName("1.4. Prueba de script con todas las palabras clave/operadores y archivo extenso (500+ líneas)")
        void test1_4_TodosLosSimbolosYCadenaMuyLarga() {
            StringBuilder sb = new StringBuilder();
            sb.append("// Inicio de prueba de estrés sintáctico y léxico\n");

            // Generar más de 500 líneas con variedad de sentencias válidas
            for (int i = 0; i < 250; i++) {
                sb.append("let variable_").append(i).append(" = ").append(i).append(";\n");
                sb.append("if (variable_").append(i).append(" > 100) {\n");
                sb.append("    console.log(\"Mayor\", variable_").append(i).append(");\n");
                sb.append("} else {\n");
                sb.append("    print(variable_").append(i).append(" + 1);\n");
                sb.append("}\n");
            }

            long inicio = System.currentTimeMillis();
            CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(sb.toString());
            long duracion = System.currentTimeMillis() - inicio;

            assertTrue(res.esExitoso(), "El script extenso debe compilar sin errores");
            assertTrue(duracion < 3000, "El tiempo de análisis debe ser lineal y completarse rápidamente (< 3000 ms)");
            assertNotNull(res.astRaiz());
            NodoAST.Programa prog = (NodoAST.Programa) res.astRaiz();
            assertEquals(500, prog.sentencias().size(), "Deben analizarse exactamente 500 sentencias de nivel superior");
        }
    }

    // =========================================================================
    // FASE 2: BLINDAJE DEL ANALIZADOR LÉXICO
    // =========================================================================
    @Nested
    @DisplayName("Fase 2: Blindaje del Analizador Léxico")
    class Fase2BlindajeLexico {

        @Test
        @DisplayName("2.1. Prioridad entre palabras reservadas e identificadores")
        void test2_1_PrioridadPalabrasReservadasEIdentificadores() throws Exception {
            String codigo = """
                    let ifelse = 10;
                    let while_1 = 20;
                    if (ifelse == 10) { }
                    """;

            TablaSimbolos tabla = new TablaSimbolos();
            AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
            TokenStream stream = new TokenStream(lexer);
            List<Token> tokens = stream.getTodosLosTokens();

            // "let" e "if" deben ser palabras reservadas
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.LET && "let".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.IF && "if".equals(t.lexema())));

            // "ifelse" y "while_1" deben ser identificadores
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.ID && "ifelse".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.ID && "while_1".equals(t.lexema())));

            // Ambos identificadores deben estar en la tabla de símbolos
            assertTrue(tabla.existe("ifelse"));
            assertTrue(tabla.existe("while_1"));
            // Las palabras clave no deben estar en la tabla de símbolos como identificadores de usuario
            assertFalse(tabla.existe("let"));
            assertFalse(tabla.existe("if"));
        }

        @Test
        @DisplayName("2.2. Colisión de operadores de múltiples caracteres")
        void test2_2_ColisionOperadoresMultiplesCaracteres() throws Exception {
            String codigo = "a===b; a==b; a=b; a!==b; a!=b; !a; a<=b; a>=b; a++; a--;";
            TablaSimbolos tabla = new TablaSimbolos();
            AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
            TokenStream stream = new TokenStream(lexer);
            List<Token> tokens = stream.getTodosLosTokens();

            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.IGUAL_ESTRICTO && "===".equals(t.lexema())),
                    "=== debe ser un solo token de igualdad estricta");
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.IGUAL_IGUAL && "==".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.ASIGNACION && "=".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.DIFERENTE_ESTRICTO && "!==".equals(t.lexema())),
                    "!== debe ser un solo token de diferencia estricta");
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.DIFERENTE && "!=".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.NOT && "!".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.MENOR_IGUAL && "<=".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.MAYOR_IGUAL && ">=".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.INCREMENTO && "++".equals(t.lexema())));
            assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.DECREMENTO && "--".equals(t.lexema())));
        }

        @Test
        @DisplayName("2.3. Cadenas y comentarios mal cerrados")
        void test2_3_CadenasYComentariosMalCerrados() {
            // Comentario de bloque sin cerrar
            String codigoComentario = "/* comentario de bloque sin cerrar";
            CompiladorJS.ResultadoCompilacion resComentario = CompiladorJS.compilar(codigoComentario);
            assertTrue(resComentario.tieneErroresLexicos(), "Debe detectar el comentario sin cerrar como error");
            assertTrue(resComentario.erroresLexicos().stream()
                    .anyMatch(e -> e.descripcion().contains("Comentario de bloque sin cerrar")));

            // Cadena sin comilla de cierre
            String codigoCadena = "let texto = \"cadena sin cerrar;\nlet y = 10;";
            CompiladorJS.ResultadoCompilacion resCadena = CompiladorJS.compilar(codigoCadena);
            assertTrue(resCadena.tieneErroresLexicos() || resCadena.tieneErroresSintacticos(),
                    "Debe reportar anomalía ante comilla sin cerrar");
        }

        @Test
        @DisplayName("2.4. Números con formatos tramposos (múltiples puntos y sufijo de dígito)")
        void test2_4_NumerosConFormatosTramposos() {
            // Múltiples puntos: 12.34.56
            String codigoNumero = "let malNumero = 12.34.56;";
            CompiladorJS.ResultadoCompilacion resNumero = CompiladorJS.compilar(codigoNumero);
            // No debe aceptarlo como un solo flotante válido y debe dar error sintáctico o léxico
            assertTrue(resNumero.tieneErroresSintacticos() || resNumero.tieneErroresLexicos(),
                    "12.34.56 no debe ser aceptado como un único literal flotante válido");

            // Identificador que inicia con dígitos: 99variable
            String codigoId = "let 99variable = 5;";
            CompiladorJS.ResultadoCompilacion resId = CompiladorJS.compilar(codigoId);
            assertTrue(resId.tieneErroresLexicos(), "Debe capturar error léxico de identificador iniciado en dígito");
            assertTrue(resId.erroresLexicos().stream()
                    .anyMatch(e -> e.descripcion().contains("no puede iniciar con un dígito")));
        }

        @Test
        @DisplayName("2.5. Registro único en la Tabla de Símbolos con múltiples apariciones")
        void test2_5_RegistroEnTablaSimbolos() {
            String codigo = """
                    let contador = 0;
                    contador = contador + 1;
                    print(contador);
                    contador++;
                    """;
            CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(codigo);
            assertTrue(res.esExitoso());

            TablaSimbolos tabla = res.tablaSimbolos();
            assertTrue(tabla.existe("contador"));
            var entrada = tabla.buscar("contador");
            assertNotNull(entrada);
            assertEquals(1, entrada.primeraLinea(), "Primera aparición debe ser en línea 1");
            assertEquals(5, entrada.ocurrencias(), "Debe registrar exactamente 5 apariciones de 'contador'");
            assertEquals(1, tabla.cantidad(), "La tabla debe tener una única fila para 'contador'");
        }
    }

    // =========================================================================
    // FASE 3: BLINDAJE DEL ANALIZADOR SINTÁCTICO
    // =========================================================================
    @Nested
    @DisplayName("Fase 3: Blindaje del Analizador Sintáctico")
    class Fase3BlindajeSintactico {

        @Test
        @DisplayName("3.1. Equilibrio y anidamiento de paréntesis y llaves")
        void test3_1_EquilibrioYAnidamientoDelimitadores() {
            // Caso válido anidado profundo
            String valido = "let x = ((1 + 2) * (3 + (4 / 2)));";
            CompiladorJS.ResultadoCompilacion resValido = CompiladorJS.compilar(valido);
            assertTrue(resValido.esExitoso(), "Paréntesis anidados equilibrados deben ser válidos");

            // Inválido 1: paréntesis de cierre sobrante
            String invalido1 = "let x = (1 + 9) * 3);";
            CompiladorJS.ResultadoCompilacion resInvalido1 = CompiladorJS.compilar(invalido1);
            assertTrue(resInvalido1.tieneErroresSintacticos(), "Paréntesis de cierre sobrante debe dar error");

            // Inválido 2: paréntesis de apertura sin cerrar
            String invalido2 = "let x = ((1 + 2);";
            CompiladorJS.ResultadoCompilacion resInvalido2 = CompiladorJS.compilar(invalido2);
            assertTrue(resInvalido2.tieneErroresSintacticos(), "Paréntesis de apertura sin cerrar debe dar error");

            // Inválido 3: llave sin cerrar en bloque if
            String invalido3 = "if (a) { let x = 1;";
            CompiladorJS.ResultadoCompilacion resInvalido3 = CompiladorJS.compilar(invalido3);
            assertTrue(resInvalido3.tieneErroresSintacticos(), "Llave sin cerrar en bloque if debe dar error");
        }

        @Test
        @DisplayName("3.2. Secuencias de condicionales if y else (problema de emparejamiento)")
        void test3_2_SecuenciasCondicionalesIfElse() {
            // Válido: if anidado con else
            String valido = "if (a) { if (b) { x = 1; } else { x = 2; } }";
            CompiladorJS.ResultadoCompilacion resValido = CompiladorJS.compilar(valido);
            assertTrue(resValido.esExitoso(), "If-else anidado debe ser válido");

            // Inválido 1: else huérfano sin if previo
            String invalido1 = "else { x = 1; }";
            CompiladorJS.ResultadoCompilacion resInvalido1 = CompiladorJS.compilar(invalido1);
            assertTrue(resInvalido1.tieneErroresSintacticos(), "Else huérfano debe ser rechazado sintácticamente");

            // Inválido 2: dos cláusulas else consecutivas para un solo if
            String invalido2 = "if (a) { x = 1; } else { x = 2; } else { x = 3; }";
            CompiladorJS.ResultadoCompilacion resInvalido2 = CompiladorJS.compilar(invalido2);
            assertTrue(resInvalido2.tieneErroresSintacticos(), "Segundo else consecutivo debe ser rechazado");
        }

        @Test
        @DisplayName("3.3. No ambigüedad en precedencia y asociatividad de operadores")
        void test3_3_NoAmbiguedadPrecedenciaYAsociatividad() {
            // Precedencia: b + c * a -> raíz es suma (+), hijo derecho es multiplicación (*)
            String codigoPrecedencia = "a = b + c * a;";
            CompiladorJS.ResultadoCompilacion resPrec = CompiladorJS.compilar(codigoPrecedencia);
            assertTrue(resPrec.esExitoso());

            NodoAST.Programa progPrec = (NodoAST.Programa) resPrec.astRaiz();
            NodoAST.Asignacion asig = (NodoAST.Asignacion) progPrec.sentencias().get(0);
            assertInstanceOf(NodoAST.ExpresionBinaria.class, asig.valor());
            NodoAST.ExpresionBinaria suma = (NodoAST.ExpresionBinaria) asig.valor();
            assertEquals("+", suma.operador());
            assertInstanceOf(NodoAST.ExpresionBinaria.class, suma.derecha(),
                    "La multiplicación debe estar más profunda en el árbol (hijo derecho de la suma)");
            assertEquals("*", ((NodoAST.ExpresionBinaria) suma.derecha()).operador());

            // Asociatividad por la izquierda: 10 - 5 - 2 -> raíz es '-', hijo izquierdo es (10 - 5)
            String codigoAsoc = "x = 10 - 5 - 2;";
            CompiladorJS.ResultadoCompilacion resAsoc = CompiladorJS.compilar(codigoAsoc);
            assertTrue(resAsoc.esExitoso());

            NodoAST.Programa progAsoc = (NodoAST.Programa) resAsoc.astRaiz();
            NodoAST.Asignacion asigResta = (NodoAST.Asignacion) progAsoc.sentencias().get(0);
            assertInstanceOf(NodoAST.ExpresionBinaria.class, asigResta.valor());
            NodoAST.ExpresionBinaria restaPrincipal = (NodoAST.ExpresionBinaria) asigResta.valor();
            assertEquals("-", restaPrincipal.operador());
            assertInstanceOf(NodoAST.ExpresionBinaria.class, restaPrincipal.izquierda(),
                    "La asociatividad por la izquierda agrupa (10 - 5) a la izquierda de la segunda resta");
            assertEquals("-", ((NodoAST.ExpresionBinaria) restaPrincipal.izquierda()).operador());
            assertInstanceOf(NodoAST.Literal.class, restaPrincipal.derecha());
            assertEquals(2L, ((NodoAST.Literal) restaPrincipal.derecha()).valor());
        }
    }

    // =========================================================================
    // FASE 4: JERARQUÍA Y ORDEN DE REPORTE DE ERRORES
    // =========================================================================
    @Nested
    @DisplayName("Fase 4: Jerarquía y Orden de Errores")
    class Fase4JerarquiaErrores {

        @Test
        @DisplayName("4.1. Prioridad de detección y errores combinados (Léxico -> Sintáctico)")
        void test4_1_PrioridadDeteccionYErroresCombinados() {
            // Oración con símbolo léxico inválido '#' y error estructural
            // (Nota técnica JS: '$' y '_' son legales en identificadores de JavaScript; símbolos como '#', '@' están fuera del alfabeto)
            String codigo1 = "1 % # 9 ) = ;";
            CompiladorJS.ResultadoCompilacion res1 = CompiladorJS.compilar(codigo1);
            assertTrue(res1.tieneErroresLexicos(), "Debe detectar el error léxico '#'");
            assertTrue(res1.tieneErroresSintacticos(), "Debe detectar el error sintáctico posterior");

            // Oración con '@' en declaración y paréntesis desbalanceado
            String codigo2 = "let @var = (1 + 9) * 3);";
            CompiladorJS.ResultadoCompilacion res2 = CompiladorJS.compilar(codigo2);
            assertTrue(res2.tieneErroresLexicos(), "Debe detectar el símbolo inválido '@'");
            assertTrue(res2.tieneErroresSintacticos(), "Debe detectar el paréntesis desbalanceado");
            assertNotNull(res2.tokens(), "La lista de tokens debe haberse generado sin excepciones fatales");
        }

        @Test
        @DisplayName("4.2. Restricciones semánticas aceptadas por el analizador sintáctico")
        void test4_2_RestriccionesSemanticasAceptadasPorSintactico() {
            // 4 / 0 es una restricción semántica (división entre cero), la sintaxis es totalmente válida
            String codigoDivCero = "let x = 4 / 0;";
            CompiladorJS.ResultadoCompilacion resDiv = CompiladorJS.compilar(codigoDivCero);
            assertTrue(resDiv.esExitoso(), "Sintácticamente '4 / 0' es una expresión de división válida");

            // Usar una variable antes de ser declarada es válido sintácticamente en la gramática
            String codigoUsoAntes = """
                    print(total);
                    let total = 50;
                    """;
            CompiladorJS.ResultadoCompilacion resUso = CompiladorJS.compilar(codigoUsoAntes);
            assertTrue(resUso.esExitoso(), "Sintácticamente ambas instrucciones son válidas");
        }
    }

    // =========================================================================
    // FASE 5: PRUEBAS DE INTERFAZ Y ESTADO
    // =========================================================================
    @Nested
    @DisplayName("Fase 5: Pruebas de Interfaz y Estado")
    class Fase5InterfazYEstado {

        @Test
        @DisplayName("5.1. Limpieza de estado entre ejecuciones sucesivas (sin duplicaciones)")
        void test5_1_LimpiezaDeEstadoEntreEjecuciones() {
            String codigo = """
                    let x = 10;
                    print(x);
                    """;

            // Ejecutar 5 veces seguidas simulando presionar 5 veces el botón "Analizar"
            CompiladorJS.ResultadoCompilacion primera = CompiladorJS.compilar(codigo);
            int tokensEsperados = primera.tokens().size();
            int simbolosEsperados = primera.tablaSimbolos().cantidad();

            for (int i = 0; i < 4; i++) {
                CompiladorJS.ResultadoCompilacion corrida = CompiladorJS.compilar(codigo);
                assertEquals(tokensEsperados, corrida.tokens().size(),
                        "La cantidad de tokens no debe incrementarse entre ejecuciones");
                assertEquals(simbolosEsperados, corrida.tablaSimbolos().cantidad(),
                        "La tabla de símbolos debe limpiarse y no duplicar filas");
                assertEquals(0, corrida.totalErrores());
            }
        }

        @Test
        @DisplayName("5.2. Recuperación tras error (código con error seguido de código limpio)")
        void test5_2_RecuperacionTrasError() {
            // Corrida 1: con errores graves
            String codigoConError = "let = ; $$$";
            CompiladorJS.ResultadoCompilacion resError = CompiladorJS.compilar(codigoConError);
            assertFalse(resError.esExitoso());
            assertTrue(resError.totalErrores() > 0);

            // Corrida 2: código completamente limpio
            String codigoLimpio = """
                    let nombre = "Compiladores";
                    print(nombre);
                    """;
            CompiladorJS.ResultadoCompilacion resLimpio = CompiladorJS.compilar(codigoLimpio);
            assertTrue(resLimpio.esExitoso(), "Tras corregir el código, el compilador debe quedar 100% limpio");
            assertEquals(0, resLimpio.totalErrores(), "El panel de errores debe quedar limpio sin residuos");
        }

        @Test
        @DisplayName("5.3. Rutas relativas y recursos independientes del classpath")
        void test5_3_RutasRelativasYClasspath() {
            // Validar que los recursos se cargan dinámicamente vía ClassLoader
            assertNotNull(getClass().getResourceAsStream("/pruebas/algoritmo_bajo.js"),
                    "algoritmo_bajo.js debe ser accesible de forma relativa vía classpath");
            assertNotNull(getClass().getResourceAsStream("/pruebas/algoritmo_medio.js"),
                    "algoritmo_medio.js debe ser accesible de forma relativa vía classpath");
        }

        @Test
        @DisplayName("5.4. Sincronización exacta de Líneas y Columnas (base 1)")
        void test5_4_SincronizacionLineasYColumnas() {
            // Generar un error en la línea 2, columna 5
            String codigo = "\n    @";
            CompiladorJS.ResultadoCompilacion res = CompiladorJS.compilar(codigo);
            assertTrue(res.tieneErroresLexicos());
            ErrorLexico error = res.erroresLexicos().get(0);
            assertEquals(2, error.linea(), "La línea debe ser exactamente 2 (base 1)");
            assertEquals(5, error.columna(), "La columna debe ser exactamente 5 (base 1)");
        }
    }
}
