import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

//this is ultimate main class.
public class Main {
    public static void main(String[] args) throws IOException {
        String dataPath = "/Users/tomal/Desktop/MyWorkspace/Winter2025/Sumo_resource";
        String trajectoryFilePath = dataPath + "/LA_sumo/LA_small/trajectories.xml";
        String landmarkFilePath = dataPath + "/LA_sumo/LA_small/smallLA.poly.xml";
        //get trajectory map

        Map<String, List<Point>> trajectories= TrajProcessor.parseTrajectories(trajectoryFilePath);
        TrajectoryAnalyzer.generateStatistics(trajectories);
        //get landmark map
        XMLPolygonParser.parseXML(landmarkFilePath);
        Map<String, Polygon> landmarks = XMLPolygonParser.geometryMap;
        XMLPolygonParser.printStats();

        int numQueries = 10;
        RegexQueryGenerator queryGenerator = new RegexQueryGenerator(landmarks, trajectories, 100);
        //List<String> queries = queryGenerator.generateQueries(numQueries);
        //queryGenerator.saveQueriesToFile(queries, "queries/queries" + numQueries + ".txt");
        //if saved once
        List<String> queries = queryGenerator.readQueriesFromFile("queries/queries" + numQueries + ".txt");
        Map<String, String> polygonToRegion = LandmarkRegionClusterer.loadPolygonToRegionMap("landmark_region");
        Map<String, Polygon> regionMBR = LandmarkRegionClusterer.loadRegionMBRs("region_info");
        //each query is basically a regexp, build DFA from these regexps
        List<DFA> queryOriginalDFAs = new ArrayList<>();
        Map<Integer, DFA> originalDFAMap = new HashMap<>();
        Set<String> allSymbolSet = new HashSet<>(landmarks.keySet());
        for (Map.Entry<String, Polygon> entry : landmarks.entrySet()) {
            String landmark = entry.getKey();
            if (!polygonToRegion.containsKey(landmark)) {
                // Add the landmark as its own region
                String newRegionName = "Region_" + landmark;
                polygonToRegion.put(landmark, newRegionName);
                regionMBR.put(newRegionName, entry.getValue());
                System.out.println("Added new region for landmark: " + landmark);
            }
        }
        Set<String> allRegionSymbolSet = new HashSet<>(regionMBR.keySet());

        RegexToNFA regexToNFA = new RegexToNFA(allSymbolSet);
        List<DFA> queryRegionDFAs = new ArrayList<>();
        RegexToNFA regionRegexToNFA = new RegexToNFA(allRegionSymbolSet);
        for(String query: queries){
            NFA nfa = regexToNFA.convertToNFA(query);
            DFA dfa = regexToNFA.convertToDFA(nfa);
            dfa.minimizeDFA();
            queryOriginalDFAs.add(dfa);
            originalDFAMap.put(dfa.getId(), dfa);
            String[] queryCells = query.split("\\.");
            StringBuilder regexp = new StringBuilder();
            for(String cell: queryCells){
                //int regionID = gen.getRegionIdForPoint(point);
                if(cell.equals("?*")) regexp.append(cell).append(".");
                else {
                    String region;
                    region = polygonToRegion.getOrDefault(cell, cell);
                    regexp.append(region).append(".");
                }
            }
            if (regexp.length() > 0 && regexp.charAt(regexp.length() - 1) == '.') {
                regexp.deleteCharAt(regexp.length() - 1);
            }
            NFA regionNFA = regionRegexToNFA.convertToNFA(regexp.toString());
            regionNFA.setId(dfa.getId());
            DFA regionDFA = regionRegexToNFA.convertToDFA(regionNFA);
            regionDFA.minimizeDFA();
            //queryOriginalDFAs.add(dfa);
            //originalDFAMap.put(dfa.getId(), dfa);
            queryRegionDFAs.add(regionDFA);
        }

        //Now do the merging

        //First: Prepare merged DFA on original symbol
        int originalStatesNumber = 0;
        for(int i = 0; i < queryOriginalDFAs.size(); i++){
            originalStatesNumber += queryOriginalDFAs.get(i).totalNumberOfStates();
        }
        System.out.println("Total States in original FSMs: " + originalStatesNumber);

        NFA mergedNFA = new NFA(allSymbolSet);
        int mergedStart = mergedNFA.getStartState();
        for(DFA dfa: queryOriginalDFAs){
            mergedNFA.addTransition(mergedStart,"ε", dfa.getStartState());
            mergedNFA.getTransitions().putAll(dfa.getTransitions());
            mergedNFA.getAcceptStates().addAll(dfa.getAcceptStates());
            mergedNFA.addAllAcceptedStatesFromOtherDFA(dfa);
        }
        //mergedNFA.generateDiagram("Merged_NFA");
        DFA mergedDFA = regexToNFA.convertToDFA(mergedNFA);
        System.out.println("Number of states in merged DFAs: " + mergedDFA.totalNumberOfStates());

        //Second: Prepare Merged DFA on region symbol
        NFA mergedRegionNFA = new NFA(allRegionSymbolSet);
        int mergedRegionStart = mergedRegionNFA.getStartState();
        for(DFA dfa: queryRegionDFAs){
            mergedRegionNFA.addTransition(mergedRegionStart,"ε", dfa.getStartState());
            mergedRegionNFA.getTransitions().putAll(dfa.getTransitions());
            mergedRegionNFA.getAcceptStates().addAll(dfa.getAcceptStates());
            mergedRegionNFA.addAllAcceptedStatesFromOtherDFA(dfa);
        }
        //mergedNFA.generateDiagram("Merged_NFA");
        DFA mergedRegionDFA = regionRegexToNFA.convertToDFA(mergedRegionNFA);
        System.out.println("Number of states in merged Region DFAs: " + mergedRegionDFA.totalNumberOfStates());

        MatchesTrajectory matcher = new MatchesTrajectory();

        //Now matching with separate original DFAs first
        long startTime = System.nanoTime();
        String outputFileNameSeparate = "results/"+ numQueries + "_separate_result" + ".txt";
        FileWriter writerSeparate = new FileWriter(outputFileNameSeparate);
        HashMap<Integer, Integer> separateStatMap = new HashMap<>();
        int resultCount = 0;
        int totalConditionChecks = 0;
        for(Map.Entry<String, List<Point>> entry : trajectories.entrySet()) {
            String objectId = entry.getKey();
            List<Point> points = entry.getValue();
            for(int i = 0; i < queryOriginalDFAs.size(); i++){
                DFA dfaToCheck = queryOriginalDFAs.get(i);
                Pair<Integer, List<MatchesTrajectory.RegionMatchResult>> matches = matcher.matches(points, dfaToCheck, landmarks, 100);
                totalConditionChecks += matches.getLeft();
                for(int j = 0; j < matches.getRight().size(); j++){
                    MatchesTrajectory.RegionMatchResult regionMatchResult = matches.getRight().get(j);
                    resultCount++;
                    List<Point> trajectory = regionMatchResult.trajectory;
                    writerSeparate.write(objectId + " is matched with Query id: "+ dfaToCheck.getId() + ": ");
                    for (Point point : trajectory) {
                        writerSeparate.write(point.toString());
                    }
                    writerSeparate.write(System.lineSeparator()); // Adds a new line after the output

                }
            }
        }
        long endTime = System.nanoTime();
        // Calculate the elapsed time
        long duration = endTime - startTime;
        long durationInMillis = duration / 1_000_000;
        System.out.println("Number of checks: " + totalConditionChecks);
        System.out.println("Number of matches: " + resultCount);
        System.out.println("Elapsed Time: "+ durationInMillis);
        writerSeparate.close();

        //Now matching with merged original DFAs
        String outputFileNameMerged = "results/"+ numQueries + "_merged_result" + ".txt";
        FileWriter writeMerged = new FileWriter(outputFileNameMerged);
        //HashMap<Integer, Integer> separateStatMap = new HashMap<>();
        startTime = System.nanoTime();
        resultCount = 0;
        totalConditionChecks = 0;
        for(Map.Entry<String, List<Point>> entry : trajectories.entrySet()) {
            String objectId = entry.getKey();
            List<Point> points = entry.getValue();
            Pair<Integer, List<MatchesTrajectory.RegionMatchResult>> matches = matcher.matches(points, mergedDFA, landmarks, 100);
            totalConditionChecks += matches.getLeft();
            for(int j = 0; j < matches.getRight().size(); j++){
                MatchesTrajectory.RegionMatchResult regionMatchResult = matches.getRight().get(j);
                resultCount++;
                List<Point> trajectory = regionMatchResult.trajectory;
                HashSet<Integer> queryIDs = regionMatchResult.queryIDs;
                for(Integer queryID: queryIDs){
                    writeMerged.write(objectId + " is matched with Query id: "+ queryID + ": ");
                }
                for (int k = 0; k < trajectory.size(); k++) {
                    writeMerged.write(trajectory.get(k).toString());
                }
                writeMerged.write(System.lineSeparator()); // Adds a new line after the output

            }

        }
        endTime = System.nanoTime();
        duration = endTime - startTime;
        durationInMillis = duration / 1_000_000;

        System.out.println("Number of checks: " + totalConditionChecks);
        System.out.println("Number of matches: " + resultCount);
        System.out.println("Elapsed Time: " + durationInMillis);
        writeMerged.close();

        //Now, we will see how region DFA goes:
        String outputFileNameMergedRegion = "results/"+ numQueries + "_merged_result_region" + ".txt";
        FileWriter writeMergedRegion = new FileWriter(outputFileNameMergedRegion);
        //HashMap<Integer, Integer> separateStatMap = new HashMap<>();
        startTime = System.nanoTime();
        resultCount = 0;
        totalConditionChecks = 0;
        for(Map.Entry<String, List<Point>> entry : trajectories.entrySet()) {
            String objectId = entry.getKey();
            List<Point> points = entry.getValue();
            Pair<Integer, List<MatchesTrajectory.RegionMatchResult>> matches = matcher.matches(points, mergedRegionDFA, regionMBR, 100);
            totalConditionChecks += matches.getLeft();
            for(int j = 0; j < matches.getRight().size(); j++){
                MatchesTrajectory.RegionMatchResult regionMatchResult = matches.getRight().get(j);
                List<Point> trajectory = regionMatchResult.trajectory;
                HashSet<Integer> queryIDs = regionMatchResult.queryIDs;
                for(Integer queryID: queryIDs){
                    Pair<Integer, Boolean> integerBooleanPair = matcher.matchesOriginal(trajectory, originalDFAMap.get(queryID), landmarks, 100);
                    totalConditionChecks += integerBooleanPair.getLeft();
                    if(integerBooleanPair.getRight()){
                        resultCount++;
                        writeMergedRegion.write(objectId + " is matched with Query id: "+ queryID + ": ");
                        for (int k = 0; k < trajectory.size(); k++) {
                            writeMergedRegion.write(trajectory.get(k).toString());
                        }
                        writeMergedRegion.write(System.lineSeparator()); // Adds a new line after the output
                    }
                }
            }

        }
        endTime = System.nanoTime();
        duration = endTime - startTime;
        durationInMillis = duration / 1_000_000;

        System.out.println("Number of checks: " + totalConditionChecks);
        System.out.println("Number of matches: " + resultCount);
        System.out.println("Elapsed Time: " + durationInMillis);
        writeMergedRegion.close();
    }
}
