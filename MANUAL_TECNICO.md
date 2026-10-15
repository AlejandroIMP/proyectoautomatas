# Manual Técnico
## Analizador Léxico para JavaScript (Java 21 & JFlex)
### Asignatura: Programación III

---

## 1. Arquitectura del Sistema y Fundamentos Teóricos

### 1.1. Ubicación en la Teoría de Compiladores
El proyecto implementa la primera fase clásica de un compilador: el **Análisis Léxico** o *Scanner*. 

```
+------------------+        +-------------------+        +--------------------+
|  Código Fuente   | -----> | Analizador Léxico | -----> | Analizador         |
|   JavaScript     |        |   (JFlex / AFD)   | Tokens | Sintáctico (Parser)|
+------------------+        +-------------------+        +--------------------+
                                      |                             |
                                      v                             v
                             +-----------------+           +------------------+
                             |    Tabla de     |           |    Árbol de      |
                             |    Símbolos     |           |  Sintaxis (AST)  |
                             +-----------------+           +------------------+
```

* **Jerarquía de Chomsky:** El nivel léxico opera estrictamente mediante **Gramáticas Regulares (Tipo 3)**. Todos los patrones léxicos (palabras clave, operadores, delimitadores, literales e identificadores) son expresables mediante expresiones regulares y reconocibles en tiempo lineal $O(n)$ por un **Autómata Finito Determinista (AFD)** generado automáticamente por JFlex.
* **Separación de Responsabilidades:** El analizador léxico no valida estructuras anidadas ni balance de llaves o paréntesis (tareas que corresponden a una Gramática Independiente del Contexto, Tipo 2, en el analizador sintáctico). Su única labor es transformar el flujo de caracteres en un flujo ordenado de objetos `Token`.

### 1.2. Características Modernas de Java 21 (LTS)
El proyecto aprovecha las capacidades del estándar Java 21:
* **Records (`record`):** Utilizados para modelar entidades de datos inmutables (`Token`, `ErrorLexico`, `EntradaSimbolo`, y variantes de `NodoAST`), reduciendo el código repetitivo (*boilerplate*) y garantizando seguridad en concurrencia e inmutabilidad estricta.
* **Interfaces Selladas (`sealed interface`):** La interfaz `NodoAST` restringe explícitamente qué registros pueden implementarla mediante la cláusula `permits`, asegurando un modelo cerrado para el futuro parser y facilitando el uso de *Pattern Matching for switch*.
* **API de Archivos y Streams (`java.nio.file.Files`, UTF-8):** Manejo robusto de codificación en todo el proyecto.

---

## 2. Estructura de Paquetes y Directorios

```plaintext
AnalizadorLexico/
├── pom.xml                                   <-- Configuración Maven y plugins
├── plan.md                                   <-- Plan de diseño técnico
├── MANUAL_DE_USUARIO.md                      <-- Manual para el usuario final
├── MANUAL_TECNICO.md                         <-- Especificación técnica de ingeniería
├── src/
│   ├── main/
│   │   ├── jflex/
│   │   │   └── com/analizador/lexico/
│   │   │       └── Lexer.flex                <-- Archivo de especificación léxica JFlex
│   │   ├── java/
│   │   │   └── com/analizador/
│   │   │       ├── MainApp.java              <-- Clase principal de entrada
│   │   │       ├── gui/
│   │   │       │   └── VentanaPrincipal.java <-- Interfaz gráfica en Swing
│   │   │       ├── modelo/
│   │   │       │   ├── TokenType.java        <-- Enumeración de componentes léxicos
│   │   │       │   ├── Token.java            <-- Record de token con posición
│   │   │       │   ├── ErrorLexico.java      <-- Record de error léxico
│   │   │       │   └── TablaSimbolos.java    <-- Gestor de identificadores
│   │   │       └── sintactico/
│   │   │           ├── TokenStream.java      <-- Buffer y adaptador LL(1)
│   │   │           └── NodoAST.java          <-- Jerarquía sellada del AST
│   │   └── resources/
│   │       └── pruebas/                      <-- Banco de pruebas JavaScript
│   │           ├── algoritmo_bajo.js
│   │           ├── algoritmo_medio.js
│   │           ├── algoritmo_alto.js
│   │           ├── algoritmo_muy_complejo.js
│   │           └── algoritmo_errores.js
│   └── test/
│       └── java/
│           └── com/analizador/
│               └── AnalizadorLexicoTest.java <-- Suite automatizada JUnit 5
└── target/
    ├── generated-sources/
    │   └── jflex/
    │       └── com/analizador/lexico/
    │           └── AnalizadorLexicoJS.java   <-- Clase Java generada por JFlex
    └── analizador-js-1.0.0.jar               <-- Artefacto ejecutable empaquetado
```

