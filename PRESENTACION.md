# Guía de presentación del Analizador Léxico para JavaScript

## 1. Descripción general

Esta aplicación implementa la primera fase de un compilador: el **análisis léxico**.

Su objetivo es recibir código fuente escrito en JavaScript y transformarlo en información estructurada:

```text
Código fuente
      ↓
Analizador léxico JFlex
      ↓
Tokens
      ↓
Tabla de símbolos y errores léxicos
      ↓
Métricas y reportes
```

La aplicación cuenta con una interfaz gráfica desarrollada con Java Swing. Permite cargar o escribir código JavaScript, ejecutar el análisis, consultar los tokens reconocidos, revisar los identificadores encontrados, visualizar errores y exportar un reporte en HTML o Markdown.

### Explicación corta para la presentación

> Esta aplicación analiza léxicamente código JavaScript. Identifica palabras reservadas, identificadores, literales, operadores y delimitadores utilizando reglas regulares generadas con JFlex. Además, registra los identificadores en una tabla de símbolos, detecta caracteres no válidos sin detener completamente el procesamiento y genera reportes con métricas del código analizado.

## 2. ¿Qué es el análisis léxico?

El análisis léxico es la primera etapa tradicional de un compilador. Lee una secuencia de caracteres y la agrupa en unidades significativas llamadas **tokens**.

Por ejemplo, para el código:

```javascript
const radio = 10;
```

El analizador produce:

| Lexema | Tipo de token |
|---|---|
| `const` | `CONST` |
| `radio` | `ID` |
| `=` | `ASIGNACION` |
| `10` | `LIT_ENTERO` |
| `;` | `PUNTO_COMA` |

Cada token contiene:

- Tipo.
- Lexema original.
- Línea.
- Columna.
- Valor literal, cuando aplica.

El token `10`, por ejemplo, conserva el texto original `"10"` como lexema y también su valor convertido a un número entero (`Long`).

## 3. Qué sí hace y qué no hace la aplicación

### Funciones que sí realiza

- Reconoce elementos léxicos de un subconjunto de JavaScript.
- Identifica palabras reservadas.
- Reconoce identificadores, incluyendo nombres con `_` y `$`.
- Reconoce números enteros, flotantes y notación científica.
- Reconoce cadenas simples, dobles y template literals.
- Reconoce operadores aritméticos, relacionales y lógicos.
- Reconoce delimitadores y signos de puntuación.
- Ignora comentarios de línea y comentarios de bloque.
- Conserva la línea y columna de cada token.
- Registra identificadores en una tabla de símbolos.
- Detecta símbolos que no pertenecen al alfabeto léxico.
- Continúa el análisis después de muchos errores léxicos.
- Calcula métricas del código.
- Exporta reportes HTML y Markdown.

### Funciones que todavía no realiza

- No ejecuta el código JavaScript.
- No interpreta ni calcula el resultado de las instrucciones.
- No realiza análisis semántico.
- No verifica tipos.
- No valida completamente la gramática de JavaScript.
- No comprueba todas las reglas de alcance o declaración de variables.
- No construye todavía un árbol sintáctico completo a partir del código.

La pestaña **Árbol Sintáctico** y las clases del paquete `sintactico` preparan el proyecto para una futura fase de análisis sintáctico.

## 4. Arquitectura del proyecto

La estructura principal es:

```text
AnalizadorLexico/
├── pom.xml
├── PRESENTACION.md
├── MANUAL_DE_USUARIO.md
├── MANUAL_TECNICO.md
├── src/
│   ├── main/
│   │   ├── java/com/analizador/
│   │   │   ├── MainApp.java
│   │   │   ├── gui/VentanaPrincipal.java
│   │   │   ├── modelo/
│   │   │   ├── reporte/
│   │   │   └── sintactico/
│   │   ├── jflex/
│   │   │   └── com/analizador/lexico/Lexer.flex
│   │   └── resources/pruebas/
│   └── test/java/
└── target/
```

### Componentes principales

