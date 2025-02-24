

public class StateIDGenerator {
    private static int stateCounter = 0; // Static counter shared across the system

    // Method to generate and return the next unique state ID
    public static int generateStateID() {
        return stateCounter++;
    }

    // Optional: reset the counter if needed (for testing or reinitializing)
    public static void reset() {
        stateCounter = 0;
    }
}
