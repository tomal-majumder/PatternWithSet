import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.*;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.core.geometry.Envelope2D;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


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
                String regionName = "Region" + regionIndex++;
                List<String> polygonNames = new ArrayList<>();
                Envelope2D envelope = new Envelope2D();
                boolean first = true;
                EnvelopeBuilder envelopeBuilder = new EnvelopeBuilder(SpatialReference.create(6423));

                for (int idx : cluster) {
                    String polygonName = (String) landmarks.keySet().toArray()[idx];
                    polygonNames.add(polygonName);
                    polygonToRegion.put(polygonName, regionName);
                    Envelope2D polyEnv = new Envelope2D();
//                    landmarks.get(polygonName).queryEnvelope2D(polyEnv);
//                    if (first) { envelope.setCoords(polyEnv); first = false; }
//                    else { envelope.merge(polyEnv); }
                    Polygon polygon = landmarks.get(polygonName);

                    // Expand the envelope to include the current polygon's extent
                    envelopeBuilder.unionOf(polygon.getExtent());
                }
                // Build the combined envelope
                Envelope combinedEnvelope = envelopeBuilder.toGeometry();

                // Create a polygon representing the boundary of the combined envelope

                // Extract the spatial reference from the envelope
                SpatialReference spatialReference = combinedEnvelope.getSpatialReference();

                // Create a PointCollection to hold the corners of the envelope
                PointCollection points = new PointCollection(spatialReference);

                // Add the corners of the envelope in clockwise order
                points.add(new Point(combinedEnvelope.getXMin(), combinedEnvelope.getYMin(), spatialReference)); // Bottom-left
                points.add(new Point(combinedEnvelope.getXMax(), combinedEnvelope.getYMin(), spatialReference)); // Bottom-right
                points.add(new Point(combinedEnvelope.getXMax(), combinedEnvelope.getYMax(), spatialReference)); // Top-right
                points.add(new Point(combinedEnvelope.getXMin(), combinedEnvelope.getYMax(), spatialReference)); // Top-left
                points.add(new Point(combinedEnvelope.getXMin(), combinedEnvelope.getYMin(), spatialReference)); // Closing the polygon

                // Create and return the polygon

                Polygon mbr =  new Polygon(points);;
                //Polygon mbr = createRectangleFromEnvelope(envelope);
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
                String wkt = WKTConverter.convertToWKT(mbr);
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
                    //Geometry geometry = GeometryEngine.geometryFromWkt(wkt, 0, Geometry.Type.Polygon);
                    Geometry geometry = WKTConverter.fromWKT(wkt, SpatialReference.create(6423));
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
            if (GeometryEngine.distanceBetween(center, points.get(i)) <= eps) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }

//    private Polygon createRectangleFromEnvelope(Envelope2D envelope) {
//        Polygon polygon = new Polygon();
//        polygon.startPath(envelope.xmin, envelope.ymin);
//        polygon.lineTo(envelope.xmin, envelope.ymax);
//        polygon.lineTo(envelope.xmax, envelope.ymax);
//        polygon.lineTo(envelope.xmax, envelope.ymin);
//        polygon.closeAllPaths();
//        return polygon;
//    }

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

    public Point getCentroid(Polygon polygon) {
        // Ensure the polygon is not null
        if (polygon == null) {
            throw new IllegalArgumentException("Polygon cannot be null");
        }

        // Use GeometryEngine.labelPoint to get the centroid
        return GeometryEngine.labelPoint(polygon);
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
                    Point projectedPoint = (Point) GeometryEngine.project(centroid, SpatialReference.create(4326));

                    Coordinate coord = new Coordinate(projectedPoint.getY(), projectedPoint.getX());
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
//        ArcGISRuntimeEnvironment.setInstallDirectory("/Users/tomal/.arcgis/200.6.0/");

        // Initialize the API
        ArcGISRuntimeEnvironment.initialize();
        XMLPolygonParser.parseXML(landmarkFilePath);
        Map<String, Polygon> landmarks = XMLPolygonParser.geometryMap;
        LandmarkRegionClusterer clusterer = new LandmarkRegionClusterer(landmarks, 100, 2);
        clusterer.clusterLandmarksAndSave("cluster_output");
        clusterer.savePolygonToRegionMap("landmark_region");
        clusterer.visualizeClustersOnMap(clusterer.loadClustersFromFile("cluster_output"));
    }
}
