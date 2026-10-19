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
6. **Visualización en Árbol (AST en Swing):** Pestaña interactiva con `JTree` que muestra la jerarquía sintáctica generada.

---

## 2. Distribución de Roles y Responsabilidades (Grupo de 4)

Para garantizar un trabajo equitativo y sin choques de código en Git, el proyecto se organizó en 4 áreas modulares:

| Integrante | Rol / Módulo Asignado | Componentes y Archivos Clave |
| :--- | :--- | :--- |
| **Alejandro** *(Tú)* | **Arquitectura, Integración CUP/JFlex y GUI** | - Configuración Maven (`pom.xml`, plugins `cup-maven-plugin` y `jflex`).<br>- Directiva `%cup` y generación de símbolos en `Lexer.flex`.<br>- Coordinación de la gramática base en `parser.cup`.<br>- Integración visual en `VentanaPrincipal.java` (`JTree` del AST y consola de errores sintácticos). |
| **Helen** | **Módulo 1: Declaración de Variables y Tipos** | - Reglas en `parser.cup` para `LET`, `CONST`, `VAR` con y sin inicialización.<br>- Reglas de reasignación (`ID ASIGNACION expresion`).<br>- Modelo AST: `NodoAST.DeclaracionVariable` y `NodoAST.Asignacion`.<br>- Validación de pruebas unitarias en `AnalizadorSintacticoTest.testDeclaracionVariables`. |
| **Pablo** | **Módulo 2: Control de Flujo (IF / ELSE y Condiciones)** | - Reglas en `parser.cup` para `sentencia_if` (IF simple, IF-ELSE y anidamiento).<br>- Manejo de bloques `{ ... }` con `NodoAST.Bloque`.<br>- Resolución de la ambigüedad *dangling else* con `precedence nonassoc ELSE`.<br>- Pruebas unitarias en `AnalizadorSintacticoTest.testInstruccionIf`. |
| **Mario** | **Módulo 3: Impresión a Pantalla y Expresiones** | - Reglas en `parser.cup` para `console.log(...)` y `print(...)` con listas dinámicas de argumentos (`lista_argumentos`).<br>- Nodos AST: `NodoAST.SentenciaImprimir`, `ExpresionBinaria`, `ExpresionUnaria`, `Literal`, `Identificador`.<br>- Archivo de prueba integral `algoritmo_sintactico_completo.js`.<br>- Pruebas en `AnalizadorSintacticoTest.testInstruccionImprimir`. |

---

## 3. Estructura de la Gramática CUP (`src/main/cup/parser.cup`)

La gramática está estructurada para que cada miembro identifique sus producciones con exactitud:

```cup
/* ========================================================
   MÓDULO 1 (HELEN): DECLARACIÓN Y ASIGNACIÓN DE VARIABLES
   ======================================================== */
declaracion_variable ::= LET ID:id PUNTO_COMA
                         {: RESULT = new NodoAST.DeclaracionVariable("let", id, null); :}
                       | LET ID:id ASIGNACION expresion:e PUNTO_COMA
                         {: RESULT = new NodoAST.DeclaracionVariable("let", id, e); :}
                       | CONST ID:id ASIGNACION expresion:e PUNTO_COMA
                         {: RESULT = new NodoAST.DeclaracionVariable("const", id, e); :}
                       | VAR ID:id PUNTO_COMA
                         {: RESULT = new NodoAST.DeclaracionVariable("var", id, null); :}
                       | VAR ID:id ASIGNACION expresion:e PUNTO_COMA
                         {: RESULT = new NodoAST.DeclaracionVariable("var", id, e); :}
                       ;

asignacion ::= ID:id ASIGNACION expresion:e PUNTO_COMA
               {: RESULT = new NodoAST.Asignacion(id, e); :}
             ;

/* ========================================================
   MÓDULO 2 (PABLO): ESTRUCTURA CONDICIONAL IF / ELSE
   ======================================================== */
sentencia_if ::= IF PAREN_IZQ expresion:cond PAREN_DER sentencia:bt
                 {: RESULT = new NodoAST.SentenciaIf(cond, bt, null); :}
               | IF PAREN_IZQ expresion:cond PAREN_DER sentencia:bt ELSE sentencia:be
                 {: RESULT = new NodoAST.SentenciaIf(cond, bt, be); :}
               ;

bloque ::= LLAVE_IZQ lista_sentencias:ls LLAVE_DER
           {: RESULT = new NodoAST.Bloque(ls); :}
         | LLAVE_IZQ LLAVE_DER
           {: RESULT = new NodoAST.Bloque(new ArrayList<>()); :}
         ;

/* ========================================================
   MÓDULO 3 (MARIO): IMPRESIÓN A PANTALLA
   ======================================================== */
sentencia_imprimir ::= CONSOLE PUNTO LOG PAREN_IZQ lista_argumentos:args PAREN_DER PUNTO_COMA
                       {: RESULT = new NodoAST.SentenciaImprimir("console.log", args); :}
                     | CONSOLE PUNTO LOG PAREN_IZQ PAREN_DER PUNTO_COMA
                       {: RESULT = new NodoAST.SentenciaImprimir("console.log", new ArrayList<>()); :}
                     | PRINT PAREN_IZQ lista_argumentos:args PAREN_DER PUNTO_COMA
                       {: RESULT = new NodoAST.SentenciaImprimir("print", args); :}
                     | PRINT PAREN_IZQ PAREN_DER PUNTO_COMA
                       {: RESULT = new NodoAST.SentenciaImprimir("print", new ArrayList<>()); :}
                     ;

lista_argumentos ::= lista_argumentos:ls COMA expresion:e
                     {: ls.add(e); RESULT = ls; :}
                   | expresion:e
                     {:
                         List<NodoAST> ls = new ArrayList<>();
                         ls.add(e);
                         RESULT = ls;
                     :}
                   ;
```

---

## 4. Guía de Ejecución y Pruebas

### Compilar y Ejecutar Pruebas
```powershell
# Compilar todo el proyecto (genera fuentes de CUP y JFlex automáticamente)
mvn compile

# Ejecutar las 19 pruebas unitarias (léxicas y sintácticas)
mvn test
```

### Ejecutar la Aplicación Gráfica
```powershell
mvn exec:java -Dexec.mainClass="com.analizador.MainApp"
```
O simplemente ejecutar la clase principal `com.analizador.MainApp` directamente desde **IntelliJ IDEA**.
