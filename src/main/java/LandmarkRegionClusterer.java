import com.esri.core.geometry.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import org.openstreetmap.gui.jmapviewer.*;

import javax.swing.*;

public class LandmarkRegionClusterer {
    private Map<String, Polygon> landmarks;
    private double eps;
    private int minPoints;
    private Map<String, List<String>> clusterPolygons = new HashMap<>();
    private Map<String, String> polygonToRegion = new HashMap<>();

    public LandmarkRegionClusterer(Map<String, Polygon> landmarks, double eps, int minPoints) {
        this.landmarks = landmarks;
        this.eps = eps;
        this.minPoints = minPoints;
    }

    public Map<String, Polygon> clusterLandmarksAndSave(String outputPath) {
        List<Point> centroids = landmarks.values().stream().map(this::getCentroid).collect(Collectors.toList());
        List<List<Integer>> clusters = dbscan(centroids);
        Map<String, Polygon> regions = new HashMap<>();
        int regionIndex = 1;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            for (List<Integer> cluster : clusters) {
                if (cluster.size() < minPoints) continue;
                String regionName = "Region_" + regionIndex++;
                List<String> polygonNames = new ArrayList<>();
                Envelope2D envelope = new Envelope2D();
                boolean first = true;

                for (int idx : cluster) {
                    String polygonName = (String) landmarks.keySet().toArray()[idx];
                    polygonNames.add(polygonName);
                    polygonToRegion.put(polygonName, regionName);
                    Envelope2D polyEnv = new Envelope2D();
                    landmarks.get(polygonName).queryEnvelope2D(polyEnv);
                    if (first) { envelope.setCoords(polyEnv); first = false; }
                    else { envelope.merge(polyEnv); }
                }
                Polygon mbr = createRectangleFromEnvelope(envelope);
                regions.put(regionName, mbr);
                clusterPolygons.put(regionName, polygonNames);
                writer.write(regionName + ", Polygons: " + polygonNames);
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("region_info"))) {
            for (Map.Entry<String, Polygon> entry : regions.entrySet()) {
                String regionName = entry.getKey();
                Polygon mbr = entry.getValue();
                String wkt = GeometryEngine.geometryToWkt(mbr, 0);
                writer.write(regionName + "," + wkt);
                writer.newLine();
            }
            System.out.println("Region MBRs saved to: " + "region-info");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return regions;
    }

