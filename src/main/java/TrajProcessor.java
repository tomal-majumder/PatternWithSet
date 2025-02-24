

import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.SpatialReference;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.util.*;
//import com.esri.core.geometry.Point;
import com.esri.arcgisruntime.geometry.Point;

public class TrajProcessor {

    public static void main(String[] args) {
        String xmlFilePath = "/home/tmaju002/Research/codes/Data/TrajectoryData/trajectories.xml"; // Update this path
        Map<String, List<Point>> trajectories = parseTrajectories(xmlFilePath);

        // Example: Print trajectories
//        for (Map.Entry<String, List<Point>> entry : trajectories.entrySet()) {
//            System.out.println("ID: " + entry.getKey());
//            for (Point point : entry.getValue()) {
//                System.out.println("  " + point);
//            }
//        }
        TrajectoryAnalyzer.generateStatistics(trajectories);
    }

    public static Map<String, List<Point>> parseTrajectories(String filePath) {
        Map<String, List<Point>> trajectoryMap = new HashMap<>();
        SpatialReference targetSR = SpatialReference.create(6423); // Web Mercator

        try {
            File inputFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            NodeList timestepList = doc.getElementsByTagName("timestep");

            for (int i = 0; i < timestepList.getLength(); i++) {
                Node timestepNode = timestepList.item(i);
                if (timestepNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element timestepElement = (Element) timestepNode;
                    double time = Double.parseDouble(timestepElement.getAttribute("time"));

                    NodeList movingObjects = timestepElement.getChildNodes();
                    for (int j = 0; j < movingObjects.getLength(); j++) {
                        Node objNode = movingObjects.item(j);
                        if (objNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element objElement = (Element) objNode;
                            String id = objElement.getAttribute("id");
                            double x = Double.parseDouble(objElement.getAttribute("x"));
                            double y = Double.parseDouble(objElement.getAttribute("y"));

                            Point originalPoint = new Point(x, y, SpatialReference.create(4326));
                            Point projectedPoint = (Point) GeometryEngine.project(originalPoint, targetSR);

                            trajectoryMap.computeIfAbsent(id, k -> new ArrayList<>()).add(projectedPoint);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return trajectoryMap;
    }


}