| Componente | Responsabilidad |
|---|---|
| `MainApp` | Punto de entrada de la aplicación |
| `VentanaPrincipal` | Interfaz gráfica y coordinación del flujo |
| `Lexer.flex` | Reglas léxicas escritas con expresiones regulares |
| `AnalizadorLexicoJS` | Lexer Java generado automáticamente por JFlex |
| `TokenType` | Enumeración de tipos de tokens |
| `Token` | Representación inmutable de un token |
| `ErrorLexico` | Representación de un error léxico |
| `TablaSimbolos` | Registro de identificadores y ocurrencias |
| `TokenStream` | Flujo con operaciones `peek`, `next` y `match` |
| `NodoAST` | Estructura preparada para el árbol sintáctico |
| `GeneradorReporte` | Métricas y generación de reportes |

## 5. Motor léxico con JFlex

El archivo que contiene la definición del lexer es:

```text
src/main/jflex/com/analizador/lexico/Lexer.flex
```

JFlex lee este archivo y genera automáticamente:

```text
target/generated-sources/jflex/com/analizador/lexico/AnalizadorLexicoJS.java
```

La clase generada no debe editarse manualmente. Los cambios deben realizarse en `Lexer.flex` y después volver a compilar el proyecto.

### Reglas importantes del lexer

El archivo define patrones para:

- Espacios y saltos de línea.
- Comentarios de una línea (`//`).
- Comentarios de bloque (`/* ... */`).
- Identificadores.
- Enteros.
- Números flotantes.
- Notación científica.
- Cadenas con comillas simples o dobles.
- Template literals.
- Operadores.
- Delimitadores.

También define acciones para devolver objetos `Token`, registrar identificadores y crear objetos `ErrorLexico`.

### Precedencia de reglas

El lexer aplica reglas específicas antes que reglas generales. Esto es especialmente importante para:

```text
=== antes que == antes que =
!== antes que !=
++ antes que +
-- antes que -
```

De esa forma, un operador compuesto no se divide incorrectamente en varios operadores simples.

## 6. Tipos de tokens reconocidos

### Palabras reservadas

```text
LET, CONST, VAR, FUNCTION, IF, ELSE,
WHILE, FOR, RETURN, TRUE, FALSE, NULL
```

### Identificadores y literales

```text
ID
LIT_ENTERO
LIT_FLOTANTE
LIT_CADENA
LIT_TEMPLATE
```

Ejemplos:

```javascript
contador
$nodos_lista
_objetivo
42
3.1415
1.5e-4
"Hola"
`Texto ${valor}`
```

### Operadores

```text
MAS, MENOS, MULT, DIV, MOD
ASIGNACION
INCREMENTO, DECREMENTO
IGUAL_IGUAL, IGUAL_ESTRICTO
DIFERENTE, DIFERENTE_ESTRICTO
MENOR, MAYOR, MENOR_IGUAL, MAYOR_IGUAL
AND, OR, NOT
```

### Delimitadores

```text
PAREN_IZQ, PAREN_DER
LLAVE_IZQ, LLAVE_DER
CORCHETE_IZQ, CORCHETE_DER
PUNTO_COMA, COMA, DOS_PUNTOS, PUNTO
```

Al terminar el archivo se genera el token especial `EOF`, que indica el final del flujo.

## 7. Tabla de símbolos

La tabla de símbolos se implementa en:

```text
src/main/java/com/analizador/modelo/TablaSimbolos.java
```

Para cada identificador se guarda:

- Nombre.
- Categoría.
- Primera línea.
- Primera columna.
- Número de ocurrencias.

Ejemplo:

```javascript
let contador = 0;
contador++;
```

Resultado esperado:

| Identificador | Categoría | Primera línea | Primera columna | Ocurrencias |
|---|---|---:|---:|---:|
| `contador` | `IDENTIFICADOR` | 1 | 5 | 2 |

La implementación utiliza un `LinkedHashMap`, por lo que mantiene el orden de aparición de los identificadores.

Una precisión importante: la tabla registra identificadores cuando aparecen en el código, tanto en declaraciones como en usos posteriores. No es todavía una tabla semántica completa con tipos, ámbitos o validación de declaraciones.

## 8. Manejo y recuperación de errores

Cuando el lexer encuentra un carácter que no pertenece al alfabeto definido, crea un objeto `ErrorLexico` con:

