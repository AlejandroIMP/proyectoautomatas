// ============================================================
// CASO EXTENSO DE ERRORES LEXICOS
// Objetivo: probar deteccion, posiciones y recuperacion.
// Los identificadores correctos posteriores confirman que el
// analizador no se detiene despues de cada error.
// ============================================================

// 1. Simbolos aislados que no pertenecen al alfabeto definido
let usuario = "Alejandro";
usuario @ "dominio";
let total # 100;
let aproximacion = 3.14 ~ 2.71;
let potencia = 2 ^ 8;
let porcentaje = 50 % 10;
let consulta ? usuario;
let referencia = valor ` texto;
let tuberia = dato | respaldo;
let ampersand = izquierda & derecha;

// 2. Simbolos Unicode no contemplados por las reglas de identificadores
let año = 2026;
let café = "Java";
let símbolo = "invalido";
let comentario = "¿Qué ocurrió?";
let moneda = €100;
let flecha = dato → resultado;

// 3. Identificadores que empiezan con un número
let 1usuario = "incorrecto";
let 2doIntento = 2;
let 123abc = 123;
let 7_dias = 7;

// Despues de cada grupo deben seguir reconociendose tokens validos.
let recuperacionUno = true;
const recuperacionDos = 42;

// 4. Mezcla de simbolos invalidos junto a operadores validos
function validarEntrada(valor) {
    if (valor >= 0 && valor <= 100) {
        return true;
    }
    return false;
}

let expresionRota = valor @@@ 10;
let otraExpresion = (20 # 5) && (3 ^ 2);
let comparacion = valor === 10;
let diferencia = valor !== 0;

// 5. Secuencias de signos invalidos: los operadores validos de alrededor
// deben seguir apareciendo como tokens independientes.
let signosUno = 10 ++@ 2;
let signosDos = 10 --# 2;
let signosTres = !~activo;
let signosCuatro = a &&& b;
let signosCinco = a ||| b;
let signosSeis = a ===? b;

// 6. Cadenas con escapes validos y caracteres invalidos fuera de ellas
let cadenaValida = "texto con @ # ~ ^ ?";
let cadenaConEscape = "comilla escapada: \" correcta";
let cadenaSimple = 'cadena simple';
let textoInvalido = "antes" @ "despues";

// 7. Template literal valido: su contenido no se analiza como tokens
const plantillaValida = `Usuario: ${usuario} @ # ~`;
const plantillaMultilinea = `Linea uno
Linea dos
Linea tres`;

// 8. Comentarios validos: no deben producir errores
// Este comentario contiene @ # ~ ^ ? y sigue siendo valido.
/*
   Este comentario contiene simbolos: @ # ~ ^ ? € →.
   Tampoco debe producir errores porque esta cerrado.
*/
let despuesDeComentario = "continua";

// 9. Tokens validos al final para comprobar la recuperacion global
var resultadoFinal = 1000;
resultadoFinal++;
resultadoFinal--;
if (resultadoFinal >= 0) {
    resultadoFinal = resultadoFinal + 1;
}

// 10. Este es el ultimo escenario.
// Desde aqui el lexer entra en SIN_CIERRE y descarta el resto del archivo.
/*
