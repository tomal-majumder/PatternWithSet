import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;

import java.util.List;
import java.util.Map;

//this is ultimate main class.
public class Main {
    public static void main(String[] args) {
        String trajectoryFilePath = "/home/tmaju002/Research/codes/Data/TrajectoryData/trajectories.xml";
        String landmarkFilePath = "/home/tmaju002/Research/codes/Data/LA_Landmarks/downtownLA.poly.xml";
        //get trajectory map

        Map<String, List<Point>> trajectories= TrajProcessor.parseTrajectories(trajectoryFilePath);
        //get landmark map
        XMLPolygonParser.parseXML(landmarkFilePath);
        Map<String, Polygon> landmarks = XMLPolygonParser.geometryMap;
        int numQueries = 100;
        RegexQueryGenerator queryGenerator = new RegexQueryGenerator(landmarks, trajectories, 100);
        List<String> queries = queryGenerator.generateQueries(numQueries);
        queryGenerator.saveQueriesToFile(queries, "queries" + numQueries + ".txt");


    }
}
