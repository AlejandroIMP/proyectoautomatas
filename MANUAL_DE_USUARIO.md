# Manual de Usuario
## Analizador Léxico para JavaScript (Java 21 & JFlex)
### Asignatura: Programación III

---

## 1. Introducción
El **Analizador Léxico para JavaScript** es una herramienta académica y profesional diseñada para descomponer código fuente en JavaScript en sus componentes léxicos fundamentales (*tokens*), registrar las declaraciones de identificadores en una tabla de símbolos y capturar cualquier carácter o secuencia que no pertenezca al alfabeto del lenguaje sin interrumpir el procesamiento del archivo.

El sistema cuenta con una interfaz gráfica interactiva construida en Java Swing que permite visualizar en tiempo real:
* La secuencia cronológica de tokens reconocidos con sus coordenadas de línea y columna.
* La tabla de símbolos con conteo de ocurrencias de variables y funciones.
* La consola de errores léxicos detectados.
* La estructura base para la futura fase de análisis sintáctico.

---

## 2. Requisitos del Sistema

* **Sistema Operativo:** Windows 10/11, Linux o macOS.
* **Java Runtime:** Java Development Kit (JDK) o Java Runtime Environment (JRE) versión **21 (LTS)** o superior.
* **Resolución de Pantalla:** Mínimo $1024 \times 700$ píxeles (recomendado $1280 \times 800$ o superior).
* **Opcional (para desarrollo/compilación):** Apache Maven 3.9+ e IntelliJ IDEA / Eclipse IDE.

---

## 3. Instalación y Ejecución

### Opción A: Ejecución Directa mediante Archivo JAR Ejecutable
Si ya se generó el empaquetado del proyecto, no es necesario abrir ningún entorno de desarrollo. Desde la terminal o consola de comandos, ubíquese en la raíz del proyecto y ejecute:

```bash
java -jar target/analizador-js-1.0.0.jar
```
*(En entornos de escritorio también puede iniciarse haciendo doble clic sobre el archivo `analizador-js-1.0.0.jar`).*

### Opción B: Ejecución desde Maven
```bash
mvn clean compile exec:java -Dexec.mainClass="com.analizador.MainApp"
```

### Opción C: Ejecución desde IntelliJ IDEA o Eclipse
1. Abra el proyecto en el IDE.
2. Localice la clase `com.analizador.MainApp` en la ruta:
   `src/main/java/com/analizador/MainApp.java`
3. Haga clic derecho sobre el archivo y seleccione **Run 'MainApp.main()'**.

---

## 4. Descripción de la Interfaz Gráfica (GUI)

La ventana principal está organizada en cuatro zonas principales:

```
+-----------------------------------------------------------------------------------+
| [ Cargar .js ]   | [ Selector de Dificultad v ] |   [ Ejecutar Análisis Léxico ]  |
+-----------------------------------------------------------------------------------+
|                                        |  [Flujo de Tokens] [Símbolos] [Árbol]   |
|                                        |------------------------------------------|
|                                        |  # | Token | Lexema | Línea | Col | Val  |
|          EDITOR DE CÓDIGO              |------------------------------------------|
|                                        |  1 | CONST | const  |   5   |  1  |  -   |
|         (Área de Texto JS)             |  2 | ID    | PI     |   5   |  7  |  -   |
|                                        |  3 | ASIG  |   =    |   5   | 10  |  -   |
|                                        |  4 | FLOT  | 3.1415 |   5   | 12  | 3.14 |
+----------------------------------------+------------------------------------------+
|  CONSOLA DE ERRORES LÉXICOS                                                       |
|  Lexema Inválido | Línea | Columna | Descripción                                  |
+-----------------------------------------------------------------------------------+
```

### 4.1. Barra Superior de Herramientas (ToolBar)
1. **Botón "Cargar .js":** Abre un selector de archivos (`JFileChooser`) preconfigurado en la carpeta de pruebas del proyecto (`src/main/resources/pruebas/`), permitiendo abrir cualquier script `.js` externo.
2. **Selector Rápido de Dificultad (Desplegable):** Permite cambiar al instante entre algoritmos preestablecidos de prueba:
   * `Dificultad Baja`: Operaciones aritméticas simples y declaraciones básicas.
   * `Dificultad Media`: Estructuras de control (`if`, `while`, `for`), funciones y arreglos.
   * `Dificultad Alta`: Búsqueda binaria, objetos, notación científica y cadenas complejas.
   * `Dificultad Muy Compleja`: Algoritmo de Dijkstra para grafos, matrices bidimensionales y estrés de tokens.
   * `Prueba Errores Léxicos`: Demostración de captura y recuperación de caracteres prohibidos (`@`, `#`, `~`, `^`, etc.).
   *(Al elegir cualquier opción, el código se carga en el editor y se analiza automáticamente).*
3. **Botón "Ejecutar Análisis Léxico":** Reanaliza el código fuente actual del editor en caso de haber realizado modificaciones manuales.

