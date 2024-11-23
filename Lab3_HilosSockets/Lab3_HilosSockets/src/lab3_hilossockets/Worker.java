package lab3_hilossockets;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import static lab3_hilossockets.Client_Server.IP_ADDRESS;
import static lab3_hilossockets.Client_Server.PORT;

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
                    int metodo = in.readInt();
                    float timeLimit = in.readFloat();

                    if (timeLimit <= 0) {
                        System.err.println("Tiempo límite inválido: " + timeLimit);
                        continue;
                    }

                    List<Long> vector = (List<Long>) in.readObject();
                    long startTime = System.currentTimeMillis();
                    sortPartially(vector, metodo, timeLimit);
                    long elapsedTime = System.currentTimeMillis() - startTime;

                    // Enviar datos de vuelta al servidor
                    out.writeObject(vector);
                    out.writeLong(elapsedTime);
                    out.flush();
                } catch (EOFException e) {
                    System.out.println("El servidor cerró la conexión. Terminando...");
                    break;
                } catch (SocketException e) {
                    System.out.println("Error de conexión. Terminando...");
                    break;
                } catch (Exception e) {
                    System.err.println("Error procesando datos: " + e.getMessage());
                }
            }
        }
    }

    public static void sortPartially(List<Long> vector, int metodo, float timeLimit) throws InterruptedException {
        // Implementa un ordenamiento parcial según el método seleccionado y el tiempo límite
        long startTime = System.currentTimeMillis();

        Thread sortThread = new Thread(() -> {
            try {
                switch (metodo) {
                    case 1:
                        mergeSort(vector, 0, vector.size() - 1);
                        break;
                    case 2:
                        heapSort(vector);
                        break;
                    case 3:
                        quickSort(vector, 0, vector.size() - 1);
                        break;
                    default:
                        System.err.println("Método de ordenamiento desconocido.");
                }
            } catch (Exception e) {
                System.err.println("Error en el ordenamiento: " + e.getMessage());
            }
        });

        sortThread.start();
        sortThread.join((long) (timeLimit * 1000));
        
        if (sortThread.isAlive()) {
            sortThread.interrupt();
            System.out.println("Tiempo límite alcanzado. Interrumpiendo el hilo.");
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Tiempo medido en sortPartially: " + elapsedTime + " ms");
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

    public static void quickSort(List<Long> list, int low, int high) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        if (low < high) {
            // Obtén el índice de partición
            int pi = partition(list, low, high);

            // Ordena los elementos antes y después de la partición
            quickSort(list, low, pi - 1);
            quickSort(list, pi + 1, high);
        }
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

    public static void mergeSort(List<Long> list, int left, int right) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        if (left < right) {
            // Encuentra el punto medio de la lista
            int mid = left + (right - left) / 2;

            // Ordena la primera y segunda mitad recursivamente
            mergeSort(list, left, mid);
            mergeSort(list, mid + 1, right);

            // Combina las dos mitades ordenadas
            merge(list, left, mid, right);
        }
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
            // Intercambiar los elementos
            Long swap = list.get(i);
            list.set(i, list.get(largest));
            list.set(largest, swap);

            // Recursivamente aplicar heapify en el subárbol afectado
            heapify(list, n, largest);
        }
    }

    public static void heapSort(List<Long> list) {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }
        int n = list.size();

        // Construir el heap (reorganizar la lista)
        for (int i = n / 2 - 1; i >= 0; i--) {
            heapify(list, n, i);
        }

        // Extraer elementos del heap uno por uno
        for (int i = n - 1; i > 0; i--) {
            // Mover la raíz actual al final
            Long temp = list.get(0);
            list.set(0, list.get(i));
            list.set(i, temp);

            // Llamar heapify en el subárbol reducido
            heapify(list, i, 0);
        }
    }

}
