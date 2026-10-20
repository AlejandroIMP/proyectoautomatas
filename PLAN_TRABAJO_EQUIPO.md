# Plan Maestro de Trabajo: Analizador Sintáctico con JCUP
**Proyecto:** Analizador Léxico y Sintáctico para JavaScript / Pseudocódigo  
**Tecnologías:** Java 21, JFlex 1.9.1, Java CUP 11b, JUnit 5, Swing GUI  
**Equipo de Desarrollo (4 integrantes):** Alejandro (Líder / Arquitectura), Helen, Pablo y Mario.

---

## 1. Resumen Ejecutivo del Proyecto

El proyecto ha sido transformado exitosamente de un analizador únicamente léxico a un **analizador sintáctico completo basado en un generador LR (LALR(1)) con Java CUP**, integrado de forma armónica con **JFlex** y una interfaz gráfica Swing con renderizado en tiempo real del **Árbol de Sintaxis Abstracta (AST)**.

### Características Principales Implementadas:
1. **Declaración y Asignación de Variables:** Soporte para `let`, `const` y `var` con o sin asignación inicial, así como reasignaciones posteriores (`x = exp;`).
2. **Estructura de Control Condicional:** Sentencias `if` e `if-else` con bloques delimitados por llaves `{ ... }` o sentencias individuales, evaluando expresiones lógicas y relacionales complejas.
3. **Instrucciones de Impresión a Pantalla:** Soporte dual para `console.log(...)` (estándar JavaScript) y `print(...)` (sintaxis de pseudocódigo o función built-in), con listas dinámicas de argumentos.
4. **Expresiones Aritméticas, Lógicas y Relacionales:** Precedencia y asociatividad matemáticamente configuradas en CUP para eliminar ambigüedades clásicas y conflictos *shift/reduce*.
5. **Recuperación y Reporte de Errores Sintácticos:** Detección de tokens no esperados con línea y columna exacta sin abortar abruptamente el análisis.
6. **Visualización en Árbol (AST en Swing y Mermaid):** Pestaña interactiva con `JTree` que muestra la jerarquía sintáctica generada y exportación en formato diagrama de flujo Mermaid.

---

## 2. Distribución de Roles y Responsabilidades (Grupo de 4)

Para garantizar un trabajo equitativo y sin choques de código en Git, el proyecto se organizó en 4 áreas modulares:

| Integrante | Rol / Módulo Asignado | Componentes y Archivos Clave |
| :--- | :--- | :--- |
| **Alejandro** *(Tú)* | **Arquitectura, Pipeline, GUI & Diagnóstico Avanzado** | - Configuración Maven (`pom.xml`, plugins `cup-maven-plugin` y `jflex`).<br>- Coordinador del pipeline [`CompiladorJS.java`](file:///C:/Users/aleja/IdeaProjects/Programacion%20III/AnalizadorLexico/src/main/java/com/analizador/servicio/CompiladorJS.java).<br>- Puente léxico-sintáctico en [`Lexer.flex`](file:///C:/Users/aleja/IdeaProjects/Programacion%20III/AnalizadorLexico/src/main/jflex/com/analizador/lexico/Lexer.flex).<br>- Editor con números de línea ([`NumeroLinea.java`](file:///C:/Users/aleja/IdeaProjects/Programacion%20III/AnalizadorLexico/src/main/java/com/analizador/gui/NumeroLinea.java)), barra de estado y resaltado interactivo de errores.<br>- Generador de reportes con métricas sintácticas y diagramas Mermaid ([`GeneradorReporte.java`](file:///C:/Users/aleja/IdeaProjects/Programacion%20III/AnalizadorLexico/src/main/java/com/analizador/reporte/GeneradorReporte.java)).<br>- Métodos de exportación de AST a Mermaid y Visitor en [`NodoAST.java`](file:///C:/Users/aleja/IdeaProjects/Programacion%20III/AnalizadorLexico/src/main/java/com/analizador/sintactico/NodoAST.java). |
| **Helen** | **Módulo 1: Declaración de Variables y Tipos** | - Reglas en `parser.cup` para `LET`, `CONST`, `VAR` con y sin inicialización.<br>- Reglas de reasignación (`ID ASIGNACION expresion`).<br>- Modelo AST: `NodoAST.DeclaracionVariable` y `NodoAST.Asignacion`.<br>- Validación de pruebas unitarias en `AnalizadorSintacticoTest.testDeclaracionVariables`. |
| **Pablo** | **Módulo 2: Control de Flujo (IF / ELSE y Condiciones)** | - Reglas en `parser.cup` para `sentencia_if` (IF simple, IF-ELSE y anidamiento).<br>- Manejo de bloques `{ ... }` con `NodoAST.Bloque`.<br>- Resolución de la ambigüedad *dangling else* con `precedence nonassoc ELSE`.<br>- Pruebas unitarias en `AnalizadorSintacticoTest.testInstruccionIf`. |
| **Mario** | **Módulo 3: Impresión a Pantalla y Expresiones** | - Reglas en `parser.cup` para `console.log(...)` y `print(...)` con listas dinámicas de argumentos (`lista_argumentos`).<br>- Nodos AST: `NodoAST.SentenciaImprimir`, `ExpresionBinaria`, `ExpresionUnaria`, `Literal`, `Identificador`.<br>- Archivo de prueba integral `algoritmo_sintactico_completo.js`.<br>- Pruebas en `AnalizadorSintacticoTest.testInstruccionImprimir`. |

---

## 3. Mejoras Implementadas para el Módulo de Alejandro

1. **Pipeline Centralizado (`CompiladorJS`):** Desacopla la lógica de compilación de la GUI. Retorna un registro completo con tokens, tabla de símbolos, errores léxicos, errores sintácticos, advertencias semánticas y tiempo de compilación en milisegundos.
2. **Análisis Estático de Advertencias:** Detección de variables declaradas sin valor inicial y variables que solo aparecen una vez (posibles variables declaradas en desuso).
3. **Editor de Código con Números de Línea (`NumeroLinea`):** Componente en el panel izquierdo del editor que numera automáticamente cada línea y se redimensiona dinámicamente según la cantidad de dígitos.
4. **Navegación y Resaltado Visual de Errores:** Al hacer doble clic en un error (léxico o sintáctico), el cursor viaja a la posición exacta y resalta la línea con un suave color rojo translúcido.
5. **Barra de Estado en Tiempo Real:** Muestra la línea y columna activa del cursor, total de caracteres, total de tokens, tiempo de compilación y estado general.
6. **Exportación de Diagramas Mermaid:** El AST incluye el método `toMermaid()` que genera diagramas de flujo compatibles con Mermaid.js, incrustados en los reportes HTML y Markdown exportables.
7. **Control del Árbol Sintáctico en la GUI:** Botones para **"Expandir Todo"** y **"Colapsar Todo"** en la pestaña del AST.

---

## 4. Guía de Ejecución y Pruebas

### Compilar y Ejecutar Pruebas Automatizadas
```powershell
# Compilar todo el proyecto (genera fuentes de CUP y JFlex automáticamente)
mvn compile

# Ejecutar las 24 pruebas unitarias (léxicas, sintácticas, pipeline y reportes)
mvn test
```

### Ejecutar la Aplicación Gráfica
```powershell
mvn exec:java -Dexec.mainClass="com.analizador.MainApp"
```
O ejecutar directamente la clase principal `com.analizador.MainApp` en **IntelliJ IDEA**.
