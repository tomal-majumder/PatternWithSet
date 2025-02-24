

import java.io.IOException;
import java.util.*;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;

public class TestMatching {

    public static void main(String[] args) throws IOException, InterruptedException {
        // Define complex trajectories
        List<List<Point>> trajectories = List.of(
                List.of( // Trajectory 1: A -> C -> F -> I
                        new Point(-118.271201, 34.051288),
                        new Point(-118.268926, 34.051288),
                        new Point(-118.268926, 34.053300),
                        new Point(-118.265000, 34.054500)
                ),
                List.of( // Trajectory 2: B -> D -> H -> J
                        new Point(-118.271300, 34.051500),
                        new Point(-118.268926, 34.051288),
                        new Point(-118.260000, 34.055000),
                        new Point(-118.258000, 34.056000)
                ),
                List.of( // Trajectory 3: A -> D -> H -> I
                        new Point(-118.271201, 34.051288),
                        new Point(-118.268926, 34.051288),
                        new Point(-118.260000, 34.055000),
                        new Point(-118.265000, 34.054500)
                ),
                List.of( // Trajectory 4: B -> C -> G -> J
                        new Point(-118.271300, 34.051500),
                        new Point(-118.268926, 34.051288),
                        new Point(-118.268926, 34.053300),
                        new Point(-118.258000, 34.056000)
                ),
                List.of( // Trajectory 5: A -> C -> G -> I
                        new Point(-118.271201, 34.051288),
                        new Point(-118.268926, 34.051288),
                        new Point(-118.268926, 34.053300),
                        new Point(-118.265000, 34.054500)
                )
        );

        // Define polygons for landmarks
        Map<String, Polygon> symbolGeometry = new HashMap<>();
//        symbolGeometry.put("A", createPolygon(-118.271201, 34.051288));
//        symbolGeometry.put("B", createPolygon(-118.271300, 34.051500));
//        symbolGeometry.put("C", createPolygon(-118.268926, 34.051288));
//        symbolGeometry.put("D", createPolygon(-118.268926, 34.051288));
//        symbolGeometry.put("E", createPolygon(-118.268000, 34.050500));
//        symbolGeometry.put("F", createPolygon(-118.268926, 34.053300));
//        symbolGeometry.put("G", createPolygon(-118.268926, 34.053300));
//        symbolGeometry.put("H", createPolygon(-118.260000, 34.055000));
//        symbolGeometry.put("I", createPolygon(-118.265000, 34.054500));
//        symbolGeometry.put("J", createPolygon(-118.258000, 34.056000));

        // Define a DFA with symbol sets as transitions
        DFA dfa = new DFA(symbolGeometry.keySet());
        dfa.setStartState(0);
        dfa.getAcceptStates().add(5);

        // Complex transitions with sets of symbols
        dfa.transitions.put(0, Map.of(Set.of("A", "B"), Set.of(1)));
        dfa.transitions.put(1, Map.of(Set.of("C"), Set.of(2), Set.of("D", "E"), Set.of(3)));
        dfa.transitions.put(2, Map.of(Set.of("F", "G"), Set.of(4)));
        dfa.transitions.put(3, Map.of(Set.of("H"), Set.of(4)));
        dfa.transitions.put(4, Map.of(Set.of("I", "J"), Set.of(5)));
        HashSet<Integer> dfaID = new HashSet<>();
        dfaID.add(dfa.getId());
        dfa.acceptedNFAIDMap.put(5, dfaID);
        //dfa.generateDiagram("dummy_dfa");
        // Run tests
        MatchesTrajectory matcher = new MatchesTrajectory();
        double thresholdDistanceMeters = 100;

        for (int i = 0; i < trajectories.size(); i++) {
            System.out.println("Testing Trajectory " + (i + 1));
            Pair<Integer, List<RegionMatchResult>> result = matcher.matches(trajectories.get(i), dfa, symbolGeometry, thresholdDistanceMeters, 1);
            System.out.println("Condition Count: " + result.getLeft());
            for (RegionMatchResult r : result.getRight()) {

                System.out.println("Path Points: ");
                r.trajectory.forEach(p -> System.out.printf("Lat: %.6f, Lon: %.6f%n", p.getY(), p.getX()));
            }
            System.out.println("------------------------------------------------");
        }
    }

//    private static Polygon createPolygon(double lon, double lat) {
//        Polygon polygon = new Polygon();
//        polygon.startPath(lon - 0.0005, lat - 0.0005);
//        polygon.lineTo(lon - 0.0005, lat + 0.0005);
//        polygon.lineTo(lon + 0.0005, lat + 0.0005);
//        polygon.lineTo(lon + 0.0005, lat - 0.0005);
//        polygon.closeAllPaths();
//        return polygon;
//    }
}
