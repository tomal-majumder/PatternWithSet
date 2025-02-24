import com.esri.arcgisruntime.geometry.*;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.mapping.view.Graphic;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class XMLPolygonParser {
    public static Map<String, Polygon> geometryMap = new HashMap<>();
    public static Map<String, Integer> typeCount = new HashMap<>();
    //private static final String FILE_PATH = "/home/tmaju002/Research/codes/Data/LA_Landmarks/downtownLA.poly.xml";

//    public static void main(String[] args) {
//        parseXML(FILE_PATH);
//        printStats();
//        //printGeometry();
//    }

    public static void parseXML(String filePath) {
        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList polyList = doc.getElementsByTagName("poly");
            Map<String, Integer> typeCounter = new HashMap<>();

            for (int i = 0; i < polyList.getLength(); i++) {
                Node node = polyList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    String type = element.getAttribute("type");
                    String shape = element.getAttribute("shape");

                    int count = typeCounter.getOrDefault(type, 0) + 1;
                    typeCounter.put(type, count);
                    String name = type + count;

                    Polygon polygon = createPolygon(shape);
                    geometryMap.put(name, polygon);
                    typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Polygon createPolygon(String shape) {
        // Create a PointCollection with WGS84 spatial reference
        // Initialize PointCollection with WGS84 SpatialReference

        SpatialReference sourceSR = SpatialReference.create(4326); // WGS84
        SpatialReference targetSR = SpatialReference.create(6423); // Web Mercator
        PointCollection points = new PointCollection(targetSR);
        // Split the input string into coordinate pairs
        String[] pointPairs = shape.split(" ");

        // Iterate over each coordinate pair
        for (String pair : pointPairs) {
            String[] coords = pair.split(",");
            if (coords.length == 2) {
                try {
                    // Parse the coordinates
                    double x = Double.parseDouble(coords[0]);
                    double y = Double.parseDouble(coords[1]);
                    Point originalPoint = new Point(x, y, SpatialReference.create(4326)); // Longitude, Latitude
                    Point projectedPoint = (Point) GeometryEngine.project(originalPoint, targetSR);

                    // Add the point to the collection

                    points.add(projectedPoint);

                } catch (NumberFormatException e) {
                    // Handle the case where parsing fails
                    System.err.println("Invalid coordinate format: " + pair);
                }
            }
        }

        // Create the polygon from the point collection
        Polygon newPolygon = new Polygon(points);

        // Verify the spatial reference
//        if (newPolygon.getSpatialReference() == null) {
//            System.err.println("Polygon spatial reference is null.");
//        } else {
//            System.out.println("Polygon created with spatial reference: " +
//                    newPolygon.getSpatialReference().getWKText());
//        }

        return newPolygon;
    }

    public static void printStats() {
        System.out.println("Total Polygons Loaded: " + geometryMap.size());
        System.out.println("Polygon Count by Type:");
        for (Map.Entry<String, Integer> entry : typeCount.entrySet()) {
            System.out.println("Type: " + entry.getKey() + " -> " + entry.getValue());
        }
    }

    private static void printGeometry() {

        for (Map.Entry<String, Polygon> entry : geometryMap.entrySet()) {
            System.out.println("Name: " + entry.getKey() + " -> " + entry.getValue());
        }
    }


//    @Override
//    public void start(Stage primaryStage) {
//        ArcGISMap map = new ArcGISMap(Basemap.createStreets());
//        MapView mapView = new MapView();
//        mapView.setMap(map);
//
//        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
//        mapView.getGraphicsOverlays().add(graphicsOverlay);
//
//        SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, 0x8800FF00,
//                new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF000000, 1));
//
//        for (Map.Entry<String, Polygon> entry : geometryMap.entrySet()) {
//            Polygon polygon = entry.getValue();
//            com.esri.arcgisruntime.geometry.Polygon arcgisPolygon = new com.esri.arcgisruntime.geometry.Polygon(
//                    polygon, SpatialReferences.getWgs84());
//            Graphic polygonGraphic = new Graphic(arcgisPolygon, fillSymbol);
//            graphicsOverlay.getGraphics().add(polygonGraphic);
//        }
//
//        StackPane stackPane = new StackPane();
//        stackPane.getChildren().add(mapView);
//        Scene scene = new Scene(stackPane, 800, 600);
//        primaryStage.setScene(scene);
//        primaryStage.setTitle("Polygon Viewer");
//        primaryStage.show();
//    }
}
