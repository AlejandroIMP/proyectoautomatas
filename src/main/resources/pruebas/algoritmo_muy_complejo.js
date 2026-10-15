/*
 * ========================================================================
 * NIVEL 4: DIFICULTAD MUY COMPLEJA (Estrés Sintáctico y Léxico)
 * Algoritmo de Dijkstra para Caminos Mínimos en Grafos Ponderados
 * Multiplicación de Matrices y Análisis de Rendimiento
 * ========================================================================
 */

// Configuración global del sistema de grafos
const MAX_NODOS = 100;
const INFINITO_SIMB = 1.0e+9;
const EPSILON = 1.0e-7;

/* Función constructora de nodos de grafo */
function crearNodoGrafo($id_nodo, _etiqueta, peso_inicial) {
    let _activo = true;
    let metadatos = {
        id: $id_nodo,
        etiqueta: _etiqueta,
        peso: peso_inicial,
        activo: _activo,
        referencia_nula: null,
        descripcion: 'Nodo principal con identificador: \"' + _etiqueta + '\"'
    };
    return metadatos;
}

// Algoritmo principal de camino más corto
function dijkstra($matriz_adyacencia, num_vertices, origen) {
    let distancia = [];
    let visitado = [];
    let predecesor = [];

    // Inicialización de vectores de estado
    for (let i = 0; i < num_vertices; i++) {
        distancia[i] = INFINITO_SIMB;
        visitado[i] = false;
        predecesor[i] = null;
    }

    distancia[origen] = 0.0;

    for (let cuenta = 0; cuenta < num_vertices - 1; cuenta++) {
        // Encontrar vértice con distancia mínima no procesado
        let minimo = INFINITO_SIMB;
        let u = -1;

        for (let v = 0; v < num_vertices; v++) {
            if (!visitado[v] && distancia[v] <= minimo) {
                minimo = distancia[v];
                u = v;
            }
        }

        if (u === -1 || minimo === INFINITO_SIMB) {
            // No existen más componentes conexas accesibles
            break;
        }

        visitado[u] = true;

        // Actualización de vecinos
        for (let v = 0; v < num_vertices; v++) {
            let costoArista = $matriz_adyacencia[u][v];
            if (!visitado[v] && costoArista !== 0 && distancia[u] !== INFINITO_SIMB) {
                let nuevaDistancia = distancia[u] + costoArista;
                if (nuevaDistancia < distancia[v]) {
                    distancia[v] = nuevaDistancia;
                    predecesor[v] = u;
                }
            }
        }
    }

    return {
        origen: origen,
        distancias: distancia,
        predecesores: predecesor,
        completado: true,
        factor_convergencia: 9.80665e-1
    };
}

// Multiplicación matricial con validación dimensional
function multiplicarMatrices(matA, filasA, colsA, matB, filasB, colsB) {
    if (colsA !== filasB) {
        return null;
    }

    let resultado = [];
    for (let r = 0; r < filasA; r++) {
        resultado[r] = [];
        for (let c = 0; c < colsB; c++) {
            let suma = 0.0;
            for (let k = 0; k < colsA; k++) {
                suma = suma + (matA[r][k] * matB[k][c]);
            }
            resultado[r][c] = suma;
        }
    }
    return resultado;
}
