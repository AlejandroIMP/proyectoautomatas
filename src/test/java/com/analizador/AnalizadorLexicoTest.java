package com.analizador;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import com.analizador.sintactico.TokenStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AnalizadorLexicoTest {

    private String leerArchivoPrueba(String nombreArchivo) throws Exception {
        InputStream is = getClass().getResourceAsStream("/pruebas/" + nombreArchivo);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream("pruebas/" + nombreArchivo);
        }
        assertNotNull(is, "El archivo de prueba no se encontro en el classpath: " + nombreArchivo);

        try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[2048];
            int n;
            while ((n = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, n);
            }
            return sb.toString();
        }
    }

    @Test
    @DisplayName("Nivel 1 (Facil): Analisis de variables, constantes y operadores basicos")
    void testAlgoritmoBajo() throws Exception {
        String codigo = leerArchivoPrueba("algoritmo_bajo.js");
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        List<Token> tokens = stream.getTodosLosTokens();
        assertFalse(tokens.isEmpty(), "La lista de tokens no debe estar vacia");
        assertEquals(TokenType.EOF, tokens.getLast().tipo(), "El ultimo token debe ser EOF");

        // Sin errores lexicos
        assertTrue(lexer.getErrores().isEmpty(), "No deben existir errores lexicos en algoritmo_bajo.js");

        // Verificacion de la tabla de simbolos
        var simbolos = tabla.obtenerTodos();
        assertFalse(simbolos.isEmpty(), "La tabla de simbolos debe contener variables registradas");
        assertTrue(simbolos.stream().anyMatch(s -> s.nombre().equals("PI")), "Debe existir el simbolo 'PI'");
        assertTrue(simbolos.stream().anyMatch(s -> s.nombre().equals("radio")), "Debe existir el simbolo 'radio'");
        assertTrue(simbolos.stream().anyMatch(s -> s.nombre().equals("celsius")), "Debe existir el simbolo 'celsius'");
    }

    @Test
    @DisplayName("Nivel 2 (Medio): Estructuras de control, bucles, arreglos y operadores relacionales")
    void testAlgoritmoMedio() throws Exception {
        String codigo = leerArchivoPrueba("algoritmo_medio.js");
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        List<Token> tokens = stream.getTodosLosTokens();
        assertTrue(lexer.getErrores().isEmpty(), "algoritmo_medio.js debe analizarse sin errores lexicos");

        // Debe reconocer palabras clave de JavaScript
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.FUNCTION), "Debe contener token FUNCTION");
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.WHILE), "Debe contener token WHILE");
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.FOR), "Debe contener token FOR");
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.IF), "Debe contener token IF");
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.ELSE), "Debe contener token ELSE");
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.RETURN), "Debe contener token RETURN");

        // Operadores de comparacion e incremento
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.MAYOR_IGUAL), "Debe contener token >=");
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.INCREMENTO), "Debe contener token ++");
    }

    @Test
    @DisplayName("Nivel 3 (Dificil): Notacion cientifica, identificadores con $ y _, escapes y comparaciones estrictas")
    void testAlgoritmoAlto() throws Exception {
        String codigo = leerArchivoPrueba("algoritmo_alto.js");
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        List<Token> tokens = stream.getTodosLosTokens();
        assertTrue(lexer.getErrores().isEmpty(), "algoritmo_alto.js debe analizarse sin errores");

        // Notacion cientifica
        Token tokenCientifico = tokens.stream()
                .filter(t -> t.tipo() == TokenType.LIT_FLOTANTE && "1.5e-4".equals(t.lexema()))
                .findFirst()
                .orElse(null);
        assertNotNull(tokenCientifico, "Debe reconocer '1.5e-4' como flotante");
        assertEquals(0.00015, (Double) tokenCientifico.valorLiteral(), 1e-9);

        // Identificadores especiales de JS
        var simbolos = tabla.obtenerTodos();
        assertTrue(simbolos.stream().anyMatch(s -> s.nombre().equals("$nodos_lista")), "Debe registrar '$nodos_lista'");
        assertTrue(simbolos.stream().anyMatch(s -> s.nombre().equals("_objetivo")), "Debe registrar '_objetivo'");

        // Operadores de igualdad estricta
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.IGUAL_ESTRICTO), "Debe contener '==='");
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.DIFERENTE_ESTRICTO), "Debe contener '!=='");

        // Cadena con comillas escapadas
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.LIT_CADENA && t.lexema().contains("\\\"")),
                "Debe reconocer cadenas con escapes de comillas");
    }

    @Test
    @DisplayName("Nivel 4 (Muy Complejo): Dijkstra, matrices, comentarios multilineas y estres lexico")
    void testAlgoritmoMuyComplejo() throws Exception {
        String codigo = leerArchivoPrueba("algoritmo_muy_complejo.js");
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        List<Token> tokens = stream.getTodosLosTokens();
        assertTrue(lexer.getErrores().isEmpty(), "algoritmo_muy_complejo.js debe analizarse con cero errores");
        assertTrue(tokens.size() > 150, "Debe procesar un volumen alto de tokens");

        // Identificadores y palabras reservadas
        var simbolos = tabla.obtenerTodos();
        assertTrue(simbolos.stream().anyMatch(s -> s.nombre().equals("dijkstra")));
        assertTrue(simbolos.stream().anyMatch(s -> s.nombre().equals("multiplicarMatrices")));

        // Validar que los comentarios multilineas extensos fueron ignorados sin alterar lineas y columnas
        Token primerToken = tokens.getFirst();
        assertTrue(primerToken.linea() > 1, "Las coordenadas de linea deben considerar los comentarios iniciales");
    }

    @Test
    @DisplayName("Nivel 5 (Recuperacion de Errores): Intercepcion de simbolos no validos sin interrumpir analisis")
    void testRecuperacionErroresLexicos() throws Exception {
        String codigo = leerArchivoPrueba("algoritmo_errores.js");
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);

        List<Token> tokens = stream.getTodosLosTokens();
        List<ErrorLexico> errores = lexer.getErrores();

        // Debe registrar los simbolos ilegales
        assertFalse(errores.isEmpty(), "Deben capturarse errores lexicos intencionales");

        // Verificar captura de simbolos invalidos especificos
        assertTrue(errores.stream().anyMatch(e -> e.lexemaInvalido().equals("@")), "Debe capturar '@'");
        assertTrue(errores.stream().anyMatch(e -> e.lexemaInvalido().equals("#")), "Debe capturar '#'");
        assertTrue(errores.stream().anyMatch(e -> e.lexemaInvalido().equals("~")), "Debe capturar '~'");
        assertTrue(errores.stream().anyMatch(e -> e.lexemaInvalido().equals("^")), "Debe capturar '^'");
        assertTrue(errores.stream().anyMatch(e -> e.lexemaInvalido().equals("?")), "Debe capturar '?'");

        // Verificar que los tokens validos posteriores siguen siendo reconocidos
        assertTrue(tokens.stream().anyMatch(t -> "totalFinal".equals(t.lexema())),
                "El analizador debe recuperarse y procesar 'totalFinal'");
        assertTrue(tokens.stream().anyMatch(t -> "estadoExitoso".equals(t.lexema())),
                "El analizador debe recuperarse y procesar 'estadoExitoso'");
        assertEquals(TokenType.EOF, tokens.getLast().tipo());
    }

    @Test
    @DisplayName("Puente Sintactico: TokenStream lookahead y validacion match")
    void testTokenStreamBridge() throws Exception {
        String input = "let x = 42;";
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(input), new TablaSimbolos());
        TokenStream stream = new TokenStream(lexer);

        // Peek no debe consumir
        assertEquals(TokenType.LET, stream.peek().tipo());
        assertEquals(TokenType.LET, stream.peek().tipo());

        // Next consume y avanza
        Token t1 = stream.next();
        assertEquals(TokenType.LET, t1.tipo());
        assertEquals(TokenType.ID, stream.peek().tipo());

        // Match exitoso
        Token t2 = stream.match(TokenType.ID);
        assertEquals("x", t2.lexema());

        Token t3 = stream.match(TokenType.ASIGNACION);
        assertEquals("=", t3.lexema());

        // Match con discrepancia debe lanzar excepcion controlada
        assertThrows(IllegalStateException.class, () -> stream.match(TokenType.LLAVE_IZQ),
                "Un tipo inesperado en match() debe lanzar excepcion sintactica");
    }

    @Test
    @DisplayName("Template Literals: Reconocimiento de plantillas de texto multilineas y simples")
    void testTemplateLiterals() throws Exception {
        String codigo = """
                const saludo = `Hola ${usuario}`;
                const multilinea = `Linea 1
                Linea 2
                Linea 3`;
                """;
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);
        List<Token> tokens = stream.getTodosLosTokens();

        assertTrue(lexer.getErrores().isEmpty(), "No deben existir errores lexicos en template strings");

        List<Token> templates = tokens.stream()
                .filter(t -> t.tipo() == TokenType.LIT_TEMPLATE)
                .toList();
        assertEquals(2, templates.size(), "Deben reconocerse 2 template literals");
        assertEquals("Hola ${usuario}", templates.get(0).valorLiteral());
        assertTrue(templates.get(1).valorLiteral().toString().contains("Linea 2"));
    }

    @Test
    @DisplayName("Tabla de Simbolos: Verificacion de metodos de consulta y busqueda")
    void testTablaSimbolosConsultas() throws Exception {
        TablaSimbolos tabla = new TablaSimbolos();
        assertTrue(tabla.estaVacia());
        assertEquals(0, tabla.cantidad());

        tabla.registrarIdentificador("contador", 5, 10);
        tabla.registrarIdentificador("contador", 8, 12);
        tabla.registrarIdentificador("limite", 6, 1);

        assertFalse(tabla.estaVacia());
        assertEquals(2, tabla.cantidad());
        assertTrue(tabla.existe("contador"));
        assertTrue(tabla.existe("limite"));
        assertFalse(tabla.existe("noExiste"));

        var entradaContador = tabla.buscar("contador");
        assertNotNull(entradaContador);
        assertEquals(5, entradaContador.primeraLinea());
        assertEquals(10, entradaContador.primeraColumna());
        assertEquals(2, entradaContador.ocurrencias());

        assertNull(tabla.buscar("noExiste"));
        assertTrue(tabla.buscarOpcional("limite").isPresent());
        assertTrue(tabla.buscarOpcional("noExiste").isEmpty());

        assertEquals(2, tabla.obtenerMapa().size());
        assertDoesNotThrow(tabla::mostrar);
    }

    @Test
    @DisplayName("Identificador invalido que inicia con digito y numeros con exponente")
    void testIdentificadorInvalidoIniciaConDigito() throws Exception {
        String codigo = "let x = 1maria;\nlet y = 1e5;\nlet z = maria1;";
        TablaSimbolos tabla = new TablaSimbolos();
        AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(codigo), tabla);
        TokenStream stream = new TokenStream(lexer);
        List<Token> tokens = stream.getTodosLosTokens();

        List<ErrorLexico> errores = lexer.getErrores();
        assertEquals(1, errores.size(), "Debe existir exactamente 1 error lexico");
        assertEquals("1maria", errores.get(0).lexemaInvalido());
        assertEquals("Identificador inválido: no puede iniciar con un dígito", errores.get(0).descripcion());

        // Verificar que 1e5 se reconocio como flotante y maria1 como identificador
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.LIT_FLOTANTE && "1e5".equals(t.lexema())));
        assertTrue(tokens.stream().anyMatch(t -> t.tipo() == TokenType.ID && "maria1".equals(t.lexema())));
    }
}
