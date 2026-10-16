import java.util.Objects;

/** Representa un token y su posicion inicial en el archivo fuente. */
public final class Token {
    private final TokenType tipo;
    private final String valor;
    private final int linea;
    private final int columna;

    public Token(TokenType tipo, String valor, int linea, int columna) {
        this.tipo = Objects.requireNonNull(tipo, "El tipo no puede ser null");
        this.valor = Objects.requireNonNull(valor, "El valor no puede ser null");
        if (linea < 1 || columna < 1) {
            throw new IllegalArgumentException("La linea y la columna deben comenzar en 1.");
        }
        this.linea = linea;
        this.columna = columna;
    }

    public TokenType getTipo() { return tipo; }
    public String getValor() { return valor; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }

    @Override
    public String toString() {
        return tipo + "('" + valor + "') en linea " + linea + ", columna " + columna;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof Token otro)) return false;
        return linea == otro.linea && columna == otro.columna
            && tipo == otro.tipo && valor.equals(otro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, valor, linea, columna);
    }
}
