package lab3_hilossockets;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocket;

public class TaskManager implements Runnable {

    private Socket socket;
    private int WorkerID;
    private String nextWorkerIP;
    private int nextWorkerPORT;
    private long tiempoLimite;
    private static final String IP_SERVER = Config.SERVER_IP_ADDRESS;
    private static final int PORT_SERVER = Config.SERVER_PORT2;
    private static Timer s;

    public TaskManager(Socket socket, int WorkerID, String nextWorkerHOST, int nextWorkerPORT) {
        this.socket = socket;
        this.WorkerID = WorkerID;
        this.nextWorkerIP = nextWorkerHOST;
        this.nextWorkerPORT = nextWorkerPORT;
        this.tiempoLimite = 0;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            List<Long> vector = (List<Long>) in.readObject();
            if (vector == null) {
                System.out.println("Worker_" + WorkerID + " recibió señal de cierre. Finalizando...");
                Worker0.isFinished = true;
                Worker1.isFinished = true;
                socket.close();
                return; // Termina el hilo
            }
            boolean sorted = in.readBoolean();
            long timeLimit = in.readLong();
            int metodo = in.readInt();
            Timer.totalTime = in.readLong(); // Recibir el tiempo acumulado

            System.out.println("Worker_" + WorkerID + " recibió el vector.");

            long startTime = System.currentTimeMillis();

            if (!sorted) {
                Thread sortThread = new Thread(() -> {
                    try {
                        switch (metodo) {
                            case 1:
                                // System.out.println("ENTRE A MERGE");
                                mergeSort(vector, 0, vector.size() - 1);
                                break;
                            case 2:
                                heapSort(vector);
                                break;
                            case 3:
                                quickSort(vector, 0, vector.size() - 1);
                                break;
                        }
                    } catch (Exception e) {
                        System.err.println("Error en el ordenamiento: " + e.getMessage());
                    }
                });

                sortThread.start();
                sortThread.join((long) (timeLimit));
                //System.out.println("SALI DEL HILO DEL SORT");
                long elapsedTime = System.currentTimeMillis() - startTime;
                Timer.totalTime += elapsedTime;

                //System.out.println("VIVO: " + sortThread.isAlive());
                if (sortThread.isAlive()) {
                    sortThread.interrupt();

                    sorted = isSorted(vector);
                    // System.out.println("ESTADO DEL VECTOR: " + sorted);

                    if (!sorted) {//aun no se ordena entonces va al otro worker

                        try (Socket nextWorker = new Socket(nextWorkerIP, nextWorkerPORT); ObjectOutputStream outNextWorker = new ObjectOutputStream(nextWorker.getOutputStream())) {
                            System.out.println("Tiempo tomado por Worker_" + WorkerID + ": " + elapsedTime + " ms");
                            System.out.println("Tiempo límite alcanzado. Interrumpiendo el hilo. Enviando al otro worker... (Worker_" + (WorkerID + 1) % 2 + ")");

                            outNextWorker.writeObject(vector);
                            outNextWorker.writeBoolean(false);
                            outNextWorker.writeLong(timeLimit);
                            outNextWorker.writeInt(metodo);
                            outNextWorker.writeLong(Timer.totalTime);
                            outNextWorker.flush();
                            System.out.println("Vector enviado al siguiente worker.");

                            nextWorker.close();
                        } catch (IOException e) {
                            System.err.println("Error al enviar al siguiente worker: " + e.getMessage());
                        }

                    } else {
                        //si ya terminó entonces devuelve al servidor el vector
                        System.out.println("Tiempo tomado por Worker_" + WorkerID + ": " + elapsedTime + " ms");
                        System.out.println("Ordenamiento terminado por Worker_" + WorkerID);
                        Socket Socket_to_Server = new Socket(IP_SERVER, PORT_SERVER);
                        ObjectOutputStream out = new ObjectOutputStream(Socket_to_Server.getOutputStream());
                        out.writeObject(vector);
                        out.writeLong(Timer.totalTime);
                        out.writeInt(WorkerID);
                        out.flush();
                        this.socket.close();
                        Socket_to_Server.close();
                        Worker1.isFinished = true;
                        Worker0.isFinished = true;

                    }
                } else {
                    //si ya terminó entonces devuelve al servidor el vector
                    System.out.println("Tiempo tomado por Worker_" + WorkerID + ": " + elapsedTime + " ms");
                    System.out.println("Ordenamiento terminado por Worker_" + WorkerID);
                    Socket Socket_to_Server = new Socket(IP_SERVER, PORT_SERVER);
                    ObjectOutputStream out = new ObjectOutputStream(Socket_to_Server.getOutputStream());
                    out.writeObject(vector);
                    out.writeLong(Timer.totalTime);
                    out.writeInt(WorkerID);
                    out.flush();
                    this.socket.close();
                    Socket_to_Server.close();
                    Worker1.isFinished = true;
                    Worker0.isFinished = true;

                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
        } finally {
            try {
                socket.close();
            } catch (IOException ex) {
                Logger.getLogger(TaskManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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

    public static boolean isSorted(List<Long> vector) {
        for (int i = 1; i < vector.size() - 1; i++) {
            if (vector.get(i - 1) > vector.get(i)) {
                return false;
            }
        }
        return true;
    }

}