- Lexema inválido.
- Línea.
- Columna.
- Descripción del problema.

Por ejemplo:

```javascript
let total = 10 @ 2;
```

El símbolo `@` se reporta como un carácter no perteneciente al alfabeto léxico.

El lexer agrega el error a una lista y continúa procesando los siguientes caracteres. Esto permite mostrar todos los problemas encontrados en una sola ejecución, en lugar de detenerse en el primer error.

### Comentario de bloque sin cerrar

Cuando encuentra un comentario que comienza con `/*` pero no termina con `*/`, el lexer registra un error y cambia a un estado especial para descartar el contenido restante.

Este comportamiento evita interpretar accidentalmente el resto del archivo como código normal.

## 9. Interfaz gráfica

La interfaz está definida en:

```text
src/main/java/com/analizador/gui/VentanaPrincipal.java
```

### Barra superior

#### `Cargar .js`

Abre un selector de archivos para cargar un archivo JavaScript externo.

#### Selector de dificultad

Carga automáticamente ejemplos incluidos en el proyecto:

- Dificultad Baja.
- Dificultad Media.
- Dificultad Alta.
- Dificultad Muy Compleja.
- Prueba de Errores Léxicos.

#### `Ejecutar Análisis Léxico`

Vuelve a analizar el contenido actual del editor. Es útil después de modificar manualmente el código.

#### `Exportar Reporte`

Genera y guarda un reporte en HTML o Markdown.

### Editor de código

El panel izquierdo permite escribir, pegar o modificar código JavaScript. Utiliza una fuente monoespaciada para facilitar la lectura y conservar la alineación.

### Pestaña Flujo de Tokens

Muestra:

- Número consecutivo.
- Tipo del token.
- Lexema.
- Línea.
- Columna.
- Valor literal.

### Pestaña Tabla de Símbolos

Muestra los identificadores encontrados, su primera ubicación y las veces que aparecen.

### Pestaña Informe Ejecutivo

Muestra:

- Estado del análisis.
- Líneas de código.
- Caracteres.
- Tokens.
- Identificadores.
- Palabras reservadas.
- Operadores.
- Delimitadores.
- Literales.
- Errores.

También indica si el flujo de tokens está listo para una futura fase sintáctica.

### Pestaña Árbol Sintáctico

Actualmente representa una preparación para el parser. La estructura de datos `NodoAST` ya contempla nodos como:

- Programa.
- Declaración de variable.
- Literal.
- Expresión binaria.

Sin embargo, todavía no existe un parser completo que construya el árbol a partir de todos los tokens.

### Consola de errores

La tabla inferior muestra cada error con su lexema, ubicación y descripción.

## 10. Generación de reportes

El módulo se encuentra en:

```text
src/main/java/com/analizador/reporte/GeneradorReporte.java
```

### Métricas calculadas

- Total de líneas.
- Total de caracteres.
- Total de tokens reales, sin contar `EOF`.
- Identificadores únicos.
- Ocurrencias totales de identificadores.
- Palabras reservadas.
- Operadores.
- Delimitadores.
- Literales.
- Frecuencia de cada tipo de token.
- Los cinco identificadores más utilizados.
- Total de errores léxicos.

### Reporte HTML

El reporte HTML es autocontenido e incluye:

- Encabezado del análisis.
- Estado exitoso o estado con errores.
- Tarjetas de métricas.
- Distribución de tokens.
- Tabla de errores.
- Tabla de símbolos.
- Fragmento del código fuente.
- Diagnóstico para la fase sintáctica.

### Reporte Markdown

El reporte Markdown es útil para:

- Documentación técnica.
- Entrega de laboratorio.
- Repositorios Git.
- Informes escritos.

## 11. Flujo interno de ejecución

Cuando el usuario presiona **Ejecutar Análisis Léxico**, ocurre lo siguiente:

1. Se limpian las tablas anteriores.
2. Se crea una nueva `TablaSimbolos`.
3. El texto del editor se envuelve en un `StringReader`.
4. Se crea `AnalizadorLexicoJS`.
5. El lexer reconoce tokens y registra identificadores.
6. `TokenStream` consume el flujo completo.
7. Se guarda la lista de tokens.
8. Se recuperan los errores léxicos.
9. Se actualizan las tablas de la interfaz.
10. Se calculan las métricas.
11. Se actualiza el informe ejecutivo.

