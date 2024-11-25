package lab3_hilossockets;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Worker0 {

    public static final String IP_ADDRESS = Config.SERVER_IP_ADDRESS;
    public static final String WORKER_1_IP_ADDRESS = Config.WORKER_1_IP_ADDRESS;
    public static final int PORT = Config.SERVER_PORT;
    public static final int PORT1 = Config.SERVER_PORT1;
    public static volatile boolean isFinished = false;

    public static void main(String[] args) throws IOException, InterruptedException {
        Work();
    }

    public static void Work() throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("WORKER 0 CONNECTED");

        try {
            while (!isFinished) {
                Socket socket = serverSocket.accept();
                new Thread(new TaskManager(socket, 0, WORKER_1_IP_ADDRESS, PORT1)).start();
            }
        } finally {
            System.out.println("Cerrando Worker...");
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar el ServerSocket: " + e.getMessage());
            }
        }
    }

}
