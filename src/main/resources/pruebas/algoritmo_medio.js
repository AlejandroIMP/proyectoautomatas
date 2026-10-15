// ========================================================
// NIVEL 2: DIFICULTAD MEDIA (Estructuras de Control y Arreglos)
// ========================================================

// Función de ordenamiento de burbuja clásico
function ordenarArreglo(arr, longitud) {
    let i = 0;
    while (i < longitud) {
        for (let j = 0; j < longitud - i - 1; j++) {
            if (arr[j] >= arr[j + 1]) {
                let temp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = temp;
            } else {
                // Posicion relativa correcta
            }
        }
        i++;
    }
    return arr;
}

// Búsqueda lineal con condicionales y comparaciones
function buscarElemento(arr, longitud, clave) {
    let encontrado = false;
    let indice = 0;
    while (indice < longitud && !encontrado) {
        if (arr[indice] == clave) {
            encontrado = true;
            return indice;
        }
        indice++;
    }
    return -1;
}

// Cálculo del factorial mediante bucle for
function calcularFactorial(numero) {
    if (numero <= 1) {
        return 1;
    }
    let resultado = 1;
    for (let k = 2; k <= numero; k++) {
        resultado = resultado * k;
    }
    return resultado;
}
