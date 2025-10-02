package src;

import java.util.Random;

public class Camera {

    private final String cameraID;
    private final float detectionProbability;
    private final Random random;
    
    /**
     * Constructor
     * @param cameraID the camera ID, a string representing the unique identifier of the camera
     * @param detectionProbability the probability of detecting an object, a float between 0 and 1
     */
    public Camera(String cameraID, float detectionProbability) {
        this.cameraID = cameraID;
        this.detectionProbability = detectionProbability;
        this.random = new Random();
    }

    /**
     * Constructor with seed for reproducibility
     * @param cameraID the camera ID, a string representing the unique identifier of the camera
     * @param detectionProbability the probability of detecting an object, a float between 0 and 1
     * @param seed the seed for the random number generator
     */
    public Camera(String cameraID, float detectionProbability, long seed) {
        this.cameraID = cameraID;
        this.detectionProbability = detectionProbability;
        this.random = new Random(seed);
    }

    /**
     * Get the camera ID
     * @return the camera ID, a string representing the unique identifier of the camera
     */
    public String getCameraID() {
        return cameraID;
    }

    public String toString() {
        return "Camera ID: " + cameraID + ", Detection Probability: " + detectionProbability;
    }

    /**
     * Simulate object detection
     * Contains placeholder logic for detecting an object that randomly returns true or false
     * @return true if an object is detected, false otherwise
     */
    public boolean isObjectDetected() {
        return random.nextFloat() < detectionProbability;
    }

}// end class Camera