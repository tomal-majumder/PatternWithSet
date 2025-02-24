import java.io.*;
import java.util.*;

public class ResultAnalyzer {
    private static final int TOP_N = 2000; // Number of top entries to save

    /**
     * Sorts a map by values in ascending order and saves the top N entries to a file.
     */
    public void saveTopTrajectories(Map<String, Integer> trajectoryStats, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            trajectoryStats.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue()) // Sort in ascending order
//                    .limit(TOP_N)
                    .forEach(entry -> {
                        try {
                            writer.write(entry.getKey() + "," + entry.getValue());
                            writer.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            System.out.println("Saved top " + TOP_N + " trajectories to: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves the per-trajectory match info to a file.
     */
    public void savePerTrajectoryMatchInfo(Map<String, Integer> pertrajMatchInfo, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            pertrajMatchInfo.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())  // Sort by integer value in ascending order
                    .forEach(entry -> {
                        try {
                            writer.write(entry.getKey() + "," + entry.getValue());
                            writer.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            System.out.println("Saved per-trajectory match info to: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a map from a file where each line contains key,value pairs.
     */
    public Map<String, Integer> readMapFromFile(String filename) {
        Map<String, Integer> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    map.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
            System.out.println("Loaded map from: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }


}