El punto de entrada de la aplicación es:

```text
src/main/java/com/analizador/MainApp.java
```

La interfaz se inicia dentro de `SwingUtilities.invokeLater`, siguiendo el modelo recomendado para trabajar con Swing.

## 12. Puente hacia el análisis sintáctico

`TokenStream` funciona como un adaptador entre el lexer y un futuro parser. Proporciona:

- `peek()`: consulta el siguiente token sin consumirlo.
- `next()`: consume y devuelve el siguiente token.
- `match(TokenType)`: verifica que el siguiente token sea del tipo esperado y lo consume.

Ejemplo conceptual:

```text
peek()  → observa LET sin consumirlo
next()  → consume LET
match(ID) → espera un identificador
match(ASIGNACION) → espera =
```

Si `match` recibe un tipo inesperado, lanza una excepción controlada. Esto facilita la implementación de un parser predictivo LL(1).

## 13. Ejemplo de demostración

Puedes usar este código durante la presentación:

```javascript
const radio = 10;
let area = 3.1415 * radio * radio;

if (area >= 100) {
    console.log("Área grande");
} else {
    console.log("Área pequeña");
}
```

### Qué explicar mientras se analiza

1. `const`, `let` e `if` son palabras reservadas.
2. `radio` y `area` son identificadores.
3. `3.1415` es un literal flotante.
4. `100` es un literal entero.
5. `*` y `>=` son operadores.
6. Los paréntesis y llaves son delimitadores.
7. Las cadenas se reconocen como literales.
8. Cada token conserva su línea y columna.
9. `radio` aparece más de una vez en la tabla de símbolos.

## 14. Demostración de errores

Después de mostrar un caso correcto, utiliza:

```javascript
let resultado = 20 @ 4;
let estado = true;
```

El sistema debe:

- Reconocer los tokens válidos.
- Mostrar `@` en la consola de errores.
- Indicar la línea y columna del error.
- Continuar procesando `estado` y `true`.
- Mostrar el estado `CON ERRORES LÉXICOS` en el informe.

También se puede seleccionar directamente:

```text
Prueba Errores Léxicos
```

## 15. Archivos de prueba incluidos

Los ejemplos se encuentran en:

```text
src/main/resources/pruebas/
```

| Archivo | Objetivo |
|---|---|
| `algoritmo_bajo.js` | Variables, constantes y operadores básicos |
| `algoritmo_medio.js` | Funciones, ciclos, arreglos y condicionales |
| `algoritmo_alto.js` | Notación científica, escapes, `$`, `_` y comparaciones estrictas |
| `algoritmo_muy_complejo.js` | Dijkstra, matrices, comentarios y alto volumen de tokens |
| `algoritmo_errores.js` | Recuperación ante símbolos no válidos |
| `algoritmo_errores_extenso.js` | Prueba amplia de símbolos inválidos, Unicode, identificadores numéricos y comentario sin cerrar |

## 16. Tecnologías utilizadas

- **Java 21 LTS**: lenguaje principal.
- **Java Swing**: interfaz gráfica.
- **JFlex 1.9.1**: generación automática del lexer.
- **Maven**: compilación y empaquetado.
- **JUnit 5**: pruebas automatizadas.
- **Java Records**: objetos de datos inmutables.
- **Sealed interfaces**: jerarquía restringida para el AST.
- **Streams de Java**: cálculo de métricas y agrupamiento.
- **UTF-8**: lectura y escritura de archivos.

## 17. Compilación y ejecución

Desde la raíz del proyecto:

```bash
mvn clean compile
```

Para ejecutar las pruebas:

```bash
mvn test
```

Para generar el JAR:

```bash
mvn clean package
```

Para ejecutar la aplicación:

```bash
java -jar target/analizador-js-1.0.0.jar
```

También se puede ejecutar la clase:

```text
com.analizador.MainApp
```

### Qué hace Maven

El archivo `pom.xml` configura:

1. La versión de Java.
2. El plugin de JFlex.
3. La inclusión del código generado.
4. El empaquetado del JAR ejecutable.
5. La dependencia de JUnit.

