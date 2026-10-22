// ========================================================
// MÓDULO 2 (PABLO): CONTROL DE FLUJO (IF / ELSE / ELSE-IF)
// ========================================================

let calificacion = 85;
let nivel = "indefinido";
let esAprobado = false;

// 1. Estructura IF - ELSE IF - ELSE encadenada
if (calificacion >= 90) {
    nivel = "Excelente (A)";
    esAprobado = true;
    console.log("Felicidades por obtener nivel A");
} else if (calificacion >= 80) {
    nivel = "Muy Bueno (B)";
    esAprobado = true;
    print("Nivel B alcanzado");
} else if (calificacion >= 70) {
    nivel = "Satisfactorio (C)";
    esAprobado = true;
} else {
    nivel = "Necesita Mejorar (F)";
    esAprobado = false;
    print("No ha alcanzado la nota minima");
}

// 2. IF simple con condiciones logicas compuestas (&&, ||, !)
let saldo = 500;
let bloqueado = false;

if (saldo > 0 && !bloqueado) {
    print("Cuenta activa y con saldo positivo");
}

// 3. Sentencias IF anidadas con bloques y ramas alternas
let edad = 20;
let tieneLicencia = true;

if (edad >= 18) {
    if (tieneLicencia) {
        console.log("Apto para conducir vehiculo");
    } else {
        print("Mayor de edad pero requiere tramitar licencia");
    }
}

// 4. Sentencias simples sin llaves (dangling else resuelto correctamente)
if (esAprobado)
    print("Resultado final: APROBADO");
else
    print("Resultado final: REPROBADO");
