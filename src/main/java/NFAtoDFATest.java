import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;

import java.io.IOException;
import java.util.*;

public class NFAtoDFATest {
    public static void main(String[] args) throws IOException, InterruptedException {
        String dataPath = "/Users/tomal/Desktop/MyWorkspace/Winter2025/Sumo_resource/LA_sumo/LA_small/";
        String trajectoryFilePath = dataPath + "trajectories.xml";
        String landmarkFilePath = dataPath + "smallLA.poly.xml";
        //get trajectory map
        Map<String, List<Point>> trajectories= TrajProcessor.parseTrajectories(trajectoryFilePath);
        TrajectoryAnalyzer.generateStatistics(trajectories);
        XMLPolygonParser.parseXML(landmarkFilePath);
        Map<String, Polygon> landmarks = XMLPolygonParser.geometryMap;
        XMLPolygonParser.printStats();

        int numQueries = 1000;
        RegexQueryGenerator queryGenerator = new RegexQueryGenerator(landmarks, trajectories, 100);
        //List<String> queries = queryGenerator.generateQueries(numQueries);
        //queryGenerator.saveQueriesToFile(queries, "queries/queries" + numQueries + ".txt");
        //if saved once
        List<String> queries = queryGenerator.readQueriesFromFile("queries/queries" + numQueries + ".txt");
        Map<String, String> polygonToRegion = LandmarkRegionClusterer.loadPolygonToRegionMap("landmark_region");
        Map<String, Polygon> regionMBR = LandmarkRegionClusterer.loadRegionMBRs("region_info");
        //each query is basically a regexp, build DFA from these regexps
        List<DFA> queryOriginalDFAs = new ArrayList<>();
        List<DFA> queryOriginalDFAsWithMinimizedTransitions = new ArrayList<>();

        Map<Integer, DFA> originalDFAMap = new HashMap<>();
        Map<Integer, DFA> originalDFAMapWithMinimizedTransition = new HashMap<>();

        Set<String> allSymbolSet = new HashSet<>(landmarks.keySet());
        for (Map.Entry<String, Polygon> entry : landmarks.entrySet()) {
            String landmark = entry.getKey();
            if (!polygonToRegion.containsKey(landmark)) {
                // Add the landmark as its own region
                String newRegionName = "Region" + landmark;
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
            //dfa.optimizeTransitions();
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
            //regionDFA.optimizeTransitions();
            if(regionDFA.getId() == 10 || regionDFA.getId() == 76){
                regionDFA.generateDiagram("regionDFA" + regionDFA.getId());
            }
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
        mergedDFA.optimizeTransitions();
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
//        mergedNFA.generateDiagram("Merged_NFA");
        DFA mergedRegionDFA = regionRegexToNFA.convertToDFA(mergedRegionNFA);
        mergedRegionDFA.optimizeTransitions();
        mergedRegionDFA.generateDiagram("merged_region_dfa");
        System.out.println("Number of states in merged Region DFAs: " + mergedRegionDFA.totalNumberOfStates());

    }
}
