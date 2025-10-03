package src;

import java.util.Random;
import java.net.*;
import java.io.*;

public class CollisionDetector implements Runnable {
    private enum HealthStatus {
        HEALTHY,
        DEGRADED
    }

    private static final int BACKUP_CONTROL_PORT = 7003;       // control socket for TAKEOVER (2nd socket)
    private static final String BACKUP_BIND_HOST = "0.0.0.0";   // or "127.0.0.1"

    // monitor (CarController) endpoint for heartbeats
    private static final String MONITOR_HOST = "localhost";
    private static final int MONITOR_PORT  = 6355;

    private final Camera camera;
    private final Random random = new Random();

    private final float failChance = 0.25f;
    private final int maxFailCount = 3;
    private int failCount;

    private Socket socket;
    private PrintWriter outWriter;

    private HealthStatus healthStatus = HealthStatus.HEALTHY;

    public CollisionDetector(Camera camera) {
        this.camera = camera;
    }

    // Default camera if none provided 
    public CollisionDetector() {
        this(new Camera("DefaultCam", 0.5f));
    }

    // Connect to CarController to publish heartbeats 
    public void connectToServer(String hostName, int port) throws IOException {
        this.socket = new Socket(hostName, port);
        this.outWriter = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("Connected to server at " + hostName + ":" + port);
    }

    // Main loop
    @Override
    public void run() {
        System.out.println("Collision Detector is running...");

        while (true) {
            publishHealth();

            if (failCount >= maxFailCount) {
                System.out.println("Maximum health check failure count reached. Going offline...");
                break;
            }

            if (isHealthy()) {
                boolean detected = isObjectDetected();

                // ADD: checkpoint decision for passive redundancy
                String decision = detected ? "BRAKE" : "SAFE";
                BackupState.updateDecision(decision);

                String message = detected
                        ? "Object detected by " + camera.getCameraID()
                        : "No object detected by " + camera.getCameraID();
                System.out.println(message);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Collision Detector interrupted.");
                break;
            }

            System.out.println("-----------------------------------");
        }

        cleanup();
        System.out.println("Collision Detector has shut down.");
    }

    // backup replica waits here until monitor sends TAKEOVER
    private static void waitForTakeoverAndRun(CollisionDetector det) {
        System.out.println("[Backup] Waiting for TAKEOVER on port " + BACKUP_CONTROL_PORT);
        try (ServerSocket server = new ServerSocket(BACKUP_CONTROL_PORT, 50, InetAddress.getByName(BACKUP_BIND_HOST))) {
            try (Socket s = server.accept();
                 BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                String line = br.readLine();
                if ("TAKEOVER".equals(line)) {
                    System.out.println("[Backup] TAKEOVER received — restoring checkpoint and starting.");
                    BackupState.State snap = BackupState.load();
                    System.out.println("[Backup] Restored: " + snap);

                    // Now that we are ACTIVE, connect to monitor and start running
                    det.connectToServer(MONITOR_HOST, MONITOR_PORT);
                    det.run();
                }
            }
        } catch (IOException e) {
            System.err.println("[Backup] Control socket error: " + e.getMessage());
        }
    }

    // helper functions
    private boolean isHealthy() {
        return healthStatus == HealthStatus.HEALTHY;
    }

    private boolean isObjectDetected() {
        return camera.isObjectDetected();
    }

    private void publishHealth() {
        HealthStatus previousHealth = healthStatus;

        // random health check result
        healthStatus = random.nextFloat() > failChance ? HealthStatus.HEALTHY : HealthStatus.DEGRADED;

        if (isHealthy()) {
            if (outWriter != null) outWriter.println("HEARTBEAT");
        } else {
            failCount++;
        }

        System.out.println("Health status: " + healthStatus);

        if (previousHealth != healthStatus) {
            switch (previousHealth) {
                case HEALTHY:
                    System.out.printf("  Health status changed from %s to %s.%n", previousHealth, healthStatus);
                    break;
                case DEGRADED:
                    failCount = 0;
                    System.out.printf("  Health status changed from %s to %s.%n", previousHealth, healthStatus);
                    break;
            }
        }

        if (healthStatus == HealthStatus.DEGRADED) {
            System.out.println("  Failure count: " + failCount);
        }
    }

    private void cleanup() {
        try {
            if (outWriter != null) outWriter.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Cleaned up resources.");
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    // main executable
    public static void main(String[] args) {
        boolean isBackup = false;
        for (String a : args) {
            if ("--backup".equalsIgnoreCase(a) || "-b".equalsIgnoreCase(a)) isBackup = true;
        }

        CollisionDetector detector = new CollisionDetector(new Camera("Camera-1", 0.75f));

        if (isBackup) {
            // PASSIVE REPLICA: wait idle, then on TAKEOVER connect + run
            waitForTakeoverAndRun(detector);
        } else {
            // PRIMARY: connect now and start running
            try {
                detector.connectToServer(MONITOR_HOST, MONITOR_PORT);
                detector.run();
            } catch (UnknownHostException e) {
                System.err.println("Unknown host: " + MONITOR_HOST);
            } catch (IOException e) {
                System.err.println("I/O error when connecting to " + MONITOR_HOST);
            }
        }
    }
}