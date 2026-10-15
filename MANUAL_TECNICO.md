# Manual Técnico
## Analizador Léxico para JavaScript (Java 21 & JFlex)
### Asignatura: Programación III

---

## 1. Arquitectura del Sistema y Fundamentos Teóricos

### 1.1. Ubicación en la Teoría de Compiladores
El proyecto implementa la primera fase clásica de un compilador: el **Análisis Léxico** o *Scanner*, complementado con un módulo de diagnóstico y generación de reportes contextualizados.

```
+------------------+        +-------------------+        +--------------------+
|  Código Fuente   | -----> | Analizador Léxico | -----> | Analizador         |
|   JavaScript     |        |   (JFlex / AFD)   | Tokens | Sintáctico (Parser)|
+------------------+        +-------------------+        +--------------------+
                                      |                             |
                                      +--------------------------+  v
                                      |                          | +------------------+
                                      v                          | |    Árbol de      |
                             +-----------------+                 | |  Sintaxis (AST)  |
                             |    Tabla de     |                 | +------------------+
                             |    Símbolos     |                 |
                             +-----------------+                 v
                                      |                 +--------------------+
                                      +---------------> | Módulo de Reportes |
                                                        |  (HTML / Markdown) |
                                                        +--------------------+
```

* **Jerarquía de Chomsky:** El nivel léxico opera estrictamente mediante **Gramáticas Regulares (Tipo 3)**. Todos los patrones léxicos (palabras clave, operadores, delimitadores, literales e identificadores) son expresables mediante expresiones regulares y reconocibles en tiempo lineal $O(n)$ por un **Autómata Finito Determinista (AFD)** generado automáticamente por JFlex.
* **Separación de Responsabilidades:** El analizador léxico no valida estructuras anidadas ni balance de llaves o paréntesis (tareas que corresponden a una Gramática Independiente del Contexto, Tipo 2, en el analizador sintáctico). Su única labor es transformar el flujo de caracteres en un flujo ordenado de objetos `Token`.

### 1.2. Características Modernas de Java 21 (LTS)
El proyecto aprovecha las capacidades del estándar Java 21:
* **Records (`record`):** Utilizados para modelar entidades de datos inmutables (`Token`, `ErrorLexico`, `EntradaSimbolo`, `MetricasReporte`, y variantes de `NodoAST`), reduciendo el código repetitivo (*boilerplate*) y garantizando inmutabilidad estricta.
* **Interfaces Selladas (`sealed interface`):** La interfaz `NodoAST` restringe explícitamente qué registros pueden implementarla mediante la cláusula `permits`, asegurando un modelo cerrado para el futuro parser y facilitando el uso de *Pattern Matching for switch*.
* **Stream API y Procesamiento Funcional:** Agrupamiento (`Collectors.groupingBy`), conteo por categorías y cálculo estadístico en el módulo de reportes.
* **API de Archivos y Codificación (`java.nio.file.Files`, UTF-8):** Manejo robusto de archivos en todo el proyecto.

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
│   │   │       ├── reporte/
│   │   │       │   └── GeneradorReporte.java <-- Módulo de métricas y exportación HTML/MD
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
│               ├── AnalizadorLexicoTest.java <-- Suite léxica JUnit 5
│               └── reporte/
│                   └── GeneradorReporteTest.java <-- Pruebas de reportes JUnit 5
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
Gestor en memoria para el catálogo de identificadores. Emplea un `LinkedHashMap` para preservar el orden cronológico de primera aparición y utiliza el método atómico `compute` para actualizar la cantidad de ocurrencias.

---

### 3.2. Motor Léxico JFlex (`Lexer.flex`)

El archivo de especificación contiene tres secciones separadas por `%%`:
* **Directivas:** `%public`, `%class AnalizadorLexicoJS`, `%unicode`, `%line`, `%column`, `%type Token`.
* **Macros Regex:** Reglas para números enteros, flotantes (notación científica `[eE]`), cadenas con escapes simples y dobles (`\"`, `\'`), comentarios de línea y bloque (`/* ... */`).
* **Acciones:**
  * Precedencia de palabras clave sobre identificadores.
  * Precedencia de operadores compuestos sobre simples (`===` sobre `==` sobre `=`).
  * Regla final de captura de errores `[^]` para no abortar el escaneo.

