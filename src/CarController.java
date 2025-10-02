package src;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class CarController extends Thread {
    private ServerSocket serverSocket;
    private int lastHeartbeat;

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