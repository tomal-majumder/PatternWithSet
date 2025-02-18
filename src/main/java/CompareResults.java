import java.io.*;
import java.util.*;

public class CompareResults {

    public static void main(String[] args) {
        String separateFile = "results/10_separate_result.txt";
        String mergedFile = "results/10_merged_result.txt";

        Set<String> separateMatches = readMatches(separateFile);
        Set<String> mergedMatches = readMatches(mergedFile);

        // Find missing matches
        separateMatches.removeAll(mergedMatches);

        // Print missing matches
        if (separateMatches.isEmpty()) {
            System.out.println("All matches from the separate file are present in the merged file.");
        } else {
            System.out.println("Missing matches in the merged file:");
            for (String match : separateMatches) {
                System.out.println(match);
            }
        }
    }

    private static Set<String> readMatches(String fileName) {
        Set<String> matches = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("is matched with Query id")) {
                    String[] parts = line.split(" is matched with Query id: ");
                    if (parts.length == 2) {
                        String objectId = parts[0].trim();
                        String queryId = parts[1].split(":")[0].trim();
                        matches.add(objectId + " -> Query " + queryId);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file " + fileName + ": " + e.getMessage());
        }
        return matches;
    }
}
