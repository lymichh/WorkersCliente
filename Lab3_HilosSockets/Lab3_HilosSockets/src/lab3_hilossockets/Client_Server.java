package lab3_hilossockets;

import java.io.*;
import static java.lang.Math.random;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLServerSocket;

public class Client_Server {

    public static Scanner scanner = new Scanner(System.in);
    public static List<Long> vector;
    public static int opcion;
    public static float timeLimit;
    public static boolean sorted = false;
    public static long totalTime = 0;
    public static long elapsedTime = 0;
    public static int workerActual = 0;

    public static final String IP_ADDRESS = Config.SERVER_IP_ADDRESS;
    public static final int PORT = Config.SERVER_PORT;
    public static final int MAX_WORKERS = Config.SERVER_MAX_WORKERS;

    public static void main(String[] args) throws FileNotFoundException {
        Cliente();
        Server();
    }

    public static void Cliente() throws FileNotFoundException {
        // Elegir el problema
        try {
            System.out.println("Seleccione el problema a resolver:");
            System.out.println("1. Ordenar con Mergesort");
            System.out.println("2. Ordenar con Heapsort");
            System.out.println("3. Ordenar con Quicksort");
            opcion = scanner.nextInt();

            if (opcion < 1 || opcion > 3) {
                System.out.println("Opción inválida. Saliendo...");
                System.exit(1);
            }

            System.out.print("Ingrese el tiempo límite por worker (en segundos): ");
            timeLimit = scanner.nextFloat();

            if (timeLimit <= 0) {
                System.out.println("El tiempo límite debe ser mayor que cero. Saliendo...");
                System.exit(1);
            }

            llenarVector();
        } catch (InputMismatchException e) {
            System.err.println("Entrada inválida. Saliendo...");
            System.exit(1);
        }
    }

    public static void Server() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Esperando conexiones de workers...");

            // Aceptar conexiones de los dos workers
            Socket worker_0 = serverSocket.accept();
            System.out.println("Worker_0 conectado.");
            new Thread(() -> handleWorker(worker_0, 0)).start();

            Socket worker_1 = serverSocket.accept();
            System.out.println("Worker_1 conectado.");
            new Thread(() -> handleWorker(worker_1, 1)).start();

            // El servidor puede realizar tareas adicionales aquí si es necesario
            System.out.println("Conexiones inicializadas. Esperando procesamiento...");
        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    private static void handleWorker(Socket socket, int workerId) {
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            while (!sorted) {
                // Enviar datos al worker
                out.writeInt(opcion);
                out.writeFloat(timeLimit * 1000); // Tiempo límite en milisegundos
                out.writeObject(vector);
                out.flush();

                // Recibir datos del worker
                vector = (List<Long>) in.readObject();
                sorted = isSorted(vector);
                elapsedTime = in.readLong();
                totalTime += elapsedTime;

                // Log de progreso
                System.out.println("Worker_" + workerId + " procesando...");
                System.out.println("Tiempo tomado por Worker_" + workerId + ": " + elapsedTime + " ms");
            }

            System.out.println("Worker_" + workerId + " finalizó el procesamiento.");
            System.out.println("Vector ordenado: " + vector);
            System.out.println("Tiempo total tomado: " + totalTime + " ms");

        } catch (Exception e) {
            System.err.println("Error procesando datos con Worker_" + workerId + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error cerrando socket de Worker_" + workerId + ": " + e.getMessage());
            }
        }
    }

    public static void llenarVector() throws FileNotFoundException {
        vector = new ArrayList<>();
        try {
            FileReader fr = new FileReader("random_numbers.txt");
            BufferedReader bf = new BufferedReader(fr);
            String cadena;
            while ((cadena = bf.readLine()) != null) {
                vector.add(Long.parseLong(cadena));
            }
        } catch (Exception e) {
            System.out.println("Error " + e);
        }
    }

    public static boolean isSorted(List<Long> vector) {
        for (int i = 0; i < vector.size() - 1; i++) {
            if (vector.get(i) > vector.get(i + 1)) {
                return false;
            }
        }
        return true;
    }
}
