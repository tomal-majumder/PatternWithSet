import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;

import java.util.*;

//this is ultimate main class.
public class Main {
    public static void main(String[] args) {
        String dataPath = "/Users/tomal/Desktop/MyWorkspace/Winter2025/Sumo_resource";
        String trajectoryFilePath = dataPath + "/LA_sumo/trajectories.xml";
        String landmarkFilePath = dataPath + "/LA_sumo/downtownLA.poly.xml";
        //get trajectory map

        Map<String, List<Point>> trajectories= TrajProcessor.parseTrajectories(trajectoryFilePath);
        //get landmark map
        XMLPolygonParser.parseXML(landmarkFilePath);
        Map<String, Polygon> landmarks = XMLPolygonParser.geometryMap;
        int numQueries = 100;
        RegexQueryGenerator queryGenerator = new RegexQueryGenerator(landmarks, trajectories, 100);
//        List<String> queries = queryGenerator.generateQueries(numQueries);
//        queryGenerator.saveQueriesToFile(queries, "queries" + numQueries + ".txt");
        //if saved once
        List<String> queries = queryGenerator.readQueriesFromFile("queries" + numQueries + ".txt");
        Map<String, String> polygonToRegion = LandmarkRegionClusterer.loadPolygonToRegionMap("landmark_region");
        Map<String, Polygon> regionMBR = LandmarkRegionClusterer.loadRegionMBRs("region_info");
        //each query is basically a regexp, build DFA from these regexps
        List<DFA> queryOriginalDFAs = new ArrayList<>();
        Map<Integer, DFA> originalDFAMap = new HashMap<>();
        Set<String> allSymbolSet = new HashSet<>(landmarks.keySet());
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
            //regionNFA.setID(id);
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
        System.out.println(mergedDFA.totalNumberOfStates());

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
        System.out.println(mergedRegionDFA.totalNumberOfStates());

        //Now matching with merged original DFAs first
        for(Map.Entry<String, List<Point>> entry : trajectories.entrySet()) {
            String objectId = entry.getKey();
            List<Point> points = entry.getValue();

        }
        //Now matching with merged region DFAs
    }
}
