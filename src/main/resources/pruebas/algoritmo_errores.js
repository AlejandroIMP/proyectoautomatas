// ========================================================
// CASO DE PRUEBA: DETECCIÓN Y RECUPERACIÓN DE ERRORES LÉXICOS
// ========================================================

// Variables válidas con caracteres ilegales inyectados
let @usuario = "Juan Perez";
const #MAX_INTENTOS = 5;
let factor = 12.5 ~ 3.2;

/* Bloque con símbolos que no pertenecen al alfabeto JS estándar */
function calcularValor(base, exponente) {
    let temporal^ = base ^ exponente;
    let resultado? = temporal * 2;
    return resultado;
}

// Inyección de operadores o símbolos no soportados:
let expresion = 10 ` 20;
let simboloInvalido = ¿error?;

// Código válido posterior para verificar que el analizador no se detiene
let totalFinal = 100 + 200;
let estadoExitoso = true;