---

## 3. Especificación de Componentes

### 3.1. Modelo Léxico (`com.analizador.modelo`)

#### `TokenType.java`
Enumeración que clasifica formalmente los tokens en:
* **Palabras reservadas:** `LET`, `CONST`, `VAR`, `FUNCTION`, `IF`, `ELSE`, `WHILE`, `FOR`, `RETURN`, `TRUE`, `FALSE`, `NULL`.
* **Identificadores y literales:** `ID`, `LIT_ENTERO`, `LIT_FLOTANTE`, `LIT_CADENA`.
* **Operadores aritméticos y de asignación:** `MAS` (`+`), `MENOS` (`-`), `MULT` (`*`), `DIV` (`/`), `MOD` (`%`), `ASIGNACION` (`=`), `INCREMENTO` (`++`), `DECREMENTO` (`--`).
* **Operadores relacionales y lógicos:** `IGUAL_IGUAL` (`==`), `IGUAL_ESTRICTO` (`===`), `DIFERENTE` (`!=`), `DIFERENTE_ESTRICTO` (`!==`), `MENOR` (`<`), `MAYOR` (`>`), `MENOR_IGUAL` (`<=`), `MAYOR_IGUAL` (`>=`), `AND` (`&&`), `OR` (`||`), `NOT` (`!`).
* **Delimitadores y puntuación:** `PAREN_IZQ` (`(`), `PAREN_DER` (`)`), `LLAVE_IZQ` (`{`), `LLAVE_DER` (`}`), `CORCHETE_IZQ` (`[`), `CORCHETE_DER` (`]`), `PUNTO_COMA` (`;`), `COMA` (`,`), `DOS_PUNTOS` (`:`), `PUNTO` (`.`).
* **Control de flujo del analizador:** `EOF` (fin de archivo).

#### `Token.java`
Record inmutable de coordenadas y atributos:
```java
public record Token(
    TokenType tipo,
    String lexema,
    int linea,
    int columna,
    Object valorLiteral
) {
    public Token(TokenType tipo, String lexema, int linea, int columna) {
        this(tipo, lexema, linea, columna, null);
    }
}
```

#### `ErrorLexico.java`
Record que encapsula la información de fallas léxicas:
```java
public record ErrorLexico(
    String lexemaInvalido,
    int linea,
    int columna,
    String descripcion
) {}
```

#### `TablaSimbolos.java`
Gestor en memoria para el catálogo de identificadores. Emplea un `LinkedHashMap` para preservar el orden cronológico de primera aparición y utiliza el método atómico `compute` para actualizar la cantidad de ocurrencias:
```java
public class TablaSimbolos {
    public record EntradaSimbolo(String nombre, String categoria, int primeraLinea,
                                 int primeraColumna, int ocurrencias) {}
    private final Map<String, EntradaSimbolo> tabla = new LinkedHashMap<>();

    public void registrarIdentificador(String nombre, int linea, int columna) {
        tabla.compute(nombre, (k, v) -> v == null
                ? new EntradaSimbolo(nombre, "IDENTIFICADOR", linea, columna, 1)
                : new EntradaSimbolo(nombre, v.categoria(), v.primeraLinea(),
                    v.primeraColumna(), v.ocurrencias() + 1));
    }
    public Collection<EntradaSimbolo> obtenerTodos() { return tabla.values(); }
    public void limpiar() { tabla.clear(); }
}
```

