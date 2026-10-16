import java.io.IOException;
import java.nio.file.Path;

/** Prueba de consola independiente de la interfaz y del lexer completo. */
public class PruebaArquitectura {
    public static void main(String[] args) {
        Path archivo = args.length > 0 ? Path.of(args[0]) : Path.of("Entrada.js");
        try {
            BufferEntrada buffer = new BufferEntrada(archivo);
            System.out.println("=== PRUEBA DEL BUFFER ===");
            while (!buffer.fin()) {
                int linea = buffer.getLinea();
                int columna = buffer.getColumna();
                char caracter = buffer.avanzar();
                String visible = switch (caracter) {
                    case '\n' -> "\\n";
                    case '\t' -> "\\t";
                    default -> Character.toString(caracter);
                };
                System.out.printf("Caracter: %-4s | linea: %d | columna inicial: %d%n",
                    visible, linea, columna);
            }

            TablaSimbolos tabla = new TablaSimbolos();
            tabla.agregar(new Token(TokenType.IDENTIFIER, "edad", 1, 5));
            tabla.agregar(new Token(TokenType.IDENTIFIER, "nombre", 2, 7));
            tabla.agregar(new Token(TokenType.IDENTIFIER, "edad", 3, 8));
            tabla.agregar(new Token(TokenType.NUMBER, "21", 3, 15));
            System.out.println();
            tabla.mostrar();
            System.out.println("Total de identificadores unicos: " + tabla.cantidad());
        } catch (IOException e) {
            System.err.println("No se pudo leer el archivo: " + archivo);
            System.err.println("Detalle: " + e.getMessage());
            System.err.println("Crea Entrada.js o indica una ruta como argumento.");
        }
    }
}
