ARQUITECTURA BASE - JAVA 21
===========================

ARCHIVOS
--------
TokenType.java          Categorias de tokens.
Token.java              Tipo, valor, linea y columna de un token.
BufferEntrada.java      Lectura UTF-8 y control de coordenadas.
TablaSimbolos.java      Registro de identificadores.
PruebaArquitectura.java Prueba de consola, sin interfaz.
Entrada.js              Archivo fuente de ejemplo.

REQUISITO
---------
Tener instalado el JDK 21 y que java y javac funcionen desde CMD o PowerShell.

COMPILAR
--------
Dentro de esta carpeta, ejecuta:

javac --release 21 TokenType.java Token.java BufferEntrada.java TablaSimbolos.java PruebaArquitectura.java

EJECUTAR
--------
java PruebaArquitectura

Para probar otro archivo:
java PruebaArquitectura "C:\ruta\a\archivo.js"

INTEGRACION CON EL RESTO DEL PROYECTO
-------------------------------------
Esta carpeta es la arquitectura base, no un analizador lexico completo.
El analizador lexico del equipo debe recorrer BufferEntrada, guardar la linea
y columna ANTES de consumir el primer caracter de cada token y crear Token
con el TokenType correspondiente.

Coordinen nombres de clases, paquetes y metodos con los demas integrantes.
Si el proyecto usa un package, agreguen la misma declaracion a los archivos.

TablaSimbolos solo registra tokens IDENTIFIER y conserva la primera aparicion.
No maneja ambitos, tipos de variables ni declaraciones repetidas.
