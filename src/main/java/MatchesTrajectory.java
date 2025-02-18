import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;

import java.util.*;

public class MatchesTrajectory {
    public Pair<Integer, List<RegionMatchResult>> matches(List<Point> trajectory, DFA dfa, Map<String, Polygon> symbolGeometry, double thresholdDistanceMeters){
        List<Integer> currentStates = new ArrayList<>();
        List<List<Point>> currentStatePartialMatches = new ArrayList<>();
        List<RegionMatchResult> resultSet = new ArrayList<>();
        int conditionCount = 0;
        currentStates.add(dfa.getStartState());
        currentStatePartialMatches.add(new ArrayList<>());
        Map<Integer, Map<Set<String>, Set<Integer>>> transitions = dfa.transitions;
        for(Point point: trajectory){
            Set<Integer> uniqueNextStates = new HashSet<>();
            Map<Integer, List<Point>> stateToPartialMatch = new HashMap<>();
            for(int i = 0; i < currentStates.size(); i++){
                int currentState = currentStates.get(i);
                List<Point> match = new ArrayList<>(currentStatePartialMatches.get(i));
                //match.add(point);
                if(transitions.containsKey(currentState)){
                    Map<Set<String>, Set<Integer>> symbolAndTo = transitions.get(currentState);
                    String nearestLandmark = null;
                    double minDistanceMeters = Double.MAX_VALUE;
                    for(Map.Entry<Set<String>, Set<Integer>> entry : symbolAndTo.entrySet()){
                        Set<String> key = entry.getKey();
                        Set<Integer> value = entry.getValue();
                        //go through each symbol of this set
                        for(String symbol: key){
                            //get the geometry of the symbol
                            Polygon symbolPolygon = symbolGeometry.get(symbol);
                            //now check if point is near the polygon with some threshold.
                            //point to polygon distance
                            double distanceMeters = calculateDistanceToPolygon(point, symbolPolygon);
                            conditionCount++;
                            if (distanceMeters <= thresholdDistanceMeters && distanceMeters < minDistanceMeters) {
                                minDistanceMeters = distanceMeters;
                                nearestLandmark = symbol;
                            }
                        }
                    }
                    if(nearestLandmark != null){
                        for(Map.Entry<Set<String>, Set<Integer>> entry : symbolAndTo.entrySet()) {
                            Set<String> key = entry.getKey();
                            Set<Integer> value = entry.getValue();
                            if(key.contains(nearestLandmark)){
                              match.add(point);
                              uniqueNextStates.addAll(value);
                            }
                        }
                    }
                }
            }
            if(uniqueNextStates.isEmpty()){
                currentStates.clear();
                currentStatePartialMatches.clear();
                currentStates.add(dfa.getStartState());
                currentStatePartialMatches.add(new ArrayList<>());
            }
            else{
                currentStates = new ArrayList<>(uniqueNextStates);
                currentStatePartialMatches.add(new ArrayList<>());
                for(int i = 0; i < currentStates.size(); i++){
                    int state = currentStates.get(i);
                    if(dfa.getAcceptStates().contains(state)){
                        resultSet.add(new RegionMatchResult(currentStatePartialMatches.get(i), dfa.acceptedNFAIDMap.get(state)));
                    }
                }
            }
            // Add start state again for possible subsequent matches
            currentStates.add(dfa.getStartState());
            currentStatePartialMatches.add(new ArrayList<>());
        }
        return new Pair<>(conditionCount, resultSet);

    }
    static class RegionMatchResult{
        List<Point> trajectory;
        HashSet<Integer> queryIDs;

        public RegionMatchResult(List<Point> trajectory, HashSet<Integer> queryIDs){
            this.trajectory = new ArrayList<>(trajectory);

            // Create a new set to store a copy of the queryIDs
            this.queryIDs = new HashSet<>(queryIDs);
        }
    }
    private double calculateDistanceToPolygon(Point point, Polygon polygon) {
        double minDistanceMeters = Double.MAX_VALUE;

        for (int i = 0; i < polygon.getPathCount(); i++) {
            for (int j = 0; j < polygon.getPointCount(); j++) {
                Point polygonPoint = polygon.getPoint(j);
                double distance = haversineDistance(point.getY(), point.getX(), polygonPoint.getY(), polygonPoint.getX());

                if (distance < minDistanceMeters) {
                    minDistanceMeters = distance;
                }
            }
        }

        return minDistanceMeters;
    }

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

}
