package src;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.net.*;
import java.io.*;


public class CarController extends Thread {
    private ServerSocket serverSocket;
    private int lastHeartbeat;

    
    private static final String BACKUP_HOST = "127.0.0.1";  // change if backup runs elsewhere
    private static final int BACKUP_CONTROL_PORT = 7003;


    public CarController(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        Scanner scanner = null;
        Socket socket = null;
        try {
            System.out.println("Car Controller started, waiting for connections...");
            socket = serverSocket.accept();
            socket.setSoTimeout(5000);
            System.out.println("Client connected: " + socket.getInetAddress());

            lastHeartbeat = (int) (System.currentTimeMillis() / 1000);

            scanner = new Scanner(socket.getInputStream());

            while (true) {
                if (scanner.hasNextLine()) {
                    String message = scanner.nextLine();
                    if (message.equals("HEARTBEAT")) {
                        lastHeartbeat = (int) (System.currentTimeMillis() / 1000);
                        System.out.println("Received heartbeat at " + lastHeartbeat);
                    } else {
                        System.out.println("Received unknown message: " + message);
                    }
                }

                if ((int) (System.currentTimeMillis() / 1000) - lastHeartbeat > 5) {
                    System.out.println("No heartbeat received for 5 seconds, taking action!");
                    lastHeartbeat = (int) (System.currentTimeMillis() / 1000); // Reset to avoid repeated actions
                    break; // Exit loop or take other actions
                }

                System.out.println("[Monitor] Heartbeat timeout — signaling backup to TAKEOVER.");
                try (Socket s = new Socket(BACKUP_HOST, BACKUP_CONTROL_PORT);
                    PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                    out.println("TAKEOVER");
                } catch (IOException e) {
                    System.err.println("[Monitor] Failed to signal TAKEOVER: " + e.getMessage());
                }

                // NOTE: we're not reviving the dead primary here; the backup will begin running

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(scanner != null) {
                scanner.close();
            }

            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Car Controller shutting down.");
    }

    public static void main(String[] args) {
        CarController controller = new CarController(6355);
        controller.start();
    }
}