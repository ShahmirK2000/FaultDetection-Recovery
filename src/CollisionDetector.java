package src;

import java.util.Random;
import java.net.*;
import java.io.*;

public class CollisionDetector implements Runnable {
    private enum HealthStatus {
        HEALTHY,
        DEGRADED
    }

    private final Camera camera;
    private final Random random;

    // control channel for TAKEOVER
    private static final int BACKUP_CONTROL_PORT = 7003;
    private static final String BACKUP_BIND_HOST = "0.0.0.0"; // or "127.0.0.1"


    private final float failChance = 0.25f;
    private final int maxFailCount = 3;
    private int failCount;

    private Socket socket;
    private PrintWriter outWriter;
    
    private HealthStatus healthStatus;

    /**
     * Constructor
     * @param camera the camera object used for detection
     */
    public CollisionDetector(Camera camera) {
        this.camera = camera;
        this.random = new Random();
        this.failCount = 0;
        this.healthStatus = HealthStatus.HEALTHY;
    }

    /**
     * Default constructor using a default camera
     * with ID "DefaultCam" and detection probability of 0.5
     */
    public CollisionDetector() {
        this(new Camera("DefaultCam", 0.5f));
    }

    /**
     * Connect to the server
     * @param hostName the server host name or IP address
     * @param port the server port number
     * @throws IOException if an I/O error occurs when creating the socket
     */
    public void connectToServer(String hostName, int port) throws IOException {
        this.socket = new Socket(hostName, port);
        this.outWriter = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server at " + hostName + ":" + port);
    }

    @Override
    public void run() {
        System.out.println("Collision Detector is running...");

        while (true) {
            publishHealth();

            if (failCount >= maxFailCount) {
                System.out.println("Maximum health check failure count reached. Going offline...");
                break;
            }

            if(isHealthy()) {
                String message = isObjectDetected() ? 
                    "Object detected by " + camera.getCameraID() :
                    "No object detected by " + camera.getCameraID(); 
                System.out.println(message);
            }

            try {
                Thread.sleep(1000); // Simulate periodic checks
            } catch (InterruptedException e) {
                System.out.println("Collision Detector interrupted.");
                break;
            }

            System.out.println("-----------------------------------");
        }

        cleanup();
        System.out.println("Collision Detector has shut down.");
    }

    /** Check if the system is healthy */
    private boolean isHealthy() {
        return healthStatus == HealthStatus.HEALTHY;
    }

    /** Simulate object detection using the camera */
    private boolean isObjectDetected() {
        return camera.isObjectDetected();
    }

    /** Publish health status to the server */
    private void publishHealth() {
        HealthStatus previousHealth = healthStatus;

        // Simulate health check with a random chance of failure
        healthStatus = random.nextFloat() > failChance ? HealthStatus.HEALTHY : HealthStatus.DEGRADED;

        if (isHealthy()) {
            outWriter.println("HEARTBEAT");
        } else {
            failCount++;
        }

        System.out.println("Health status: " + healthStatus);
        
        if (previousHealth != healthStatus) {
            switch (previousHealth) {
                case HEALTHY:
                System.out.println(
                    String.format("  Health status changed from %s to %s.", previousHealth, healthStatus));
                break;
                
                case DEGRADED:
                failCount = 0;
                System.out.println(
                    String.format("  Health status changed from %s to %s.", previousHealth, healthStatus));
                break;
            }
        }

        if (healthStatus == HealthStatus.DEGRADED) {
            System.out.println("  Failure count: " + failCount);
        }
    }

    /** Cleanup resources */
    private void cleanup() {
        try {
            if (outWriter != null) {
                outWriter.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Cleaned up resources.");
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    private static void waitForTakeoverAndRun(CollisionDetector det) {
        System.out.println("[Backup] Waiting for TAKEOVER on port " + BACKUP_CONTROL_PORT);
        try (ServerSocket server = new ServerSocket(BACKUP_CONTROL_PORT, 50, InetAddress.getByName(BACKUP_BIND_HOST))) {
            try (Socket s = server.accept();
                BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                String line = br.readLine();
                if ("TAKEOVER".equals(line)) {
                    System.out.println("[Backup] TAKEOVER received: restoring checkpoint and starting.");
                    BackupState.State snap = BackupState.load();
                    System.out.println("[Backup] Restored: " + snap);
                    det.run(); // start normal detection loop
                }
            }
        } catch (IOException e) {
            System.err.println("[Backup] Control socket error: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        boolean isBackup = false;
        for (String a : args) {
            if ("--backup".equalsIgnoreCase(a) || "-b".equalsIgnoreCase(a)) isBackup = true;
        }

        CollisionDetector detector = new CollisionDetector(new Camera("Camera-1", 0.75f));
        if (isBackup) {
            waitForTakeoverAndRun(detector); // ADD
        } else {
            detector.run();
        }

        String hostName = "localhost";
        int port = 6355;
        try {
            detector.connectToServer(hostName, port);

            detector.run();
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + hostName);
        } catch (IOException e) {
            System.err.println("I/O error when connecting to " + hostName);
        }
    }
} // end class CollisionDetector