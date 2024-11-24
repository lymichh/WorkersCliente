package lab3_hilossockets;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import static lab3_hilossockets.Client_Server.IP_ADDRESS;
import static lab3_hilossockets.Client_Server.PORT;
import lab3_hilossockets.SortState;

public class Worker {

    public static final String IP_ADDRESS = Config.SERVER_IP_ADDRESS;
    public static final int PORT = Config.SERVER_PORT;

    public static void main(String[] args) throws IOException, InterruptedException {
        Work();
    }

    public static void Work() throws IOException, InterruptedException {
    try (Socket socket = new Socket(IP_ADDRESS, PORT)) {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        while (true) {
            try {
                SortState state = (SortState) in.readObject();
                int metodo = in.readInt();
                float timeLimit = in.readFloat();

                if (timeLimit <= 0) {
                    System.err.println("Tiempo límite inválido: " + timeLimit);
                    continue;
                }
                long startTime = System.currentTimeMillis();

                // Continuar ordenando el vector desde el progreso actual
                state = sortIncrementally(state, metodo);

                long elapsedTime = System.currentTimeMillis() - startTime;
                System.out.println("Tiempo de ordenamiento parcial: " + elapsedTime + " ms");

                // Verifica si el worker ya terminó
                if (state.progress == -1) {
                    System.out.println("Ordenamiento completado por Worker.");
                    out.writeObject(state);  // Enviar el estado final
                    out.flush();
                    break;  // Salir del ciclo ya que el worker ha terminado
                } else {
                    // Enviar el estado actualizado al servidor
                    out.writeObject(state);
                    out.flush();
                }

            } catch (EOFException e) {
                System.out.println("El servidor cerró la conexión. Terminando...");
                break;
            } catch (Exception e) {
                System.err.println("Error procesando datos: " + e.getMessage());
            }
        }

    }
}


    public static SortState sortIncrementally(SortState state, int metodo) {
        
        // Determinar qué algoritmo usar basado en el método guardado en el estado
        switch (metodo) {
            case 1: // QuickSort Incremental
                state = quickSortIncremental(state);
                break;
            case 2: // MergeSort Incremental
                state = mergeSortIncremental(state);
                break;
            case 3: // HeapSort Incremental
                state = heapSortIncremental(state);
                break;
            default:
                System.err.println("Método de ordenamiento desconocido.");
        }

        // Verifica si se ha completado el ordenamiento
        if (state.progress == -1) {
            System.out.println("Ordenamiento completado.");
        }

        return state;  // Asegúrate de devolver el estado actualizado.
    }


    public static int partition(List<Long> list, int low, int high) {
        Long pivot = list.get(high); // Selecciona el pivote como el último elemento
        int i = low - 1; // Índice más pequeño

        for (int j = low; j < high; j++) {
            // Si el elemento actual es menor o igual al pivote
            if (list.get(j) <= pivot) {
                i++;
                // Intercambia list[i] y list[j]
                Long temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
            }
        }

        // Intercambia list[i+1] y list[high] (o el pivote)
        Long temp = list.get(i + 1);
        list.set(i + 1, list.get(high));
        list.set(high, temp);

        return i + 1; // Retorna el índice de partición
    }

    public static SortState quickSortIncremental(SortState state) {
    Stack<int[]> stack = new Stack<>();
    if (state.progress == 0) {
        stack.push(new int[]{state.left, state.right});
    } else {
        stack.push(new int[]{state.progress, state.right});
    }

    while (!stack.isEmpty()) {
        int[] range = stack.pop();
        int low = range[0];
        int high = range[1];

        if (low < high) {
            int pi = partition(state.vector, low, high);

            stack.push(new int[]{pi + 1, high});
            stack.push(new int[]{low, pi - 1});
        }

        state.progress = low; // Actualizar el progreso
    }

    return state;
}

    public static void merge(List<Long> list, int left, int mid, int right) {
        // Tamaños de los sublistas
        int n1 = mid - left + 1;
        int n2 = right - mid;

        // Listas temporales para almacenar los elementos de ambas mitades
        List<Long> L = new ArrayList<>(n1);
        List<Long> R = new ArrayList<>(n2);

        // Copiar los datos a las listas temporales L y R
        for (int i = 0; i < n1; i++) {
            L.add(list.get(left + i));
        }
        for (int j = 0; j < n2; j++) {
            R.add(list.get(mid + 1 + j));
        }

        // Índices iniciales de los sublistas y de la lista principal
        int i = 0, j = 0, k = left;

        // Combina las listas temporales nuevamente en list[left..right]
        while (i < n1 && j < n2) {
            if (L.get(i) <= R.get(j)) {
                list.set(k, L.get(i));
                i++;
            } else {
                list.set(k, R.get(j));
                j++;
            }
            k++;
        }

        // Copiar los elementos restantes de L si hay alguno
        while (i < n1) {
            list.set(k, L.get(i));
            i++;
            k++;
        }

        // Copiar los elementos restantes de R si hay alguno
        while (j < n2) {
            list.set(k, R.get(j));
            j++;
            k++;
        }
    }

    public static SortState mergeSortIncremental(SortState state) {
    int currentSize = state.progress == 0 ? 1 : state.progress;
    int n = state.vector.size();

    while (currentSize < n) {
        for (int left = 0; left < n - 1; left += 2 * currentSize) {
            int mid = Math.min(left + currentSize - 1, n - 1);
            int right = Math.min(left + 2 * currentSize - 1, n - 1);

            merge(state.vector, left, mid, right);
        }
        currentSize *= 2;
        state.progress = currentSize;
    }

    return state;
}

    public static void heapify(List<Long> list, int n, int i) {
        int largest = i; // Inicializar el nodo raíz como el más grande
        int left = 2 * i + 1; // Hijo izquierdo
        int right = 2 * i + 2; // Hijo derecho

        // Si el hijo izquierdo es más grande que la raíz
        if (left < n && list.get(left) > list.get(largest)) {
            largest = left;
        }

        // Si el hijo derecho es más grande que el más grande hasta ahora
        if (right < n && list.get(right) > list.get(largest)) {
            largest = right;
        }

        // Si el más grande no es la raíz
        if (largest != i) {
            Collections.swap(list, i, largest);
            heapify(list, n, largest);
        }
    }

    public static SortState heapSortIncremental(SortState state) {
        int n = state.vector.size();

        // Paso 1: Construcción del heap
        if (state.progress == 0) { // Si aún no se ha construido el heap
            for (int i = n / 2 - 1; i >= 0; i--) {
                heapify(state.vector, n, i);
            }
            state.progress = n; // Marca el inicio de la extracción
        }

        // Paso 2: Extracción incremental
        for (int i = state.progress - 1; i > 0; i--) {
            // Intercambia el elemento más grande (raíz) con el último del heap
            Collections.swap(state.vector, 0, i);
            heapify(state.vector, i, 0);

            // Guarda el progreso después de cada extracción
            state.progress--;
            return state; // Pausa aquí y devuelve el estado actualizado
        }

        // Si llegamos aquí, el vector está completamente ordenado
        state.progress = -1; // Marca como completado
        return state; // Devuelve el estado final
    }

}