---

### 3.3. Módulo de Reportes Contextualizados (`com.analizador.reporte`)

#### `GeneradorReporte.java`
Ofrece las siguientes responsabilidades desacopladas de la interfaz gráfica:
1. **`calcularMetricas(...)`**: Calcula estadísticas avanzadas mediante Streams de Java: total de líneas, caracteres, desglose por familias de tokens (palabras clave, operadores, delimitadores, literales), frecuencias y top 5 de identificadores más utilizados.
2. **`generarHTML(...)`**: Genera un documento HTML5 completo y autocontenido con estilos CSS modernos (tarjetas métricas, tablas de distribución con porcentajes, etiquetas de colores, código resaltado y diagnósticos).
3. **`generarMarkdown(...)`**: Genera un documento Markdown con tablas y bloques de código, ideal para documentación técnica en repositorios Git.
4. **`generarHTMLParaSwing(...)`**: Genera un resumen HTML ligero compatible con `JEditorPane` de Swing para visualizar el reporte dentro de la propia interfaz gráfica.
5. **`guardarEnArchivo(...)`**: Escribe el contenido en disco de forma segura utilizando codificación UTF-8.

---

### 3.4. Puente hacia el Analizador Sintáctico (`com.analizador.sintactico`)

* **`TokenStream.java`**: Buffer desacoplado con lookahead $k=1$ (`peek()`), avance (`next()`) y emparejamiento con validación (`match(TokenType)`).
* **`NodoAST.java`**: Jerarquía con interfaces selladas (`sealed interface`) para modelar las expresiones binarias, literales, declaraciones y programas.

---

## 4. Configuración del Entorno Maven (`pom.xml`)

1. **`jflex-maven-plugin` (v1.9.1):** Compilación automática de `Lexer.flex` en la fase `generate-sources`.
2. **`build-helper-maven-plugin` (v3.5.0):** Agrega `target/generated-sources/jflex` como carpeta de fuentes para IDEs y compilador.
3. **`maven-jar-plugin` (v3.3.0):** Genera el manifest con `Main-Class: com.analizador.MainApp`.
4. **`junit-jupiter` (v5.10.2):** Dependencia de pruebas unitarias.

---

## 5. Compilación, Pruebas y Despliegue

```bash
# Compilar proyecto y generar código de JFlex
mvn clean compile

# Ejecutar las 11 pruebas unitarias automatizadas
mvn test

# Generar archivo ejecutable JAR
mvn clean package

# Ejecutar la aplicación
java -jar target/analizador-js-1.0.0.jar
```

---

## 6. Cobertura de Pruebas Unitarias

El proyecto cuenta con **11 pruebas automatizadas** que se ejecutan con `mvn test`:

### Suite de Análisis Léxico (`AnalizadorLexicoTest.java`)
1. `testAlgoritmoBajo()`: Valida variables, constantes y operadores elementales.
2. `testAlgoritmoMedio()`: Valida estructuras de control y funciones.
3. `testAlgoritmoAlto()`: Valida notación científica, identificadores con `$` y `_`, y secuencias de escape.
4. `testAlgoritmoMuyComplejo()`: Prueba de estrés con el algoritmo de Dijkstra y matrices (más de 150 tokens).
5. `testRecuperacionErroresLexicos()`: Valida detección de símbolos prohibidos (`@`, `#`, `~`, `^`, `?`) sin abortar el escaneo.
6. `testTokenStreamBridge()`: Valida operaciones predictivas LL(1) (`peek`, `next`, `match`).

### Suite del Módulo de Reportes (`GeneradorReporteTest.java`)
7. `testMetricasScriptLimpio()`: Valida el cálculo de líneas, tokens y distribución de categorías.
8. `testGenerarHTML()`: Valida la estructura de etiquetas, metadatos y secciones del reporte HTML5.
9. `testGenerarMarkdown()`: Valida la consistencia de tablas y encabezados Markdown.
10. `testReporteConErrores()`: Valida que los errores léxicos se reflejen en los reportes HTML y MD.
11. `testGenerarHTMLParaSwing()`: Valida la generación de HTML compatible con el visor interno de Swing.
