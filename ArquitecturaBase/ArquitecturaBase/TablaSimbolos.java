import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Tabla base que registra identificadores unicos en orden de aparicion. */
public final class TablaSimbolos {
    private final Map<String, Token> simbolos = new LinkedHashMap<>();

    /** Agrega solo tokens IDENTIFIER y conserva la primera aparicion. */
    public void agregar(Token token) {
        if (token == null) throw new IllegalArgumentException("El token no puede ser null.");
        if (token.getTipo() == TokenType.IDENTIFIER) {
            simbolos.putIfAbsent(token.getValor(), token);
        }
    }

    public boolean existe(String nombre) { return simbolos.containsKey(nombre); }
    /** Devuelve null si no existe. */
    public Token buscar(String nombre) { return simbolos.get(nombre); }
    public Optional<Token> buscarOpcional(String nombre) {
        return Optional.ofNullable(simbolos.get(nombre));
    }
    public int cantidad() { return simbolos.size(); }
    public boolean estaVacia() { return simbolos.isEmpty(); }
    public Map<String, Token> obtenerTodos() {
        return Collections.unmodifiableMap(simbolos);
    }
    public void limpiar() { simbolos.clear(); }

    public void mostrar() {
        System.out.println("=== TABLA DE SIMBOLOS ===");
        if (simbolos.isEmpty()) {
            System.out.println("(sin identificadores)");
            return;
        }
        for (Map.Entry<String, Token> entrada : simbolos.entrySet()) {
            Token token = entrada.getValue();
            System.out.printf("%s | primera aparicion: linea %d, columna %d%n",
                entrada.getKey(), token.getLinea(), token.getColumna());
        }
    }
}
