

import com.esri.core.geometry.Point;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TrajectoryBuilder {

    static class TrajectoryPoint {
        Point point;
        long time;

        public TrajectoryPoint(Point point, long time) {
            this.point = point;
            this.time = time;
        }
    }

    /**
     * Reads raw trajectory data from multiple text files and saves results per ObjectID in separate files.
     */
    public static Map<Integer, List<TrajectoryPoint>> processAndSaveTrajectories(String inputFolder, String outputFolder) {
        Map<Integer, List<TrajectoryPoint>> trajectoryMap = new HashMap<>();

        try {
            Files.walk(Paths.get(inputFolder))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .forEach(path -> processFile(path.toFile(), trajectoryMap));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Sort and save trajectories per ObjectID
        for (Map.Entry<Integer, List<TrajectoryPoint>> entry : trajectoryMap.entrySet()) {
            int objectId = entry.getKey();
            List<TrajectoryPoint> sortedPoints = entry.getValue();
            if(sortedPoints.size() < 50) {
                continue; //do not process them
            }
            sortedPoints.sort(Comparator.comparingLong(tp -> tp.time)); // Sort by time
            saveTrajectoryToFile(objectId, sortedPoints, outputFolder);
        }

        System.out.println("Processing complete. Trajectories saved to: " + outputFolder);
        return trajectoryMap;
    }

    /**
     * Reads a specific trajectory file and reconstructs the trajectory.
     */
    public static List<Point> readTrajectoryFromFile(int objectId, String outputFolder) {
        List<Point> trajectory = new ArrayList<>();
        String fileName = outputFolder + "/object_" + objectId + ".txt";

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 3) continue;

                double lon = Double.parseDouble(parts[0].trim());
                double lat = Double.parseDouble(parts[1].trim());
                trajectory.add(new Point(lon, lat));
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + fileName);
        }

        return trajectory;
    }

    private static void processFile(File file, Map<Integer, List<TrajectoryPoint>> trajectoryMap) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length != 4) continue;

                int objectId = Integer.parseInt(parts[0].trim());
                double lat = Double.parseDouble(parts[1].trim());
                double lon = Double.parseDouble(parts[2].trim());
                long time = Long.parseLong(parts[3].trim());

                Point point = new Point(lon, lat); // Esri Point (Lon first, then Lat)

                trajectoryMap.computeIfAbsent(objectId, k -> new ArrayList<>())
                        .add(new TrajectoryPoint(point, time));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveTrajectoryToFile(int objectId, List<TrajectoryPoint> trajectory, String outputFolder) {
        String fileName = outputFolder + "/object_" + objectId + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (TrajectoryPoint tp : trajectory) {
                writer.write(tp.point.getX() + "," + tp.point.getY() + "," + tp.time);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving file: " + fileName);
        }
    }

    public static Map<Integer, List<Point>> readSavedTrajectories(String folderPath) {
        Map<Integer, List<Point>> trajectoryMap = new HashMap<>();

        try {
            Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("object_\\d+\\.txt")) // Updated regex for object_<id>.txt
                    .forEach(path -> processTrajectoryFile(path.toFile(), trajectoryMap));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return trajectoryMap;
    }

    private static void processTrajectoryFile(File file, Map<Integer, List<Point>> trajectoryMap) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String fileName = file.getName();

            // Extract Object ID correctly from 'object_<id>.txt'
            int objectId = Integer.parseInt(fileName.replaceAll("object_(\\d+)\\.txt", "$1"));

            List<Point> trajectory = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 3) continue; // Ensure correct format

                double lon = Double.parseDouble(parts[0].trim()); // Longitude
                double lat = Double.parseDouble(parts[1].trim()); // Latitude
                trajectory.add(new Point(lon, lat));
            }
            trajectoryMap.put(objectId, trajectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Computes and prints statistics for trajectory lengths.
     */
    public static void printTrajectoryStats(Map<Integer, List<Point>> trajectoryMap) {
        int totalObjects = trajectoryMap.size();
        int minLength = Integer.MAX_VALUE;
        int maxLength = Integer.MIN_VALUE;
        int totalLength = 0;
        Map<Integer, Integer> lengthDistribution = new HashMap<>(); // Maps trajectory length -> count

        for (List<Point> trajectory : trajectoryMap.values()) {
            int length = trajectory.size();
            minLength = Math.min(minLength, length);
            maxLength = Math.max(maxLength, length);
            totalLength += length;

            // Update length distribution count
            lengthDistribution.put(length, lengthDistribution.getOrDefault(length, 0) + 1);
        }

        double avgLength = totalObjects > 0 ? (double) totalLength / totalObjects : 0;

        // Find most and least common trajectory lengths
        int mostCommonLength = -1, leastCommonLength = -1;
        int maxCount = Integer.MIN_VALUE, minCount = Integer.MAX_VALUE;

        for (Map.Entry<Integer, Integer> entry : lengthDistribution.entrySet()) {
            int length = entry.getKey();
            int count = entry.getValue();

            if (count > maxCount) {
                maxCount = count;
                mostCommonLength = length;
            }
            if (count < minCount) {
                minCount = count;
                leastCommonLength = length;
            }
        }

        // Print statistics
        System.out.println("Trajectory Statistics:");
        System.out.println("Total Objects: " + totalObjects);
        System.out.println("Max Trajectory Length: " + maxLength);
        System.out.println("Min Trajectory Length: " + minLength);
        System.out.println("Average Trajectory Length: " + avgLength);
        System.out.println("\nTrajectory Length Distribution:");
        for (Map.Entry<Integer, Integer> entry : lengthDistribution.entrySet()) {
            System.out.println("  Length " + entry.getKey() + " -> " + entry.getValue() + " objects");
        }
        System.out.println("\nMost Common Trajectory Length: " + mostCommonLength + " (appeared " + maxCount + " times)");
        System.out.println("Least Common Trajectory Length: " + leastCommonLength + " (appeared " + minCount + " times)");
    }

    public static void main(String[] args) {
        String inputFolder = "LA_50K"; // Change to actual folder path
        String outputFolder = "/Users/tomal/Desktop/Data/LA50K_processed"; // Change to where you want to store trajectories
        Map<Integer, List<Point>> map = readSavedTrajectories(outputFolder);
        printTrajectoryStats(map);
        // First, process data and save trajectories
        //Map<Integer, List<TrajectoryPoint>> trajectoryMap = processAndSaveTrajectories(inputFolder, outputFolder);
        //System.out.println(trajectoryMap.size());
        //printTrajectoryStats(trajectoryMap);

//        // Later, read a specific trajectory from saved files
//        int objectIdToRead = 101; // Change to desired ObjectID
//        List<Point> trajectory = readTrajectoryFromFile(objectIdToRead, outputFolder);
//
//        // Print loaded trajectory
//        System.out.println("Trajectory for ObjectID: " + objectIdToRead);
//        for (Point p : trajectory) {
//            System.out.println("  (" + p.getX() + ", " + p.getY() + ")");
//        }
    }
}