## 18. Pruebas automatizadas

Las pruebas verifican diferentes niveles del proyecto:

- Reconocimiento de variables y constantes.
- Operadores básicos.
- Estructuras de control.
- Funciones y ciclos.
- Notación científica.
- Identificadores con `$` y `_`.
- Cadenas con escapes.
- Template literals.
- Comentarios multilinea.
- Recuperación de errores léxicos.
- Operaciones de `TokenStream`.
- Consultas a la tabla de símbolos.
- Cálculo de métricas.
- Generación de HTML.
- Generación de Markdown.

Las pruebas son importantes porque comprueban no solo que se produzcan tokens, sino también que los valores, ubicaciones, errores y reportes sean consistentes.

## 19. Preguntas frecuentes para la presentación

### ¿La aplicación ejecuta JavaScript?

No. Analiza el código y lo transforma en tokens, pero no lo ejecuta.

### ¿Es un compilador completo?

No. Implementa principalmente la etapa léxica y prepara componentes para la etapa sintáctica.

### ¿Por qué utilizar JFlex?

JFlex permite definir patrones mediante expresiones regulares y genera un analizador eficiente basado en un autómata finito. Esto evita programar manualmente cada transición entre caracteres.

### ¿Qué ocurre si hay un carácter inválido?

Se crea un error léxico con su ubicación y el lexer intenta continuar para encontrar más problemas.

### ¿Por qué son importantes la línea y la columna?

Permiten localizar con precisión el token o error dentro del código fuente.

### ¿Qué diferencia hay entre un error léxico y uno sintáctico?

Un error léxico ocurre cuando un carácter o secuencia no puede reconocerse, como `@` en una posición inválida. Un error sintáctico ocurre cuando tokens válidos están organizados de una forma que no cumple la gramática, por ejemplo una expresión incompleta.

### ¿La tabla de símbolos valida si una variable fue declarada?

No. Actualmente registra identificadores y sus ocurrencias. La validación de declaraciones, ámbitos y tipos correspondería al análisis semántico.

### ¿El árbol sintáctico ya está implementado?

Existe la estructura base para representar nodos del AST y existe un puente de tokens para un parser futuro, pero todavía no se construye un árbol sintáctico completo.

### ¿Por qué `EOF` no aparece en las métricas?

`EOF` sirve para indicar el final del flujo de tokens, pero no representa una parte real del código fuente. Por eso se excluye de las métricas.

## 20. Limitaciones técnicas que conviene explicar

La aplicación implementa un subconjunto académico de JavaScript. No intenta cubrir todas las características del lenguaje oficial ni reemplazar un lexer de producción.

También es importante distinguir:

- **Tokenización**: sí está implementada.
- **Tabla de símbolos básica**: sí está implementada.
- **Diagnóstico léxico**: sí está implementado.
- **Reportes**: sí están implementados.
- **Parser completo**: todavía pendiente.
- **Análisis semántico**: todavía pendiente.
- **Ejecución de JavaScript**: fuera del alcance.

## 21. Guion recomendado para la exposición

1. Presentar el problema: transformar código fuente en unidades que un compilador pueda procesar.
2. Explicar qué es un token.
3. Mostrar el código JavaScript de ejemplo.
4. Ejecutar el análisis.
5. Mostrar la tabla de tokens.
6. Explicar línea, columna y valor literal.
7. Mostrar la tabla de símbolos.
8. Abrir el informe ejecutivo.
9. Exportar un reporte HTML o Markdown.
10. Ejecutar el ejemplo con errores.
11. Mostrar que el análisis continúa después del error.
12. Explicar la diferencia entre la fase léxica y la fase sintáctica.
13. Mencionar las tecnologías y las pruebas.
14. Cerrar con las futuras extensiones: parser, AST real y análisis semántico.

## 22. Idea principal para cerrar

> La aplicación convierte código JavaScript en información estructurada y visual: tokens, posiciones, símbolos, errores y métricas. Su objetivo es demostrar de forma práctica cómo funciona la primera etapa de un compilador y dejar preparada la comunicación con un futuro analizador sintáctico.
