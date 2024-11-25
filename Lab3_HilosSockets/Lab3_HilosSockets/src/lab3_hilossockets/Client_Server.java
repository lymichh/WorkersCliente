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
    public static Long timeLimit;
    public static boolean sorted = false;
    public static long totalTime;
    public static long elapsedTime = 0;
    public static int workerActual = 0;
    public static int WorkerID;

    public static final String IP_ADDRESS = Config.SERVER_IP_ADDRESS;
    public static final int PORT = Config.SERVER_PORT;
    public static final int PORT1 = Config.SERVER_PORT1;
    public static final int PORT2 = Config.SERVER_PORT2;

    public static void main(String[] args) throws FileNotFoundException, IOException {
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
                System.exit(0);
            }

            System.out.print("Ingrese el tiempo límite por worker (en segundos): ");
            timeLimit = (long) (scanner.nextFloat() * 1000);

            if (timeLimit <= 0) {
                System.out.println("El tiempo límite debe ser mayor que cero. Saliendo...");
                System.exit(0);
            }

            llenarVector();
        } catch (InputMismatchException e) {
            System.err.println("Entrada inválida. Saliendo...");
            System.exit(1);
        }
    }

    public static void Server() throws IOException {
        try {
            //Se conecta al worker_0
            Socket socket_0 = new Socket(IP_ADDRESS, PORT);

            sortVector(socket_0, vector, sorted, timeLimit, opcion);

            socket_0.close();

            //Ahora a esperar respuesta de algun worker
            ServerSocket respuesta = new ServerSocket(PORT2);
            Socket socket_1 = null;
            while (!sorted) {
                socket_1 = respuesta.accept();
                ObjectInputStream in = new ObjectInputStream(socket_1.getInputStream());

                vector = (List<Long>) in.readObject();
                totalTime = in.readLong();
                WorkerID = in.readInt();

                sorted = isSorted(vector);
            }

            // Resultado final
            System.out.println("Worker_" + WorkerID + " terminó de organizar el vector.");
            System.out.println("Vector ordenado: " + vector);
            System.out.println("Tiempo total tomado: " + totalTime + " ms");
            socket_1.close();
            
            try (Socket worker0Socket = new Socket(Config.WORKER_0_IP_ADDRESS, Config.SERVER_PORT); ObjectOutputStream worker0Out = new ObjectOutputStream(worker0Socket.getOutputStream())) {
                worker0Out.writeObject(null); // Enviar un vector nulo como señal de cierre
            }

            try (Socket worker1Socket = new Socket(Config.WORKER_1_IP_ADDRESS, Config.SERVER_PORT1); ObjectOutputStream worker1Out = new ObjectOutputStream(worker1Socket.getOutputStream())) {
                worker1Out.writeObject(null); // Enviar un vector nulo como señal de cierre
            }

        } catch (Exception e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }

    }

    private static void sortVector(Socket socket, List<Long> vector, boolean ordenado, long timeLimit, int metodo) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(vector);
        out.writeBoolean(ordenado);
        out.writeLong(timeLimit);
        out.writeInt(metodo);
        out.writeLong(0);
        out.flush();
    }

    private static void handleWorker(Socket socket, ObjectInputStream in, ObjectOutputStream out, int workerId) {
        try {

            // Enviar datos al worker
            out.writeInt(opcion);
            out.writeLong(timeLimit); // Tiempo límite en milisegundos
            out.writeObject(vector);
            out.flush();

            // Recibir datos del worker
            vector = (List<Long>) in.readObject();
            elapsedTime = in.readLong();
            totalTime += elapsedTime;

            sorted = isSorted(vector);

            // Log de progreso
            System.out.println("Worker_" + workerId + " procesó.");
            System.out.println("Tiempo tomado por Worker_" + workerId + ": " + elapsedTime + " ms");

        } catch (Exception e) {
            System.err.println("Error procesando datos con Worker_" + workerId + ": " + e.getMessage());
        }
    }

    public static void llenarVector() throws FileNotFoundException {
        vector = new ArrayList<>();
        try (BufferedReader bf = new BufferedReader(new FileReader("random_numbers.txt"))) {
            String cadena;
            while ((cadena = bf.readLine()) != null) {
                vector.add(Long.parseLong(cadena));
            }
        } catch (Exception e) {
            System.err.println("Error " + e);
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