---

### 3.2. Motor Léxico JFlex (`Lexer.flex`)

El archivo de especificación contiene tres secciones separadas por `%%`:

#### Directivas y Código de Usuario (Sección 1)
* `%public`, `%class AnalizadorLexicoJS`: Declara la visibilidad y nombre de la clase generada.
* `%unicode`: Permite procesamiento del juego completo de caracteres Unicode.
* `%line`, `%column`: Activa el seguimiento de números de línea (`yyline`) y columna (`yycolumn`).
* `%type Token`: Define que el método `yylex()` retorna instancias de `Token`.
* `%eofval{ ... %eofval}`: Genera automáticamente el token `TokenType.EOF` con sus coordenadas al finalizar la lectura.

#### Macros y Expresiones Regulares (Sección 2)
```jflex
LineTerminator = \r|\n|\r\n
WhiteSpace     = [ \t\f] | {LineTerminator}
LineComment    = "//" [^\r\n]* {LineTerminator}?
BlockComment   = "/*" ([^*]|\*+[^*/])* \*+ "/"
Letter         = [a-zA-Z_$]
Digit          = [0-9]
Identifier     = {Letter}({Letter}|{Digit})*
Integer        = 0 | [1-9]{Digit}*
Float          = ({Integer}"."{Digit}+|{Integer}("."{Digit}+)?[eE][+-]?{Digit}+)
DoubleString   = \"([^\"\\\r\n]|\\.)*\"
SingleString   = \'([^\\'\\\r\n]|\\.)*\'
String         = {DoubleString}|{SingleString}
```

#### Reglas Léxicas y Acciones (Sección 3)
1. **Espacios y Comentarios:** Se ignoran sin generar tokens pero incrementan los contadores de posición.
2. **Prioridad de Palabras Reservadas:** Reglas explícitas (`"let"`, `"const"`, `"function"`, etc.) declaradas **antes** que `{Identifier}`. Si una secuencia coincide con `else`, JFlex resolverá la ambigüedad asociándola a la palabra reservada en lugar de un identificador genérico.
3. **Prioridad de Operadores Compuestos:** Reglas como `"==="` y `"!=="` se ubican antes de `"=="` y `"="` para garantizar el emparejamiento con el prefijo más largo (*longest match*).
4. **Captura de Errores Léxicos:** Regla de respaldo:
   ```jflex
   [^] {
       errores.add(new ErrorLexico(yytext(), yyline + 1, yycolumn + 1,
               "Símbolo no perteneciente al alfabeto léxico"));
   }
   ```
   Esta regla captura cualquier carácter no reconocido por las reglas anteriores, añade el incidente a la lista de errores y permite al AFD continuar con el siguiente carácter.

---

### 3.3. Puente hacia el Analizador Sintáctico (`com.analizador.sintactico`)

#### `TokenStream.java`
Desacopla el analizador sintáctico del lexer de JFlex mediante un buffer de tokens que implementa las operaciones necesarias para analizadores predictivos descendentes LL(1):
* `peek()`: Inspecciona el token actual sin consumirlo (Lookahead $k=1$).
* `next()`: Consume el token actual y avanza el cursor de posición.
* `match(TokenType esperado)`: Verifica que el token actual coincida con la expectativa gramatical; en caso afirmativo avanza, de lo contrario lanza una excepción controlada.
* `getTodosLosTokens()`: Retorna una copia inmutable del flujo completo de tokens.

#### `NodoAST.java`
Estructura jerárquica con interfaces selladas para representar las sentencias y expresiones:
```java
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
```

---

## 4. Configuración del Entorno Maven (`pom.xml`)

