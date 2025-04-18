


import java.io.*;
import java.util.*;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.GeometryEngine;

public class RegexQueryGenerator {
    private Map<String, Polygon> landmarkMap;  // Landmarks (name → polygon)
    private Map<String, List<Point>> trajectoryMap; // Users' trajectories (user → list of points)
    private double thresholdDistanceMeters; // Distance threshold in meters

    public RegexQueryGenerator(Map<String, Polygon> landmarkMap, Map<String, List<Point>> trajectoryMap, double thresholdDistanceMeters) {
        this.landmarkMap = landmarkMap;
        this.trajectoryMap = trajectoryMap;
        this.thresholdDistanceMeters = thresholdDistanceMeters;
    }

    public List<String> generateQueries(int numQueries) {
        List<String> queries = new ArrayList<>();
        int attempts = 0;  // Track how many times we try generating queries

        while (queries.size() < numQueries) { // Extra attempts to ensure we get enough
            String query = generateSingleQuery();
            if (query != null && isValidQuery(query)) {
                queries.add(query);
            }
            attempts++;
        }

        return queries;
    }

    private String generateSingleQuery() {
        // Step 1: Pick a random trajectory
        List<String> userKeys = new ArrayList<>(trajectoryMap.keySet());
        if (userKeys.isEmpty()) return null;
        String randomUser = userKeys.get(new Random().nextInt(userKeys.size()));
        List<Point> trajectory = trajectoryMap.get(randomUser);

        if (trajectory == null || trajectory.size() < 10) return null; // Ensure a valid trajectory

        List<String> selectedLandmarks = new ArrayList<>();
        List<Point> sampledPoints = sampleOrderedPoints(trajectory, 3);

        // Step 2: Ensure each sampled point has a nearby landmark
        for (Point point : sampledPoints) {
            String nearestLandmark = findNearestLandmark(point);
            if (nearestLandmark != null) {
                selectedLandmarks.add(nearestLandmark);
            }
        }

        // Ensure we have exactly 3 landmarks
        if (selectedLandmarks.size() != 3) return null;

        // Step 3: Form the regex query
        return String.join(".?*.", selectedLandmarks);
    }

    private List<Point> sampleOrderedPoints(List<Point> trajectory, int count) {
        List<Point> sampled = new ArrayList<>();
        int step = trajectory.size() / (count + 1); // Divide into (count+1) sections for spacing

        for (int i = 1; i <= count; i++) { // Start from 1 to avoid first point
            sampled.add(trajectory.get(i * step));
        }

        return sampled;
    }

    private String findNearestLandmark(Point point) {
        String nearestLandmark = null;
        double minDistanceMeters = Double.MAX_VALUE;

        for (Map.Entry<String, Polygon> entry : landmarkMap.entrySet()) {
            String landmarkName = entry.getKey();
            Polygon polygon = entry.getValue();
            double distanceMeters = GeometryEngine.distanceBetween(polygon, point);
            if (distanceMeters <= thresholdDistanceMeters && distanceMeters < minDistanceMeters) {
                minDistanceMeters = distanceMeters;
                nearestLandmark = landmarkName;
            }
        }

        return nearestLandmark;
    }

//    private double calculateDistanceToPolygon(Point point, Polygon polygon) {
//        double minDistanceMeters = Double.MAX_VALUE;
//
//        for (int i = 0; i < polygon.getPathCount(); i++) {
//            for (int j = 0; j < polygon.getPointCount(); j++) {
//                Point polygonPoint = polygon.getPoint(j);
//                double distance = haversineDistance(point.getY(), point.getX(), polygonPoint.getY(), polygonPoint.getX());
//
//                if (distance < minDistanceMeters) {
//                    minDistanceMeters = distance;
//                }
//            }
//        }
//
//        return minDistanceMeters;
//    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth's radius in meters
        double latDiff = Math.toRadians(lat2 - lat1);
        double lonDiff = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in meters
    }

    private boolean isValidQuery(String query) {
        String[] parts = query.split("\\.\\?\\*\\.");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(parts[i + 1])) {
                return false; // Reject query if two consecutive landmarks are the same
            }
        }
        return true;
    }

    public void saveQueriesToFile(List<String> queries, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String query : queries) {
                writer.write(query);
                writer.newLine();
            }
            System.out.println("Queries saved to file: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> readQueriesFromFile(String filePath) {
        List<String> queries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) { // Skip empty lines
                    queries.add(line.trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return queries;
    }

    public static void main(String[] args) {
        // Example data setup
        String dataPath = "/Users/tomal/Desktop/MyWorkspace/Winter2025/Sumo_resource/LA_sumo/LA_small/";
        String trajectoryFilePath = dataPath + "trajectories.xml";
        String landmarkFilePath = dataPath + "smallLA.poly.xml";
        //get trajectory map

        Map<String, List<Point>> trajectories= TrajProcessor.parseTrajectories(trajectoryFilePath);
        TrajectoryAnalyzer.generateStatistics(trajectories);
        //get landmark map
        XMLPolygonParser.parseXML(landmarkFilePath);
        Map<String, Polygon> landmarks = XMLPolygonParser.geometryMap;
        XMLPolygonParser.printStats();

        int numQueries = 1000;
        RegexQueryGenerator queryGenerator = new RegexQueryGenerator(landmarks, trajectories, 100);
        List<String> queries = queryGenerator.generateQueries(numQueries);
        queryGenerator.saveQueriesToFile(queries, "queries/queries" + numQueries + ".txt");
        //if saved once
    }
}
