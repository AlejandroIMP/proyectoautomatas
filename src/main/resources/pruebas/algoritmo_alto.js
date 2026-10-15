/*
 * ========================================================
 * NIVEL 3: DIFICULTAD DIFÍCIL / ALTA
 * Búsqueda Binaria, Objetos, Igualdad Estricta y Notación Científica
 * ========================================================
 */

function busquedaBinaria($nodos_lista, _objetivo) {
    let limite_inf = 0;
    let limite_sup = $nodos_lista.length - 1;
    const tolerancia = 1.5e-4;
    let mensaje = "Iniciando busqueda de: \"Elemento Requerido\"";

    while (limite_inf <= limite_sup && $nodos_lista !== null) {
        let medio = (limite_inf + limite_sup) / 2;
        let valorActual = $nodos_lista[medio];

        // Comparaciones estrictas y compuestas
        if (valorActual === _objetivo || (valorActual - _objetivo) < tolerancia) {
            return {
                encontrado: true,
                indice: medio,
                log: mensaje,
                tiempo_estimado: 2.75e-2
            };
        }

        if (valorActual < _objetivo && !false) {
            limite_inf = medio + 1;
        } else {
            limite_sup = medio - 1;
        }
    }

    return {
        encontrado: false,
        indice: -1,
        log: 'No se encontro el objetivo en la lista',
        codigo_estado: null
    };
}
