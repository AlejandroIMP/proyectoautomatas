1. Análisis y Mejora del Plan ActualEl plan original contemplaba la programación manual de los autómatas y del lector de caracteres desde cero. Al incorporar JFlex, Java 21 y Eclipse IDE, el proyecto evoluciona de la siguiente manera:Sustitución de AFD manuales por Generación Automática (JFlex): En lugar de codificar manualmente las transiciones de estados en Java, se empleará JFlex (la adaptación para Java de herramientas clásicas como Lex/Flex). Un generador de analizadores léxicos toma descripciones de alto nivel basadas en expresiones regulares y construye automáticamente el Autómata Finito Determinista (AFD) que reconoce las unidades lógicas en tiempo lineal. Esto reduce un trabajo pesado de depuración a la simple edición de expresiones regulares.   Aprovechamiento de Java 21 (LTS):Records (record): Uso de clases inmutables de una sola línea para modelar Token, ErrorLexico y entradas de la TablaSimbolos, eliminando código repetitivo (boilerplate).Interfaces Selladas (sealed interface) y Pattern Matching: Preparación de la jerarquía de nodos del futuro Árbol de Análisis Sintáctico y evaluación limpia mediante switch modernos.Text Blocks ("""): Inclusión de plantillas de algoritmos JavaScript multilínea directamente en las pruebas unitarias.Automatización en Eclipse IDE: Uso de un proyecto Maven dentro de Eclipse con el plugin jflex-maven-plugin, logrando que al guardar el archivo .flex, Eclipse genere y compile automáticamente la clase Lexer.java sin usar la consola manualmente.Alineación con la Teoría de Compiladores: Separación estricta de fases: el nivel léxico operará mediante gramáticas regulares (Tipo 3 de la Jerarquía de Chomsky) reconocidas por el AFD de JFlex, dejando listo el contrato para que el nivel sintáctico opere mediante una Gramática Independiente del Contexto (Tipo 2) capaz de validar estructuras anidadas y recursivas (como paréntesis equilibrados o bloques if-else) que un autómata finito no puede resolver por su memoria limitada.   2. Parte 1: Configuración del Entorno en Eclipse IDE (Java 21 + JFlex)2.1. Estructura de Paquetes del ProyectoCrear en Eclipse un nuevo Maven Project (File > New > Maven Project, seleccionando Create a simple project) con la siguiente estructura de directorios:PlaintextAnalizadorJS/
   ├── pom.xml
   ├── src/
   │   ├── main/
   │   │   ├── jflex/
   │   │   │   └── com/analizador/lexico/Lexer.flex      <-- Especificación JFlex
   │   │   ├── java/
   │   │   │   └── com/analizador/
   │   │   │       ├── MainApp.java                      <-- Punto de entrada
   │   │   │       ├── modelo/
   │   │   │       │   ├── TokenType.java                <-- Clasificación de tokens
   │   │   │       │   ├── Token.java                    <-- Record (Java 21)
   │   │   │       │   ├── ErrorLexico.java              <-- Record (Java 21)
   │   │   │       │   └── TablaSimbolos.java            <-- Gestor de identificadores
   │   │   │       ├── sintactico/
   │   │   │       │   ├── TokenStream.java              <-- Puente para futuro Parser
   │   │   │       │   └── NodoAST.java                  <-- Sealed Interface (Java 21)
   │   │   │       └── gui/
   │   │   │           └── VentanaPrincipal.java         <-- Interfaz Java Swing
   │   │   └── resources/
   │   │       └── pruebas/
   │   │           ├── algoritmo_bajo.js
   │   │           ├── algoritmo_medio.js
   │   │           └── algoritmo_alto.js
   2.2. Configuración del pom.xml para EclipseReemplazar el contenido del archivo pom.xml por la siguiente configuración. Al hacer clic derecho sobre el proyecto en Eclipse y seleccionar Maven > Update Project (Alt + F5), Eclipse descargará JFlex y compilará automáticamente el archivo .flex hacia target/generated-sources/jflex:XML<project xmlns="http://maven.apache.org/POM/4.0.0"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
   <modelVersion>4.0.0</modelVersion>
   <groupId>com.analizador</groupId>
   <artifactId>analizador-js</artifactId>
   <version>1.0.0</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <maven.compiler.release>21</maven.compiler.release>
    </properties>

    <build>
        <plugins>
            <!-- Plugin que compila automáticamente archivos .flex a código Java -->
            <plugin>
                <groupId>de.jflex</groupId>
                <artifactId>jflex-maven-plugin</artifactId>
                <version>1.9.1</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
3. Parte 2: Modelo de Datos Léxico y Tabla de SímbolosEn la definición conceptual de un lenguaje, el Token es el componente mínimo que representa una clasificación léxica, el Lexema es el valor particular de cada token, y el Patrón especifica la regla de asociación.   3.1. Enumeración TokenType.javaDefine las constantes simbólicas que el analizador léxico devolverá al futuro analizador sintáctico:   Javapackage com.analizador.modelo;

public enum TokenType {
// Palabras reservadas de JavaScript
LET, CONST, VAR, FUNCTION, IF, ELSE, WHILE, FOR, RETURN, TRUE, FALSE, NULL,

    // Identificadores y Literales
    ID, LIT_ENTERO, LIT_FLOTANTE, LIT_CADENA,
    
    // Operadores Aritméticos y de Asignación
    MAS, MENOS, MULT, DIV, MOD, ASIGNACION, INCREMENTO, DECREMENTO,
    
    // Operadores Relacionales y Lógicos
    IGUAL_IGUAL, IGUAL_ESTRICTO, DIFERENTE, DIFERENTE_ESTRICTO,
    MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL, AND, OR, NOT,
    
    // Delimitadores y Signos de Puntuación
    PAREN_IZQ, PAREN_DER, LLAVE_IZQ, LLAVE_DER, CORCHETE_IZQ, CORCHETE_DER,
    PUNTO_COMA, COMA, DOS_PUNTOS, PUNTO,
    
    // Control de Flujo del Compilador
    EOF
}
3.2. Records Token.java y ErrorLexico.java (Java 21)Uso de record para almacenar las propiedades inmutables y las coordenadas del texto fuente:Javapackage com.analizador.modelo;

public record Token(
TokenType tipo,
String lexema,
int linea,
int columna,
Object valorLiteral
) {
// Constructor compacto para tokens sin valor literal convertido
public Token(TokenType tipo, String lexema, int linea, int columna) {
this(tipo, lexema, linea, columna, null);
}
}
Javapackage com.analizador.modelo;

public record ErrorLexico(
String lexemaInvalido,
int linea,
int columna,
String descripcion
) {}
3.3. Clase TablaSimbolos.javaCuando el analizador léxico reconoce un identificador, lo introduce en la tabla de símbolos si todavía no figura en ella, permitiendo que el compilador conserve sus anotaciones para las fases sintáctica y semántica:   Javapackage com.analizador.modelo;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class TablaSimbolos {

    public record EntradaSimbolo(String nombre, String categoria, int primeraLinea, int primeraColumna, int ocurrencias) {}

    private final Map<String, EntradaSimbolo> tabla = new LinkedHashMap<>();

    public void registrarIdentificador(String lexema, int linea, int columna) {
        tabla.compute(lexema, (k, v) -> (v == null)
            ? new EntradaSimbolo(lexema, "IDENTIFICADOR", linea, columna, 1)
            : new EntradaSimbolo(lexema, v.categoria(), v.primeraLinea(), v.primeraColumna(), v.ocurrencias() + 1)
        );
    }

    public Collection<EntradaSimbolo> obtenerTodos() {
        return tabla.values();
    }

    public void limpiar() {
        tabla.clear();
    }
}
4. Parte 3: Especificación del Motor Léxico en JFlex (Lexer.flex)El archivo JFlex consta de tres secciones separadas por %%. Para su construcción se aplican tres principios fundamentales de la teoría de autómatas y compiladores:Prioridad de Palabras Reservadas: Dado que una palabra clave como else también coincide con el patrón de un identificador, JFlex resuelve la ambigüedad dando prioridad a la primera expresión regular mencionada. Por lo tanto, todas las palabras reservadas deben declararse antes de la regla de identificadores.   Jerarquía de Expresiones Regulares: Definición de macros utilizando clases de caracteres [], cerradura de Kleene *, cerradura positiva +, opción ?, agrupación por paréntesis y unión |.   Programación "a prueba de balas": Inclusión de una rutina de captura final para interceptar cualquier carácter que no pertenezca al alfabeto válido del lenguaje sin abortar la ejecución.   Código completo de src/main/jflex/com/analizador/lexico/Lexer.flexFragmento de códigopackage com.analizador.lexico;

import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.TablaSimbolos;
import java.util.ArrayList;
import java.util.List;

%%

%public
%class AnalizadorLexicoJS
%unicode
%line
%column
%type Token

%{
private final TablaSimbolos tablaSimbolos;
private final List<ErrorLexico> errores = new ArrayList<>();

    public AnalizadorLexicoJS(java.io.Reader in, TablaSimbolos tablaSimbolos) {
        this(in);
        this.tablaSimbolos = tablaSimbolos;
    }

    public List<ErrorLexico> getErrores() {
        return errores;
    }

    private Token crearToken(TokenType tipo) {
        return new Token(tipo, yytext(), yyline + 1, yycolumn + 1);
    }

    private Token crearToken(TokenType tipo, Object literal) {
        return new Token(tipo, yytext(), yyline + 1, yycolumn + 1, literal);
    }
%}

%eofval{
return new Token(TokenType.EOF, "EOF", yyline + 1, yycolumn + 1);
%eofval}

/* ==================== MACROS (EXPRESIONES REGULARES) ==================== */
FinLinea        = \r|\n|\r\n
EspacioBlanco   = {FinLinea} | [ \t\f]

/* Comentarios simples y multilínea */
ComentarioLinea = "//" [^\r\n]* {FinLinea}?
ComentarioBloque = "/*" ~"*/"
Comentario      = {ComentarioLinea} | {ComentarioBloque}

/* Identificadores y Números en JavaScript */
Letra           = [a-zA-Z_$]
Digito          = [0-9]
Identificador   = {Letra} ({Letra} | {Digito})*
Entero          = 0 | [1-9]{Digito}*
Flotante        = {Entero} "." {Digito}+ ([eE] [\+\-]? {Digito}+)?

/* Literales de Cadena (Comillas dobles o simples con secuencias de escape) */
CadenaDoble     = \" ( [^\"\\\r\n] | \\. )* \"
CadenaSimple    = \' ( [^\'\\\r\n] | \\. )* \'
Cadena          = {CadenaDoble} | {CadenaSimple}

%%

/* ==================== REGLAS LÉXICAS Y ACCIONES ==================== */

<YYINITIAL> {
    /* 1. Ignorar espacios en blanco y comentarios */
    {EspacioBlanco}     { /* Ignorar */ }
    {Comentario}        { /* Ignorar comentario */ }

    /* 2. Palabras Reservadas (DEBEN ir antes que Identificador) */
    "let"               { return crearToken(TokenType.LET); }
    "const"             { return crearToken(TokenType.CONST); }
    "var"               { return crearToken(TokenType.VAR); }
    "function"          { return crearToken(TokenType.FUNCTION); }
    "if"                { return crearToken(TokenType.IF); }
    "else"              { return crearToken(TokenType.ELSE); }
    "while"             { return crearToken(TokenType.WHILE); }
    "for"               { return crearToken(TokenType.FOR); }
    "return"            { return crearToken(TokenType.RETURN); }
    "true"              { return crearToken(TokenType.TRUE, true); }
    "false"             { return crearToken(TokenType.FALSE, false); }
    "null"              { return crearToken(TokenType.NULL); }

    /* 3. Operadores Compuestos y Simples */
    "==="               { return crearToken(TokenType.IGUAL_ESTRICTO); }
    "!=="               { return crearToken(TokenType.DIFERENTE_ESTRICTO); }
    "=="                { return crearToken(TokenType.IGUAL_IGUAL); }
    "!="                { return crearToken(TokenType.DIFERENTE); }
    "<="                { return crearToken(TokenType.MENOR_IGUAL); }
    ">="                { return crearToken(TokenType.MAYOR_IGUAL); }
    "++"                { return crearToken(TokenType.INCREMENTO); }
    "--"                { return crearToken(TokenType.DECREMENTO); }
    "&&"                { return crearToken(TokenType.AND); }
    "||"                { return crearToken(TokenType.OR); }
    "+"                 { return crearToken(TokenType.MAS); }
    "-"                 { return crearToken(TokenType.MENOS); }
    "*"                 { return crearToken(TokenType.MULT); }
    "/"                 { return crearToken(TokenType.DIV); }
    "%"                 { return crearToken(TokenType.MOD); }
    "="                 { return crearToken(TokenType.ASIGNACION); }
    "<"                 { return crearToken(TokenType.MENOR); }
    ">"                 { return crearToken(TokenType.MAYOR); }
    "!"                 { return crearToken(TokenType.NOT); }

    /* 4. Delimitadores */
    "("                 { return crearToken(TokenType.PAREN_IZQ); }
    ")"                 { return crearToken(TokenType.PAREN_DER); }
    "{"                 { return crearToken(TokenType.LLAVE_IZQ); }
    "}"                 { return crearToken(TokenType.LLAVE_DER); }
    "["                 { return crearToken(TokenType.CORCHETE_IZQ); }
    "]"                 { return crearToken(TokenType.CORCHETE_DER); }
    ";"                 { return crearToken(TokenType.PUNTO_COMA); }
    ","                 { return crearToken(TokenType.COMA); }
    ":"                 { return crearToken(TokenType.DOS_PUNTOS); }
    "."                 { return crearToken(TokenType.PUNTO); }

    /* 5. Literales e Identificadores */
    {Flotante}          { return crearToken(TokenType.LIT_FLOTANTE, Double.parseDouble(yytext())); }
    {Entero}            { return crearToken(TokenType.LIT_ENTERO, Long.parseLong(yytext())); }
    {Cadena}            { return crearToken(TokenType.LIT_CADENA, yytext().substring(1, yytext().length() - 1)); }
    {Identificador}     { 
                          tablaSimbolos.registrarIdentificador(yytext(), yyline + 1, yycolumn + 1);
                          return crearToken(TokenType.ID); 
                        }

    /* 6. Captura de Errores Léxicos (Cualquier otro carácter) */
    [^]                 { 
                          errores.add(new ErrorLexico(
                              yytext(), 
                              yyline + 1, 
                              yycolumn + 1, 
                              "Símbolo no perteneciente al alfabeto léxico del lenguaje"
                          )); 
                        }
}
5. Parte 4: Arquitectura de Adaptabilidad al Analizador SintácticoMientras que el análisis léxico divide el programa en unidades lógicas mediante expresiones regulares, el futuro analizador sintáctico (parser) necesitará descubrir la estructura jerárquica del programa usando una Gramática Independiente del Contexto y producir un árbol de análisis sintáctico (árbol de derivación).   5.1. Proveedor de Flujo de Tokens (TokenStream.java)Un analizador sintáctico predictivo descendente LL(1) intenta emparejar símbolos terminales con la entrada mediante llamadas recursivas a procedimientos por cada variable no terminal de la gramática. Para lograrlo sin acoplarse a JFlex, se construye esta clase puente con soporte de lookahead (peek):   Javapackage com.analizador.sintactico;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.Token;
import com.analizador.modelo.TokenType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TokenStream {
private final List<Token> buffer = new ArrayList<>();
private int posicionActual = 0;

    public TokenStream(AnalizadorLexicoJS lexer) throws IOException {
        Token t;
        do {
            t = lexer.yylex();
            buffer.add(t);
        } while (t.tipo() != TokenType.EOF);
    }

    /** Mira el token actual sin consumirlo (Lookahead k=1 para LL(1)) */
    public Token peek() {
        return buffer.get(posicionActual);
    }

    /** Consume y avanza al siguiente token */
    public Token next() {
        Token actual = buffer.get(posicionActual);
        if (actual.tipo() != TokenType.EOF) {
            posicionActual++;
        }
        return actual;
    }

    /** Empareja un terminal esperado por la gramática o lanza error sintáctico */
    public Token match(TokenType tipoEsperado) {
        Token actual = peek();
        if (actual.tipo() == tipoEsperado) {
            return next();
        }
        throw new RuntimeException(String.format(
            "Error Sintáctico en [%d:%d]: Se esperaba %s pero se encontró '%s' (%s)",
            actual.linea(), actual.columna(), tipoEsperado, actual.lexema(), actual.tipo()
        ));
    }

    public List<Token> getTodosLosTokens() {
        return buffer;
    }
}
5.2. Base del Árbol Sintáctico con sealed interface (NodoAST.java)Se deja definida la estructura recursiva para representar expresiones como $E \Rightarrow E + E$:   Javapackage com.analizador.sintactico;

import com.analizador.modelo.Token;
import java.util.List;

public sealed interface NodoAST permits
NodoAST.ExpresionBinaria,
NodoAST.Literal,
NodoAST.DeclaracionVariable,
NodoAST.Programa {

    record Programa(List<NodoAST> sentencias) implements NodoAST {}
    record ExpresionBinaria(NodoAST izquierda, Token operador, NodoAST derecha) implements NodoAST {}
    record Literal(Token token) implements NodoAST {}
    record DeclaracionVariable(Token tipoDeclaracion, Token identificador, NodoAST valorInicial) implements NodoAST {}
}
6. Parte 5: Interfaz Gráfica (Swing) y Suite de Pruebas en JavaScript6.1. Especificación de VentanaPrincipal.javaConstruir un JFrame dividido en tres áreas mediante JSplitPane:Barra Superior (JToolBar):JButton "Cargar .js" (JFileChooser apuntando a src/main/resources/pruebas).JComboBox<String> con selector rápido: "Dificultad Baja", "Dificultad Media", "Dificultad Alta".JButton "Ejecutar Análisis Léxico".Panel Central (JSplitPane horizontal):Izquierda: JTextArea con fuente monoespaciada para editar/visualizar el archivo .js.Derecha (JTabbedPane):Pestaña 1 — Flujo de Tokens (JTable): Columnas [# , Token, Lexema, Línea, Columna, Valor Literal].   Pestaña 2 — Tabla de Símbolos (JTable): Columnas [Identificador, Categoría, 1ra Línea, 1ra Columna, Ocurrencias].   Pestaña 3 — Árbol Sintáctico (JTree): Lista para conectarse a NodoAST en la segunda fase del proyecto.   Panel Inferior (JScrollPane con JTable o JTextArea):Consola de Errores Léxicos que muestra cualquier cadena que haya fallado en el autómata finito.   6.2. Archivos JavaScript de Prueba (Baja, Media y Alta Dificultad)A. Dificultad Baja (algoritmo_bajo.js)Evalúa declaraciones básicas, identificadores simples, números enteros/flotantes y operadores aritméticos:   JavaScript// Cálculo de área y conversión de temperatura
   const PI = 3.14159;
   let radio = 15;
   let areaCirculo = PI * radio * radio;

let celsius = 25.5;
let fahrenheit = (celsius * 1.8) + 32;
B. Dificultad Media (algoritmo_medio.js)Evalúa palabras reservadas de control (function, for, if, else, while, return), paréntesis/corchetes equilibrados y operadores relacionales/incrementales:   JavaScript// Ordenamiento Burbuja (Bubble Sort)
function ordenarArreglo(arr, longitud) {
let i = 0;
while (i < longitud) {
for (let j = 0; j < longitud - i - 1; j++) {
if (arr[j] >= arr[j + 1]) {
let temp = arr[j];
arr[j] = arr[j + 1];
arr[j + 1] = temp;
} else {
// Orden correcto
}
}
i++;
}
return arr;
}
C. Dificultad Alta (algoritmo_alto.js)Evalúa comentarios multilínea, notación científica (1.5e-4), identificadores con $ y _, operadores de igualdad estricta (===, !==), operadores lógicos (&&, ||), objetos (:) y cadenas con caracteres escapados:   JavaScript/*
* Búsqueda Binaria y validación de umbrales con objetos
* Prueba de estrés léxico
  */
  function busquedaBinaria($nodos_lista, _objetivo) {
  let limite_inf = 0;
  let limite_sup = $nodos_lista.length - 1;
  const tolerancia = 1.5e-4;
  let mensaje = "Iniciando búsqueda de: \"Elemento\"";

  while (limite_inf <= limite_sup && $nodos_lista !== null) {
  let medio = (limite_inf + limite_sup) / 2;
  let valorActual = $nodos_lista[medio];

       if (valorActual === _objetivo || (valorActual - _objetivo) < tolerancia) {
           return { encontrado: true, indice: medio, log: mensaje };
       }
       if (valorActual < _objetivo && !false) {
           limite_inf = medio + 1;
       } else {
           limite_sup = medio - 1;
       }
  }
  return { encontrado: false, indice: -1, log: 'No hallado' };
  }
7. Parte 6: Cronograma de Ejecución en 3 Días (Por Bloques Técnicos)Día 1: Cimientos, Entorno Eclipse y Especificación LéxicaBloque 1.1 (Configuración): Creación del repositorio Git, configuración del proyecto Maven en Eclipse con Java 21 y verificación del plugin jflex-maven-plugin.Bloque 1.2 (Modelo Léxico): Codificación de TokenType.java, Token.java, ErrorLexico.java y TablaSimbolos.java.   Bloque 1.3 (Expresiones Regulares): Escritura de la sección de macros en Lexer.flex (espacios, comentarios, números, cadenas e identificadores).   Bloque 1.4 (Banco de Pruebas): Creación de los archivos algoritmo_bajo.js, algoritmo_medio.js y algoritmo_alto.js en la carpeta resources/pruebas.Día 2: Reglas JFlex, Puente Sintáctico e Interfaz GráficaBloque 2.1 (Acciones Léxicas): Codificación de las reglas de retorno de tokens en Lexer.flex, asegurando que las palabras reservadas precedan a los identificadores y los operadores de 3 y 2 caracteres precedan a los de 1 carácter.   Bloque 2.2 (Puente Sintáctico): Implementación de TokenStream.java (peek, next, match) y de la interfaz sellada NodoAST.java para dejar lista la fase de análisis sintáctico.   Bloque 2.3 (Construcción GUI): Desarrollo de VentanaPrincipal.java en Swing con el editor de código, selector de archivos .js y las tablas conectadas a DefaultTableModel.Día 3: Integración, Pruebas de Estrés y DocumentaciónBloque 3.1 (Conexión Total): Enlace del botón "Ejecutar Análisis Léxico" con AnalizadorLexicoJS y TokenStream, poblando en tiempo real la tabla de tokens, la tabla de símbolos y la consola de errores.   Bloque 3.2 (Pruebas de Calidad): Ejecución de los 3 niveles de algoritmos JavaScript e inyección intencional de símbolos inválidos (ej. @, #, `) para comprobar la recuperación de errores léxicos sin caída del programa.   Bloque 3.3 (Empaquetado): Generación del archivo ejecutable .jar desde Eclipse (Run As > Maven build... > package) y redacción del informe técnico con las expresiones regulares utilizadas y el diagrama del AFD generado por JFlex. 