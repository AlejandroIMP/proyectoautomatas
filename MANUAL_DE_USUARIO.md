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
* **Informes y reportes ejecutivos contextualizados:** Resumen estadístico, distribución de tokens, porcentajes y diagnóstico del código analizado.
* **Exportación de reportes:** Generación de reportes en formato **HTML moderno** (con diseño responsivo y estilos CSS) y en formato **Markdown (.md)**.
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
+-------------------------------------------------------------------------------------------------+
| [ Cargar .js ] | [ Selector de Dificultad v ] | [ Ejecutar Análisis Léxico ] | [ Exportar Reporte ]
+-------------------------------------------------------------------------------------------------+
|                                        |  [Tokens] [Símbolos] [Informe Ejecutivo] [Árbol]      |
|                                        |--------------------------------------------------------|
|                                        |  # | Token | Lexema | Línea | Col | Valor Literal      |
|          EDITOR DE CÓDIGO              |--------------------------------------------------------|
|                                        |  1 | CONST | const  |   5   |  1  |  -                 |
|         (Área de Texto JS)             |  2 | ID    | PI     |   5   |  7  |  -                 |
|                                        |  3 | ASIG  |   =    |   5   | 10  |  -                 |
|                                        |  4 | FLOT  | 3.1415 |   5   | 12  | 3.1415             |
+----------------------------------------+--------------------------------------------------------+
|  CONSOLA DE ERRORES LÉXICOS                                                                     |
|  Lexema Inválido | Línea | Columna | Descripción                                                |
+-------------------------------------------------------------------------------------------------+
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
4. **Botón "Exportar Reporte":** Permite generar y guardar un reporte contextualizado completo en formato **HTML** o **Markdown (.md)**, y ofrece abrirlo directamente en el navegador web predeterminado.

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
* **Pestaña 3 — "Informe Ejecutivo":** Muestra un resumen visual inmediato con métricas clave:
  * Total de líneas, caracteres y tokens procesados.
  * Desglose por categorías (palabras clave, operadores, literales, delimitadores).
  * Diagnóstico contextual para la fase sintáctica.
* **Pestaña 4 — "Árbol Sintáctico":** Estructura jerárquica visual (`JTree`) preparada para conectar el analizador sintáctico en la siguiente fase de desarrollo.

### 4.4. Consola de Errores Léxicos (Panel Inferior)
Si el código contiene caracteres ajenos al alfabeto del lenguaje (como `@`, `#`, tildes invertidas, símbolos no reconocidos), esta tabla muestra:
* `Lexema Inválido`: El carácter o símbolo ilegal.
* `Línea` y `Columna`: Coordenadas exactas del error.
* `Descripción`: Detalle del fallo léxico.

---

## 5. Guía de Uso Paso a Paso

### Caso 1: Generar y Exportar un Reporte
1. Inicie la aplicación o seleccione el algoritmo deseado (por ejemplo, **"Dificultad Muy Compleja"**).
2. Revise la pestaña **"Informe Ejecutivo"** para ver las métricas preliminares.
3. Haga clic en el botón superior **"Exportar Reporte"**.
4. Seleccione el formato deseado:
   * **Reporte HTML (Completo con Estilos):** Genera una página web moderna con tarjetas de resumen, tablas interactivas, código resaltado y estilos CSS independientes.
   * **Reporte Markdown (.md):** Genera un documento en texto estructurado ideal para documentación técnica, repositorios Git o informes de laboratorio.
5. Elija la ubicación donde desea guardar el archivo.
6. El sistema le preguntará si desea abrir el reporte inmediatamente en su navegador web. Haga clic en **Sí** para visualizarlo.

### Caso 2: Probar la detección de errores léxicos y su reporte
1. En el menú desplegable, seleccione **"Prueba Errores Léxicos"**.
2. Observe la tabla inferior **Consola de Errores Léxicos**:
   * Se registrarán los símbolos `@`, `#`, `~`, `^`, `?`.
3. Vaya a la pestaña **"Informe Ejecutivo"**:
   * El estado del análisis cambiará automáticamente a <font color="red">**CON ERRORES LÉXICOS**</font> y detallará el impacto de dichos símbolos.
4. Al exportar el reporte en HTML, se incluirá una sección especial de **Incidentes Léxicos Detectados** con advertencias técnicas para el usuario.

---

## 6. Preguntas Frecuentes y Solución de Problemas

* **¿Por qué un error como `"le t"` no sale en la consola de errores léxicos?**
  * Porque `"le"` y `"t"` son caracteres alfabéticos válidos, por lo que son reconocidos como identificadores (variables) independientes. La prohibición de tener dos identificadores consecutivos es una regla sintáctica, no léxica.
* **¿Qué archivos de reporte se pueden compartir?**
  * El archivo `.html` exportado es completamente autónomo (no depende de conexión a internet ni librerías externas) y puede enviarse por correo, abrirse en Chrome, Edge o Firefox, o guardarse como PDF desde la opción de imprimir del navegador.
