// ========================================================
// PRUEBA COMPLETA: SINTÁCTICO (Variables, IF, Impresión)
// Integrantes: Alejandro, Helen, Pablo, Mario
// ========================================================

// 1. Módulo Helen: Declaración de Variables
let base = 10;
let altura = 20;
const limite = 100;
var resultado = (base * altura) / 2;
let mensaje;

// 2. Módulo Pablo: Instrucción Condicional IF / ELSE
if (resultado > limite) {
    let sobreLimite = true;
    console.log("Alerta: El resultado excede el límite permitido:", resultado);
} else {
    let sobreLimite = false;
    print("Resultado dentro del rango seguro:", resultado);
}

// 3. Módulo Mario: Impresión a pantalla
console.log("--- Resumen del Análisis ---");
print("Base:", base);
print("Altura:", altura);
console.log("Cálculo final exitoso!");