### 4.2. Editor de Código Fuente (Panel Izquierdo)
* Área de texto con tipografía monoespaciada de alta legibilidad (`Font.MONOSPACED`).
* Permite escribir, pegar o modificar código JavaScript de forma libre.

### 4.3. Panel de Resultados (Pestañas - Panel Derecho)
* **Pestaña 1 — "Flujo de Tokens":** Presenta una tabla con cada unidad lógica identificada:
  * `#`: Número consecutivo de token.
  * `Token`: Categoría gramatical (`LET`, `CONST`, `IF`, `ID`, `LIT_ENTERO`, `MAS`, etc.).
  * `Lexema`: Cadena exacta de texto encontrada en el código fuente.
  * `Línea` y `Columna`: Coordenadas de inicio del token en el archivo.
  * `Valor Literal`: Valor evaluado y convertido (por ejemplo, valor numérico `Double` o `Long`, valor booleano o valor de cadena limpia).
* **Pestaña 2 — "Tabla de Símbolos":** Presenta el inventario ordenado de identificadores del programa:
  * `Identificador`: Nombre de la variable o función.
  * `Categoría`: Clasificación (`IDENTIFICADOR`).
  * `1ra Línea` y `1ra Columna`: Ubicación donde fue declarado o usado por primera vez.
  * `Ocurrencias`: Contador de veces que se repite el identificador en todo el código.
* **Pestaña 3 — "Árbol Sintáctico":** Estructura jerárquica visual (`JTree`) preparada para conectar el analizador sintáctico en la siguiente fase de desarrollo.

### 4.4. Consola de Errores Léxicos (Panel Inferior)
Si el código contiene caracteres ajenos al alfabeto del lenguaje (como `@`, `#`, tildes invertidas, símbolos no reconocidos), esta tabla muestra:
* `Lexema Inválido`: El carácter o símbolo ilegal.
* `Línea` y `Columna`: Coordenadas exactas del error.
* `Descripción`: Detalle del fallo léxico.

---

## 5. Guía de Uso Paso a Paso

### Caso 1: Probar un algoritmo preestablecido
1. Inicie la aplicación. Por defecto se cargará el algoritmo de **Dificultad Baja**.
2. Despliegue el menú **Selector de Dificultad** y seleccione **"Dificultad Media"**.
3. Observe cómo el editor se actualiza con un algoritmo de ordenamiento burbuja y cálculo de factoriales.
4. La tabla **Flujo de Tokens** mostrará inmediatamente tokens como `FUNCTION`, `WHILE`, `FOR`, `RETURN`, `INCREMENTO`, etc.
5. Pase a la pestaña **Tabla de Símbolos** para verificar cuántas veces se utilizan los identificadores `arr`, `longitud`, `temp`, etc.

### Caso 2: Probar la detección de errores léxicos
1. En el menú desplegable, seleccione **"Prueba Errores Léxicos"**.
2. El editor mostrará declaraciones como:
   ```javascript
   let @usuario = "Juan Perez";
   const #MAX_INTENTOS = 5;
   let factor = 12.5 ~ 3.2;
   ```
3. Observe la tabla inferior **Consola de Errores Léxicos**:
   * Se registrarán de forma individual los símbolos `@`, `#`, `~`, `^`, `?`.
4. Observe la tabla superior **Flujo de Tokens**:
   * A pesar de los errores, el analizador no se detuvo y continuó extrayendo tokens válidos como `let`, `usuario`, `=`, `"Juan Perez"`, `;`.

### Caso 3: Escribir o editar código propio
1. Haga clic en el editor de código a la izquierda.
2. Escriba o pegue cualquier función o expresión JavaScript:
   ```javascript
   function calcularArea(ancho, alto) {
       let resultado = ancho * alto;
       return resultado;
   }
   ```
3. Haga clic en el botón superior **"Ejecutar Análisis Léxico"**.
4. Las tablas de resultados se actualizarán al instante.

---

## 6. Preguntas Frecuentes y Solución de Problemas

* **¿Por qué un carácter ilegal no detiene la ejecución del programa?**
  * El analizador implementa una estrategia de captura y recuperación léxica mediante la regla comodín `[^]` en JFlex. Esto permite listar todos los errores presentes en el archivo sin abortar el análisis.
* **¿Cómo se diferencian operadores como `=` y `===`?**
  * JFlex utiliza el principio de máxima longitud (*longest match*) y prioridad de reglas. El operador estricto `===` tiene prioridad sobre la igualdad simple `==` y la asignación `=`.
* **¿Qué sucede con los comentarios y espacios en blanco?**
  * Los espacios, tabulaciones y saltos de línea se utilizan como separadores y son descartados, al igual que los comentarios de línea (`//...`) y de bloque (`/*...*/`), sin generar tokens en la tabla pero incrementando adecuadamente los contadores de línea y columna.
