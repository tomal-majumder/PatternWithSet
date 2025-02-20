package main.java;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class NFA {
    private int id; // Unique ID for each NFA
    private int startState; // Start state ID
    private Set<Integer> acceptStates; // Set of accepting state IDs
    private Map<Integer, Map<Set<String>, Set<Integer>>> transitions; // Transition table
    public Map<Integer, HashSet<Integer>> acceptedNFAIDMap;

    int numberOfSymbols;
    Set<String> allSymbolSet = new HashSet<>();
    public int type;
    public NFA(Set<String> allSymbolSet) {
        this.id = AutomatonIDGenerator.generateAutomatonID(); // Unique ID for this NFA
        this.startState = StateIDGenerator.generateStateID(); // Get unique ID from StateIDGenerator
        this.acceptStates = new HashSet<>();
        this.transitions = new HashMap<>();
        this.acceptedNFAIDMap = new HashMap<>();
//        this.type = type;
        // Determine the number of symbols and populate allSymbolSet
//        if (type == 1) {
//            this.numberOfSymbols = Constants.NUMBER_OF_CELLS;
//
//            // Generate strings r1, r2, ..., rN and add to allSymbolSet
//            for (int i = 1; i <= this.numberOfSymbols; i++) {
//                this.allSymbolSet.add("r" + i);
//            }
//        } else if (type == 2) {
//            this.numberOfSymbols = Constants.NUMBER_OF_REGIONS;
//
//            // Generate strings R1, R2, ..., RN and add to allSymbolSet
//            for (int i = 1; i <= this.numberOfSymbols; i++) {
//                this.allSymbolSet.add("R" + i);
//            }
//        }
        this.numberOfSymbols = allSymbolSet.size();
        this.allSymbolSet.addAll(allSymbolSet);
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getStartState() {
        return startState;
    }
    public void setStartState(int startState) {
        this.startState = startState;
    }
    public Set<Integer> getAcceptStates() {
        return acceptStates;
    }

    public Map<Integer, Map<Set<String>, Set<Integer>>> getTransitions() {
        return transitions;
    }

    // Method to add a transition
//    public void addTransition(int fromState, String symbol, int toState) {
//        transitions
//                .computeIfAbsent(fromState, k -> new HashMap<>())
//                .computeIfAbsent(Collections.singleton(symbol), k -> new HashSet<>())
//                .add(toState);*
//    }
    public void addTransition(int fromState, String symbol, int toState) {
        // Get or create the transitions for this state
        Map<Set<String>, Set<Integer>> stateTransitions = transitions
                .computeIfAbsent(fromState, k -> new HashMap<>());

        boolean transitionUpdated = false;

        // Check if the target state `toState` already exists in the map
        for (Map.Entry<Set<String>, Set<Integer>> entry : stateTransitions.entrySet()) {
            Set<String> symbolSet = entry.getKey();
            Set<Integer> targetStates = entry.getValue();

            // If the target state already exists, update the symbol set
            if (targetStates.contains(toState)) {
                symbolSet.add(symbol); // Add the symbol to the existing set
                transitionUpdated = true;
                break;
            }

            if(symbolSet.contains(symbol)){
                targetStates.add(toState);
                transitionUpdated = true;
                break;
            }
        }

        // If no matching target state exists, create a new entry
        if (!transitionUpdated) {
            Set<String> newSymbolSet = new HashSet<>();
            newSymbolSet.add(symbol);
            stateTransitions.put(newSymbolSet, new HashSet<>(Collections.singleton(toState)));
        }
    }


    // Mark a state as accepting
    public void markAsAccepting(int stateID) {
        acceptStates.add(stateID);
    }

    public void setAcceptedIDs(){
        for (Integer element : acceptStates) {
            acceptedNFAIDMap.computeIfAbsent(element, k -> new HashSet<>()).add(id);
        }
    }
    // Method to add all accepted NFA states from another NFA
    public void addAllAcceptedStatesFromOtherNFA(NFA otherNFA) {
        // Iterate over the accepted states in the other NFA's acceptedNFAIDMap
        for (Map.Entry<Integer, HashSet<Integer>> entry : otherNFA.acceptedNFAIDMap.entrySet()) {
            Integer key = entry.getKey();
            HashSet<Integer> otherAcceptedStates = entry.getValue();

            // If the key doesn't exist in the current NFA's acceptedNFAIDMap, create a new set
            this.acceptedNFAIDMap.putIfAbsent(key, new HashSet<>());

            // Use addAll() to add the states from the other NFA to the current NFA
            this.acceptedNFAIDMap.get(key).addAll(otherAcceptedStates);
        }
    }

    // Method to add all accepted NFA states from another NFA
    public void addAllAcceptedStatesFromOtherDFA(DFA otherNFA) {
        // Iterate over the accepted states in the other NFA's acceptedNFAIDMap
        for (Map.Entry<Integer, HashSet<Integer>> entry : otherNFA.acceptedNFAIDMap.entrySet()) {
            Integer key = entry.getKey();
            HashSet<Integer> otherAcceptedStates = entry.getValue();

            // If the key doesn't exist in the current NFA's acceptedNFAIDMap, create a new set
            this.acceptedNFAIDMap.putIfAbsent(key, new HashSet<>());

            // Use addAll() to add the states from the other NFA to the current NFA
            this.acceptedNFAIDMap.get(key).addAll(otherAcceptedStates);
        }
    }
    // Calculate the total number of states in the NFA
    public int getTotalNumberOfStates() {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(startState);
        visited.add(startState);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            for (Set<Integer> nextStates : transitions.getOrDefault(current, new HashMap<>()).values()) {
                for (int nextState : nextStates) {
                    if (visited.add(nextState)) {
                        queue.add(nextState);
                    }
                }
            }
        }
        return visited.size();
    }

    // Move to next states based on a given symbol
//    public Set<Integer> move(int stateID, Set<String> symbol) {
//        return new HashSet<>(transitions.getOrDefault(stateID, new HashMap<>()).getOrDefault(symbol, Collections.emptySet()));
//    }
    public Set<Integer> move(int stateID, String symbol) {
        // Get the transitions for this state
        Map<Set<String>, Set<Integer>> stateTransitions = transitions.getOrDefault(stateID, new HashMap<>());
        Set<Integer> resultSet = new HashSet<>();
        // Check each key (set of symbols) for the presence of the given symbol
        for (Map.Entry<Set<String>, Set<Integer>> entry : stateTransitions.entrySet()) {
            Set<String> symbolSet = entry.getKey();

            if (symbolSet.contains("ANY") || symbolSet.contains(symbol)) {
                // Return the states reachable by this symbol
                resultSet.addAll(entry.getValue());
            }
        }
        return resultSet;

        // If no transition is found, return an empty set
//        return Collections.emptySet();
    }

    // Generate a diagram of the NFA in DOT format
    public void generateDiagram(String filename) throws IOException, InterruptedException {
        StringBuilder dot = new StringBuilder();
        dot.append("digraph NFA {\n");
        dot.append("rankdir=LR;\n");
        dot.append("size=\"8,5\"\n");
        dot.append("node [shape = doublecircle]; ");

        for (int acceptState : acceptStates) {
            dot.append(acceptState).append(" ");
        }
        dot.append(";\n");
        dot.append("node [shape = circle];\n");

        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(startState);
        visited.add(startState);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            Map<Set<String>, Set<Integer>> stateTransitions = transitions.getOrDefault(current, new HashMap<>());

            for (Map.Entry<Set<String>, Set<Integer>> entry : stateTransitions.entrySet()) {
                Set<String> symbol = entry.getKey();
                for (int nextState : entry.getValue()) {
                    dot.append(current)
                            .append(" -> ")
                            .append(nextState)
                            .append(" [label=\"");
//                                    .append(symbol)
//                                    .append("\"];\n");
                    dot.append(String.join(",", symbol)).append("\"];\n");
                    if (visited.add(nextState)) {
                        queue.add(nextState);
                    }
                }
            }
        }
        dot.append("}");

        // Write the DOT file
        String dotFilename = filename + ".dot";
        try (FileWriter fileWriter = new FileWriter(dotFilename)) {
            fileWriter.write(dot.toString());
        }

        // Convert DOT to PNG using graphviz
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFilename, "-o", filename + ".png");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
    }
}
