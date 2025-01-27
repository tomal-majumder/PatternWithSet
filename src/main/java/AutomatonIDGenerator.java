public class AutomatonIDGenerator {
    private static int automatonCounter = 0; // Static counter for automaton IDs

    // Method to generate and return the next unique automaton ID
    public static int generateAutomatonID() {
        return automatonCounter++;
    }

    // Optional: reset the counter if needed (for testing or reinitializing)
    public static void reset() {
        automatonCounter = 0;
    }
}
