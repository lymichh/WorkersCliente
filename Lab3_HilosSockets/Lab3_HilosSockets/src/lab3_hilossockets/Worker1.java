package lab3_hilossockets;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Worker1 {

    public static final String IP_ADDRESS = Config.SERVER_IP_ADDRESS;
    public static final String WORKER_0_IP_ADDRESS = Config.WORKER_0_IP_ADDRESS;
    public static final int PORT = Config.SERVER_PORT;
    public static final int PORT1 = Config.SERVER_PORT1;
    public static volatile boolean isFinished = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        Work();
    }

    public static void Work() throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(PORT1);
        System.out.println("WORKER 1 CONNECTED");

        try {
            while (!isFinished) {
                Socket socket = serverSocket.accept();
                new Thread(new TaskManager(socket, 1, WORKER_0_IP_ADDRESS, PORT)).start();
            }
        } finally {
            System.out.println("Cerrando Worker1...");
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el ServerSocket: " + e.getMessage());
            }
        }
    }

}
