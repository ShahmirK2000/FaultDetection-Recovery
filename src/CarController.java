package src;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.net.*;
import java.io.*;


public class CarController extends Thread {
    private ServerSocket serverSocket;
    private int lastHeartbeat;

    
    private static final String BACKUP_HOST = "127.0.0.1";  // change if backup runs elsewhere (for team members this port may be used)
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
        System.out.println("Car Controller started, waiting for connections...");

        while (true) { // outer loop: accept primary, then backup, then next, etc.
            Socket socket = null;
            Scanner scanner = null;
            try {
                socket = serverSocket.accept();
                socket.setSoTimeout(5000);
                System.out.println("Client connected: " + socket.getInetAddress());

                lastHeartbeat = (int) (System.currentTimeMillis() / 1000);
                scanner = new Scanner(socket.getInputStream());

                while (true) { // inner loop: monitor this one client
                    if (scanner.hasNextLine()) {
                        String message = scanner.nextLine();
                        if ("HEARTBEAT".equals(message)) {
                            lastHeartbeat = (int) (System.currentTimeMillis() / 1000);
                            System.out.println("Received heartbeat at " + lastHeartbeat);
                        } else {
                            System.out.println("Received unknown message: " + message);
                        }
                    }

                    // TIMEOUT: trigger takeover ONCE for this client
                    if ((int) (System.currentTimeMillis() / 1000) - lastHeartbeat > 5) {
                        System.out.println("No heartbeat received for 5 seconds → TAKEOVER.");
                        signalBackupTakeover(); // send once
                        break; // break inner loop to accept() a new client (the backup)
                    }

                    // small nap to avoid a busy-loop
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (scanner != null) scanner.close();
                if (socket != null) {
                    try { socket.close(); } catch (IOException ignored) {}
                }
            }

            // loop back to wait for the backup (or next primary) to connect
            System.out.println("Waiting for next client (backup should connect now)...");
        }
    }

    private void signalBackupTakeover() {
        System.out.println("[Monitor] Heartbeat timeout — signaling backup to TAKEOVER.");
        try (Socket s = new Socket(BACKUP_HOST, BACKUP_CONTROL_PORT);
            PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
            out.println("TAKEOVER");
        } catch (IOException e) {
            System.err.println("[Monitor] Failed to signal TAKEOVER: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        CarController controller = new CarController(6355);
        controller.start();
    }
}