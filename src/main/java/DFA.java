

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DFA {
    private int id; // Unique ID for each DFA
    private int startState; // Start state ID
    private Set<Integer> acceptStates; // Set of accepting state IDs
    //private Map<Integer, Map<String, Set<Integer>>> transitions; // Transition table
    public Map<Integer, Map<Set<String>, Set<Integer>>> transitions; // Transition table
    int numberOfSymbols;
    Set<String> allSymbolSet = new HashSet<>();

    public Map<Integer, HashSet<Integer>> acceptedNFAIDMap;

    public DFA(Set<String> allSymbolSet) {
        //this.id = AutomatonIDGenerator.generateAutomatonID(); // Unique ID for this DFA
        this.startState = StateIDGenerator.generateStateID(); // Get unique ID from StateIDGenerator
        this.acceptStates = new HashSet<>();
        this.transitions = new HashMap<>();
        this.acceptedNFAIDMap = new HashMap<>();
        this.numberOfSymbols = allSymbolSet.size();
        this.allSymbolSet.addAll(allSymbolSet);
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
    }

    public int getId() {
        return id;
    }
    // Clone method
    @Override
    public DFA clone() {
        DFA clonedDFA = new DFA(this.allSymbolSet); // Create a new DFA with copied symbols
        clonedDFA.id = this.id;
        clonedDFA.startState = this.startState;
        clonedDFA.numberOfSymbols = this.numberOfSymbols;

        // Deep copy acceptStates
        clonedDFA.acceptStates = new HashSet<>(this.acceptStates);

        // Deep copy transitions
        clonedDFA.transitions = new HashMap<>();
        for (Map.Entry<Integer, Map<Set<String>, Set<Integer>>> entry : this.transitions.entrySet()) {
            Map<Set<String>, Set<Integer>> newInnerMap = new HashMap<>();
            for (Map.Entry<Set<String>, Set<Integer>> innerEntry : entry.getValue().entrySet()) {
                newInnerMap.put(new HashSet<>(innerEntry.getKey()), new HashSet<>(innerEntry.getValue()));
            }
            clonedDFA.transitions.put(entry.getKey(), newInnerMap);
        }

        // Deep copy acceptedNFAIDMap
        clonedDFA.acceptedNFAIDMap = new HashMap<>();
        for (Map.Entry<Integer, HashSet<Integer>> entry : this.acceptedNFAIDMap.entrySet()) {
            clonedDFA.acceptedNFAIDMap.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        return clonedDFA;
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
//    public void addTransition(int fromState, Set<String> symbol, int toState) {
////        transitions
////                .computeIfAbsent(fromState, k -> new HashMap<>())
////                .put(symbol, toState);
//        transitions
//                .computeIfAbsent(fromState, k -> new HashMap<>())
//                .computeIfAbsent(Collections.singleton(symbol), k -> new HashSet<>())
//                .add(toState);
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
    // Method to calculate the total number of states
    public int totalNumberOfStates() {
        Set<Integer> allStates = new HashSet<>();
        // Add start state
        allStates.add(startState);

        // Add all fromState and toState
        for (Map.Entry<Integer, Map<Set<String>, Set<Integer>>> entry : transitions.entrySet()) {
            allStates.add(entry.getKey());
            for (Set<Integer> toStates : entry.getValue().values()) {
                allStates.addAll(toStates);
            }
        }

        return allStates.size();
    }

    // Method to calculate the total number of transitions
    public int totalNumberOfTransitions() {
        int count = 0;
        for (Map<Set<String>, Set<Integer>> symbolMap : transitions.values()) {
            for (Set<Integer> toStates : symbolMap.values()) {
                count += toStates.size();
            }
        }
        return count;
    }
//    public void setAcceptedIDs(){
//        for (Integer element : acceptStates) {
//            acceptedNFAIDMap.put(element, id);
//        }
//    }

    // Get the total number of states in the DFA
//    public int getTotalNumberOfStates() {
//        Set<Integer> visited = new HashSet<>();
//        Queue<Integer> queue = new LinkedList<>();
//        queue.add(startState);
//        visited.add(startState);
//
//        while (!queue.isEmpty()) {
//            int current = queue.poll();
//            for (Integer nextState : transitions.getOrDefault(current, new HashMap<>()).values()) {
//                if (visited.add(nextState)) {
//                    queue.add(nextState);
//                }
//            }
//        }
//        return visited.size();
//    }

    // Get the total number of transitions in the DFA
//    public int getTotalNumberOfTransitions() {
//        int totalTransitions = 0;
//        Set<Integer> visited = new HashSet<>();
//        Queue<Integer> queue = new LinkedList<>();
//        queue.add(startState);
//        visited.add(startState);
//
//        while (!queue.isEmpty()) {
//            int current = queue.poll();
//            Map<String, Integer> stateTransitions = transitions.getOrDefault(current, new HashMap<>());
//
//            // Count transitions for the current state
//            totalTransitions += stateTransitions.size();
//
//            // Add unvisited next states to the queue
//            for (Integer nextState : stateTransitions.values()) {
//                if (visited.add(nextState)) {
//                    queue.add(nextState);
//                }
//            }
//        }
//        return totalTransitions;
//    }

    public void setID(int id) {
        this.id = id;
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

public void minimizeDFA() {
    // Step 1: Collect all states
    Set<Integer> allStates = new HashSet<>();
    allStates.add(startState);
    for (Map.Entry<Integer, Map<Set<String>, Set<Integer>>> entry : transitions.entrySet()) {
        allStates.add(entry.getKey());
        for (Set<Integer> toStates : entry.getValue().values()) {
            allStates.addAll(toStates);
        }
    }

    // Step 2: Initial partition: accepting and non-accepting states
    Set<Integer> nonAcceptStates = new HashSet<>(allStates);
    nonAcceptStates.removeAll(acceptStates);
    Set<Set<Integer>> partition = new HashSet<>();
    partition.add(acceptStates);
    if (!nonAcceptStates.isEmpty()) {
        partition.add(nonAcceptStates);
    }

    // Step 3: Minimize using partition refinement
    Queue<Set<Integer>> workQueue = new LinkedList<>(partition);

    while (!workQueue.isEmpty()) {
        Set<Integer> current = workQueue.poll(); // Current partition to refine

        // Iterate over all symbol sets
        for (Set<String> symbolSet : getAllSymbolSets()) {
            Set<Integer> X = new HashSet<>();

            // Collect states that transition on this symbol set to a state in `current`
            for (int state : allStates) {
                Map<Set<String>, Set<Integer>> stateTransitions = transitions.getOrDefault(state, new HashMap<>());
                for (Map.Entry<Set<String>, Set<Integer>> entry : stateTransitions.entrySet()) {
                    if (!Collections.disjoint(entry.getKey(), symbolSet) && !Collections.disjoint(entry.getValue(), current)) {
                        X.add(state);
                        break;
                    }
                }
            }

            // Refine partitions based on X
            Set<Set<Integer>> newPartition = new HashSet<>();
            for (Set<Integer> Y : partition) {
                Set<Integer> intersection = new HashSet<>(Y);
                intersection.retainAll(X);
                Set<Integer> difference = new HashSet<>(Y);
                difference.removeAll(X);

                if (!intersection.isEmpty() && !difference.isEmpty()) {
                    newPartition.add(intersection);
                    newPartition.add(difference);
                    workQueue.add(intersection);
                    workQueue.add(difference);
                } else {
                    newPartition.add(Y);
                }
            }
            partition = newPartition;
        }
    }

    // Step 4: Create new minimized DFA
    Map<Integer, Integer> stateMapping = new HashMap<>();
    int newStateID = StateIDGenerator.generateStateID();
    for (Set<Integer> block : partition) {
        //int representative = block.iterator().next();
        for (int state : block) {
            stateMapping.put(state, newStateID);
        }
        newStateID = StateIDGenerator.generateStateID();
    }

    // Step 5: Update transitions and accepting states
    Map<Integer, Map<Set<String>, Set<Integer>>> newTransitions = new HashMap<>();
    for (Map.Entry<Integer, Map<Set<String>, Set<Integer>>> entry : transitions.entrySet()) {
        int fromState = stateMapping.get(entry.getKey());
        for (Map.Entry<Set<String>, Set<Integer>> transEntry : entry.getValue().entrySet()) {
            Set<String> symbolSet = transEntry.getKey();
            Set<Integer> toStates = transEntry.getValue();

            // Map each target state in the old DFA to its new state
            Set<Integer> newToStates = new HashSet<>();
            for (int toState : toStates) {
                newToStates.add(stateMapping.get(toState));
            }
//            if(fromState == 58){
//                System.out.println("Lemme know");
//            }
//            newTransitions
//                    .computeIfAbsent(fromState, k -> new HashMap<>())
//                    .computeIfAbsent(symbolSet, k -> new HashSet<>())
//                    .addAll(newToStates);

            Map<Set<String>, Set<Integer>> trans = newTransitions.computeIfAbsent(fromState, k -> new HashMap<>());
            for(int tostate: newToStates){
                boolean transitionUpdated = false;

                // Check if the target state `toState` already exists in the map
                for (Map.Entry<Set<String>, Set<Integer>> e : trans.entrySet()) {
                    Set<String> s1 = e.getKey();
                    Set<Integer> t1 = e.getValue();

                    // If the target state already exists, update the symbol set
                    if (t1.contains(tostate)) {
                        s1.addAll(symbolSet); // Add the symbol to the existing set
                        transitionUpdated = true;
                        break;
                    }

                    if(s1.contains(symbolSet)){
                        t1.add(tostate);
                        transitionUpdated = true;
                        break;
                    }
                }
                if(!transitionUpdated){
                    Set<String> newSymbolSet = new HashSet<>();
                    newSymbolSet.addAll(symbolSet);
                    trans.put(newSymbolSet, new HashSet<>(Collections.singleton(tostate)));
                }

            }
            /*
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

         */
        }
    }

    // Update accepting states
    Set<Integer> newAcceptStates = new HashSet<>();
    for (int acceptState : acceptStates) {
        newAcceptStates.add(stateMapping.get(acceptState));
    }

    // Update DFA with minimized data
    this.startState = stateMapping.get(startState);
    this.transitions = newTransitions;
    this.acceptStates = newAcceptStates;
    //        //update the map
        acceptedNFAIDMap.clear();
        for (Integer element : newAcceptStates) {
            acceptedNFAIDMap.computeIfAbsent(element, k -> new HashSet<>()).add(id);
        }
}
    private Set<Set<String>> getAllSymbolSets() {
        Set<Set<String>> allSymbolSets = new HashSet<>();
        for (Map<Set<String>, Set<Integer>> stateTransitions : transitions.values()) {
            allSymbolSets.addAll(stateTransitions.keySet()); // Add all symbol sets from each state's transitions
        }
        return allSymbolSets;
    }
    public void optimizeTransitions() {
        int threshold = (int) (allSymbolSet.size() * 0.5); // 90% threshold
        Map<Integer, Map<Set<String>, Set<Integer>>> optimizedTransitions = new HashMap<>();

        for (Map.Entry<Integer, Map<Set<String>, Set<Integer>>> stateEntry : transitions.entrySet()) {
            int fromState = stateEntry.getKey();
            Map<Set<String>, Set<Integer>> stateTransitions = stateEntry.getValue();
            Map<Set<String>, Set<Integer>> newStateTransitions = new HashMap<>();

            for (Map.Entry<Set<String>, Set<Integer>> transitionEntry : stateTransitions.entrySet()) {
                Set<String> symbolSet = transitionEntry.getKey();
                Set<Integer> toStates = transitionEntry.getValue();

                if (symbolSet.size() >= threshold) { // If it exceeds the threshold
                    Set<String> remainingSymbols = new HashSet<>(allSymbolSet);
                    remainingSymbols.removeAll(symbolSet); // Find missing symbols

                    Set<String> negatedSet = new HashSet<>();
                    negatedSet.add("ALL_BUT_" + String.join(",", remainingSymbols)); // Store as negation

                    newStateTransitions.put(negatedSet, toStates);
                } else {
                    newStateTransitions.put(symbolSet, toStates);
                }
            }
            optimizedTransitions.put(fromState, newStateTransitions);
        }

        // Update DFA transitions with optimized ones
        transitions = optimizedTransitions;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Set<String> symbolSet = new HashSet<>();
        int type = 1;
        int numberOfSymbols = 5;
        if(type == 1){
            // Generate strings r1, r2, ..., rN and add to allSymbolSet
            for (int i = 1; i <= numberOfSymbols; i++) {
                symbolSet.add("r" + i);
            }
        }
        else{
            for (int i = 1; i <= numberOfSymbols; i++) {
                symbolSet.add("R" + i);
            }
        }
        RegexToNFA converter = new RegexToNFA(symbolSet);
        NFA nfa1 = converter.convertToNFA("r1.?*.r2.?*.r3");
        DFA dfa = converter.convertToDFA(nfa1);
        dfa.minimizeDFA();
        dfa.optimizeTransitions();
        dfa.generateDiagram("dfa1_new");

    }

}