El archivo de configuración gestiona tres plugins esenciales:

1. **`jflex-maven-plugin` (v1.9.1):**
   * Fase: `generate-sources`.
   * Acción: Toma `Lexer.flex` y genera `target/generated-sources/jflex/com/analizador/lexico/AnalizadorLexicoJS.java`.
2. **`build-helper-maven-plugin` (v3.5.0):**
   * Acción: Añade `target/generated-sources/jflex` al conjunto de carpetas de código fuente del proyecto para que tanto Maven como IntelliJ IDEA y Eclipse reconozcan la clase generada sin configuración manual adicional.
3. **`maven-jar-plugin` (v3.3.0):**
   * Acción: Agrega el atributo `Main-Class: com.analizador.MainApp` al archivo `META-INF/MANIFEST.MF`, permitiendo la ejecución directa del `.jar`.
4. **`junit-jupiter` (v5.10.2):**
   * Alcance: `test`. Soporte para ejecución de pruebas unitarias modernas.

---

## 5. Compilación, Pruebas y Despliegue

### Comandos de Terminal

* **Limpiar y compilar el proyecto:**
  ```bash
  mvn clean compile
  ```
* **Ejecutar la suite completa de pruebas unitarias:**
  ```bash
  mvn test
  ```
* **Empaquetar la aplicación en un archivo `.jar` ejecutable:**
  ```bash
  mvn clean package
  ```
  El artefacto resultante se ubicará en `target/analizador-js-1.0.0.jar`.
* **Ejecutar el archivo `.jar` empaquetado:**
  ```bash
  java -jar target/analizador-js-1.0.0.jar
  ```

---

## 6. Cobertura de Pruebas Unitarias (`AnalizadorLexicoTest.java`)

La clase de pruebas incluye 6 casos automatizados con JUnit 5:

1. `testAlgoritmoBajo()`: Valida variables, constantes (`const`, `let`), operaciones aritméticas elementales, verificación de fin de archivo (`EOF`) y presencia de identificadores en la tabla de símbolos.
2. `testAlgoritmoMedio()`: Valida palabras clave de control de flujo (`function`, `while`, `for`, `if`, `else`, `return`), operadores relacionales (`>=`) e incremento (`++`).
3. `testAlgoritmoAlto()`: Valida notación científica (`1.5e-4`), identificadores que inician con `$` y `_`, operadores de igualdad estricta (`===`, `!==`) y cadenas con comillas escapadas (`\"`).
4. `testAlgoritmoMuyComplejo()`: Prueba de estrés con el algoritmo de Dijkstra y multiplicación de matrices, verificando más de 150 tokens, comentarios multilínea extensos y precisión en coordenadas.
5. `testRecuperacionErroresLexicos()`: Inyecta caracteres ilegales (`@`, `#`, `~`, `^`, `?`) y comprueba que la lista de errores los captura de forma independiente con línea y columna exactas, sin impedir que los tokens válidos posteriores sean procesados.
6. `testTokenStreamBridge()`: Valida los métodos `peek()`, `next()`, `match()` exitoso y la generación de excepciones ante discrepancias léxicas.

---

## 7. Extensibilidad hacia la Fase Sintáctica

Para integrar la siguiente fase (analizador sintáctico):
1. **Crear la clase `ParserJS`:**
   ```java
   public class ParserJS {
       private final TokenStream stream;
       public ParserJS(TokenStream stream) { this.stream = stream; }
       public NodoAST parsePrograma() { ... }
   }
   ```
2. **Implementar las funciones de derivación recursiva:**
   Utilizar `stream.peek()` para seleccionar la regla de producción gramatical y `stream.match(...)` para consumir los terminales esperados.
3. **Construir el Árbol AST:**
   Instanciar los registros de `NodoAST` (`Programa`, `ExpresionBinaria`, etc.) y enlazarlos al componente `JTree` de `VentanaPrincipal`.
