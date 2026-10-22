package com.analizador;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.sintactico.AnalizadorSintacticoJS;
import com.analizador.sintactico.NodoAST;
import java.io.StringReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AnalizadorSintacticoTest {

    private AnalizadorSintacticoJS parsear(String codigo) throws Exception {
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), new TablaSimbolos());
        AnalizadorSintacticoJS parser = new AnalizadorSintacticoJS(lexer);
        parser.parse();
        return parser;
    }

    @Test
    @DisplayName("Helen - Módulo Variables: let, const, var con y sin inicialización")
    void testDeclaracionVariables() throws Exception {
        String codigo = """
                let x = 10;
                const nombre = "Compiladores";
                var pi = 3.1416;
                let sinValor;
                var bandera = true;
                x = x + 1;
                """;
        AnalizadorSintacticoJS parser = parsear(codigo);
        assertTrue(parser.getErroresSintacticos().isEmpty(), "No deben existir errores sintácticos");

        NodoAST ast = parser.getAstRaiz();
        assertNotNull(ast);
        assertTrue(ast instanceof NodoAST.Programa);
        NodoAST.Programa prog = (NodoAST.Programa) ast;
        assertEquals(6, prog.sentencias().size());

        // Verificar primera sentencia (let x = 10)
        assertTrue(prog.sentencias().get(0) instanceof NodoAST.DeclaracionVariable);
        NodoAST.DeclaracionVariable declLet = (NodoAST.DeclaracionVariable) prog.sentencias().get(0);
        assertEquals("let", declLet.tipoDeclaracion());
        assertEquals("x", declLet.identificador());
        assertNotNull(declLet.valorInicial());

        // Verificar segunda sentencia (const nombre = "Compiladores")
        NodoAST.DeclaracionVariable declConst = (NodoAST.DeclaracionVariable) prog.sentencias().get(1);
        assertEquals("const", declConst.tipoDeclaracion());
        assertEquals("nombre", declConst.identificador());

        // Verificar asignación posterior (x = x + 1)
        assertTrue(prog.sentencias().get(5) instanceof NodoAST.Asignacion);
        NodoAST.Asignacion asig = (NodoAST.Asignacion) prog.sentencias().get(5);
        assertEquals("x", asig.identificador());
        assertTrue(asig.valor() instanceof NodoAST.ExpresionBinaria);
    }

    
@Test
@DisplayName("Helen - const sin inicialización debe generar error")
void testConstSinInicializacion() throws Exception {
    String codigo = "const PI;";

    AnalizadorSintacticoJS parser = parsear(codigo);

    assertFalse(
        parser.getErroresSintacticos().isEmpty(),
        "Debe detectar const sin valor inicial"
    );
}