    public static Map<String, Polygon> loadRegionMBRs(String filePath) {
        Map<String, Polygon> regions = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    String regionName = parts[0].trim();
                    String wkt = parts[1].trim();
                    Geometry geometry = GeometryEngine.geometryFromWkt(wkt, 0, Geometry.Type.Polygon);
                    if (geometry instanceof Polygon) {
                        regions.put(regionName, (Polygon) geometry);
                    }
                }
            }
            System.out.println("Region MBRs loaded from: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return regions;
    }

    private List<List<Integer>> dbscan(List<Point> points) {
        List<List<Integer>> clusters = new ArrayList<>();
        boolean[] visited = new boolean[points.size()];
        for (int i = 0; i < points.size(); i++) {
            if (visited[i]) continue;
            visited[i] = true;
            List<Integer> neighbors = regionQuery(points, points.get(i));
            if (neighbors.size() < minPoints) continue;
            List<Integer> cluster = expandCluster(points, i, neighbors, visited);
            clusters.add(cluster);
        }
        return clusters;
    }

    private List<Integer> expandCluster(List<Point> points, int index, List<Integer> neighbors, boolean[] visited) {
        List<Integer> cluster = new ArrayList<>(neighbors);
        for (int neighbor : new ArrayList<>(neighbors)) {
            if (!visited[neighbor]) {
                visited[neighbor] = true;
                List<Integer> neighborPts = regionQuery(points, points.get(neighbor));
                if (neighborPts.size() >= minPoints) {
                    neighbors.addAll(neighborPts);
                }
            }
            if (!cluster.contains(neighbor)) {
                cluster.add(neighbor);
            }
        }
        return cluster;
    }

    private List<Integer> regionQuery(List<Point> points, Point center) {
        List<Integer> neighbors = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            if (haversineDistance(center, points.get(i)) <= eps) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }

    private Polygon createRectangleFromEnvelope(Envelope2D envelope) {
        Polygon polygon = new Polygon();
        polygon.startPath(envelope.xmin, envelope.ymin);
        polygon.lineTo(envelope.xmin, envelope.ymax);
        polygon.lineTo(envelope.xmax, envelope.ymax);
        polygon.lineTo(envelope.xmax, envelope.ymin);
        polygon.closeAllPaths();
        return polygon;
    }

    private double haversineDistance(Point p1, Point p2) {
        final double R = 6371000;
        double latDiff = Math.toRadians(p2.getY() - p1.getY());
        double lonDiff = Math.toRadians(p2.getX() - p1.getX());
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(p1.getY())) * Math.cos(Math.toRadians(p2.getY())) *
                        Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private Point getCentroid(Polygon polygon) {
        Envelope2D envelope = new Envelope2D();
        polygon.queryEnvelope2D(envelope);
        return new Point((envelope.xmin + envelope.xmax) / 2, (envelope.ymin + envelope.ymax) / 2);
    }
    public void visualizeClusters(Map<String, List<String>> clusterPolygons) {
        if (clusterPolygons.isEmpty()) {
            System.out.println("No clusters to visualize. Run clustering first.");
            return;
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        for (Map.Entry<String, List<String>> entry : clusterPolygons.entrySet()) {
            String clusterName = entry.getKey();
            XYSeries series = new XYSeries(clusterName);
            for (String polygonName : entry.getValue()) {
                Point centroid = getCentroid(landmarks.get(polygonName));
                series.add(centroid.getX(), centroid.getY());
            }
            dataset.addSeries(series);
        }

        JFreeChart chart = ChartFactory.createScatterPlot(
                "Landmark Clusters Visualization",
                "Longitude (EPSG:4326)",
                "Latitude (EPSG:4326)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.GRAY);
        plot.setRangeGridlinePaint(Color.GRAY);

        JFrame frame = new JFrame("Cluster Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }
    public void visualizeClustersOnMap(Map<String, List<String>> clusterPolygons) {
        JMapViewer mapViewer = new JMapViewer();
        Random random = new Random();

        for (Map.Entry<String, List<String>> entry : clusterPolygons.entrySet()) {
            Color clusterColor = new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 150);
            for (String polygonName : entry.getValue()) {
                if (landmarks.containsKey(polygonName)) {
                    Point centroid = getCentroid(landmarks.get(polygonName));
                    Coordinate coord = new Coordinate(centroid.getY(), centroid.getX());
                    MapMarkerDot marker = new MapMarkerDot(coord);
                    marker.setBackColor(clusterColor);
                    mapViewer.addMapMarker(marker);
                }
            }
        }

        JFrame frame = new JFrame("Cluster Visualization on Map");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(mapViewer);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }
    public void savePolygonToRegionMap(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Map.Entry<String, String> entry : polygonToRegion.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
            System.out.println("polygonToRegion map saved to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static Map<String, String> loadPolygonToRegionMap(String filePath) {
        Map<String, String> polygonToRegion = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    polygonToRegion.put(parts[0].trim(), parts[1].trim());
                }
            }
            System.out.println("polygonToRegion map loaded from: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return polygonToRegion;
    }


    public Map<String, List<String>> loadClustersFromFile(String filePath) {
        Map<String, List<String>> clusterPolygons = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(", Polygons: ");
                if (parts.length == 2) {
                    String clusterName = parts[0].trim();
                    List<String> polygonNames = Arrays.stream(parts[1]
                                    .replaceAll("\\[|\\]", "")
                                    .split(", "))
                            .map(String::trim) // Trim spaces from each string
                            .collect(Collectors.toList());
                    clusterPolygons.put(clusterName, polygonNames);
                }
            }
            System.out.println("Clusters loaded from: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return clusterPolygons;
    }
    public static void main(String[] args) {
        String dataPath = "/Users/tomal/Desktop/MyWorkspace/Winter2025/Sumo_resource";
        String landmarkFilePath = dataPath + "/LA_sumo/LA_small/smallLA.poly.xml";
        XMLPolygonParser.parseXML(landmarkFilePath);
        Map<String, Polygon> landmarks = XMLPolygonParser.geometryMap;
        LandmarkRegionClusterer clusterer = new LandmarkRegionClusterer(landmarks, 50, 2);
        clusterer.clusterLandmarksAndSave("cluster_output");
        clusterer.savePolygonToRegionMap("landmark_region");
        clusterer.visualizeClustersOnMap(clusterer.loadClustersFromFile("cluster_output"));
    }
}
