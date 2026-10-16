import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lee un archivo UTF-8 y permite recorrerlo caracter por caracter.
 * Las coordenadas comienzan en linea 1, columna 1.
 * Normaliza finales de linea LF, CRLF y CR.
 */
public final class BufferEntrada {
    private final String contenido;
    private int posicion;
    private int linea;
    private int columna;

    public BufferEntrada(String ruta) throws IOException {
        this(Path.of(ruta));
    }

    public BufferEntrada(Path ruta) throws IOException {
        String texto = Files.readString(ruta, StandardCharsets.UTF_8);
        this.contenido = texto.replace("\r\n", "\n").replace('\r', '\n');
        this.posicion = 0;
        this.linea = 1;
        this.columna = 1;
    }

    /** Caracter actual sin consumirlo; '\0' representa fin de archivo. */
    public char actual() {
        return fin() ? '\0' : contenido.charAt(posicion);
    }

    /** Siguiente caracter sin consumirlo; '\0' si no existe. */
    public char mirarAdelante() {
        int siguiente = posicion + 1;
        return siguiente >= contenido.length() ? '\0' : contenido.charAt(siguiente);
    }

    /** Consume el caracter actual y actualiza las coordenadas. */
    public char avanzar() {
        if (fin()) return '\0';
        char caracter = contenido.charAt(posicion++);
        if (caracter == '\n') {
            linea++;
            columna = 1;
        } else {
            columna++;
        }
        return caracter;
    }

    public boolean fin() { return posicion >= contenido.length(); }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
    public int getPosicion() { return posicion; }
    public int getLongitud() { return contenido.length(); }
    public String getContenido() { return contenido; }
}