@Test
@DisplayName("Helen - declaración mal terminada debe generar error")
void testDeclaracionMalTerminada() throws Exception {
    String codigo = "let edad = 20";

    AnalizadorSintacticoJS parser = parsear(codigo);

    assertFalse(
        parser.getErroresSintacticos().isEmpty(),
        "Debe detectar la falta del punto y coma"
    );
}

    @Test
    @DisplayName("Pablo - Módulo Control de Flujo: IF simple, IF-ELSE y condiciones lógicas")
    void testInstruccionIf() throws Exception {
        String codigo = """
                if (edad >= 18) {
                    let esAdulto = true;
                }

                if (saldo > 0 && !bloqueado) {
                    let estado = "permitido";
                } else {
                    let estado = "denegado";
                }
                """;
        AnalizadorSintacticoJS parser = parsear(codigo);
        assertTrue(parser.getErroresSintacticos().isEmpty(), "No deben existir errores sintácticos en IF");

        NodoAST ast = parser.getAstRaiz();
        assertNotNull(ast);
        NodoAST.Programa prog = (NodoAST.Programa) ast;
        assertEquals(2, prog.sentencias().size());

        // Primer if sin else
        assertTrue(prog.sentencias().get(0) instanceof NodoAST.SentenciaIf);
        NodoAST.SentenciaIf if1 = (NodoAST.SentenciaIf) prog.sentencias().get(0);
        assertNotNull(if1.condicion());
        assertNotNull(if1.bloqueThen());
        assertNull(if1.bloqueElse());

        // Segundo if con else
        assertTrue(prog.sentencias().get(1) instanceof NodoAST.SentenciaIf);
        NodoAST.SentenciaIf if2 = (NodoAST.SentenciaIf) prog.sentencias().get(1);
        assertNotNull(if2.condicion());
        assertNotNull(if2.bloqueThen());
        assertNotNull(if2.bloqueElse());
    }

    @Test
    @DisplayName("Mario - Módulo Salida: console.log(...) y print(...) con múltiples expresiones")
    void testInstruccionImprimir() throws Exception {
        String codigo = """
                console.log("El resultado es:", total);
                print(42);
                console.log();
                print();
                """;
        AnalizadorSintacticoJS parser = parsear(codigo);
        assertTrue(parser.getErroresSintacticos().isEmpty(), "No deben existir errores en console.log / print");

        NodoAST ast = parser.getAstRaiz();
        assertNotNull(ast);
        NodoAST.Programa prog = (NodoAST.Programa) ast;
        assertEquals(4, prog.sentencias().size());

        NodoAST.SentenciaImprimir p1 = (NodoAST.SentenciaImprimir) prog.sentencias().get(0);
        assertEquals("console.log", p1.funcion());
        assertEquals(2, p1.argumentos().size());

        NodoAST.SentenciaImprimir p2 = (NodoAST.SentenciaImprimir) prog.sentencias().get(1);
        assertEquals("print", p2.funcion());
        assertEquals(1, p2.argumentos().size());

        NodoAST.SentenciaImprimir p3 = (NodoAST.SentenciaImprimir) prog.sentencias().get(2);
        assertEquals("console.log", p3.funcion());
        assertTrue(p3.argumentos().isEmpty());
    }

    @Test
    @DisplayName("Mario - Módulo Expresiones: precedencia, operadores unarios y literales")
    void testExpresionesEnImpresion() throws Exception {
        String codigo = """
                console.log("resultado", 1 + 2 * 3, -(4 - 1), !false, ++contador, contador--);
                print(`listo`, null, true, 2.5);
                """;
        AnalizadorSintacticoJS parser = parsear(codigo);
        assertTrue(parser.getErroresSintacticos().isEmpty(),
                "Las expresiones y listas de argumentos deben analizarse sin errores");

        NodoAST.Programa programa = (NodoAST.Programa) parser.getAstRaiz();
        assertEquals(2, programa.sentencias().size());

        NodoAST.SentenciaImprimir consoleLog =
                (NodoAST.SentenciaImprimir) programa.sentencias().get(0);
        assertEquals(6, consoleLog.argumentos().size());

        NodoAST.ExpresionBinaria suma =
                (NodoAST.ExpresionBinaria) consoleLog.argumentos().get(1);
        assertEquals("+", suma.operador());
        assertInstanceOf(NodoAST.ExpresionBinaria.class, suma.derecha());
        assertEquals("*", ((NodoAST.ExpresionBinaria) suma.derecha()).operador());

        assertEquals("-", ((NodoAST.ExpresionUnaria) consoleLog.argumentos().get(2)).operador());
        assertEquals("!", ((NodoAST.ExpresionUnaria) consoleLog.argumentos().get(3)).operador());
        assertEquals("++ (prefijo)",
                ((NodoAST.ExpresionUnaria) consoleLog.argumentos().get(4)).operador());
        assertEquals("-- (postfijo)",
                ((NodoAST.ExpresionUnaria) consoleLog.argumentos().get(5)).operador());

        NodoAST.SentenciaImprimir print =
                (NodoAST.SentenciaImprimir) programa.sentencias().get(1);
        assertEquals(4, print.argumentos().size());
        assertEquals("Plantilla", ((NodoAST.Literal) print.argumentos().get(0)).tipo());
        assertEquals("Null", ((NodoAST.Literal) print.argumentos().get(1)).tipo());
        assertEquals(Boolean.TRUE, ((NodoAST.Literal) print.argumentos().get(2)).valor());
        assertEquals("Flotante", ((NodoAST.Literal) print.argumentos().get(3)).tipo());
    }

    @Test
    @DisplayName("Mario - print sin paréntesis debe generar error")
    void testPrintSinParentesis() throws Exception {
        String codigo = "print \"hola\";";
        AnalizadorSintacticoJS parser = parsear(codigo);
        assertFalse(parser.getErroresSintacticos().isEmpty(),
                "Debe detectar print sin paréntesis como error");
    }

    @Test
    @DisplayName("Mario - console.log sin cierre de paréntesis debe generar error")
    void testConsoleLogSinCierre() throws Exception {
        String codigo = "console.log(\"incompleto\";";
        AnalizadorSintacticoJS parser = parsear(codigo);
        assertFalse(parser.getErroresSintacticos().isEmpty(),
                "Debe detectar console.log sin cerrar paréntesis");
    }

    @Test
    @DisplayName("Alejandro - Integración Completa: Programa que combina variables, if, impresión y árbol visual")
    void testProgramaCompletoConArbol() throws Exception {
        String codigo = """
                let a = 15;
                let b = 25;
                let suma = a + b;

                if (suma > 30) {
                    console.log("La suma es mayor a 30:", suma);
                } else {
                    print("La suma es menor o igual a 30");
                }
                """;
        AnalizadorSintacticoJS parser = parsear(codigo);
        assertTrue(parser.getErroresSintacticos().isEmpty());

        NodoAST ast = parser.getAstRaiz();
        assertNotNull(ast);

        // Generar nodo para el árbol de la interfaz gráfica
        var treeNode = ast.toTreeNode();
        assertNotNull(treeNode);
        assertTrue(treeNode.getChildCount() >= 4, "El árbol visual debe reflejar los nodos del programa");
    }

    @Test
    @DisplayName("Manejo y Registro de Errores Sintácticos")
    void testCapturaErroresSintacticos() throws Exception {
        // Falta expresión o token inválido en la asignación
        String codigoConError = """
                let = 123;
                let correcto = 50;
                """;
        AnalizadorSintacticoJS parser = parsear(codigoConError);
        assertFalse(parser.getErroresSintacticos().isEmpty(), "Debe capturar el error sintáctico");
        assertEquals(1, parser.getErroresSintacticos().size());
        assertTrue(parser.getErroresSintacticos().get(0).descripcion().contains("Error sintáctico"));
    }
}
