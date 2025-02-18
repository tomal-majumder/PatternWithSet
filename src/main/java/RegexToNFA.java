import java.io.IOException;
import java.util.*;

public class RegexToNFA {
    // Converts a regular expression pattern into an NFA
    Set<String> allSymbolSet;
    RegexToNFA(Set<String> allSymbolSet){
        this.allSymbolSet = new HashSet<>();
        this.allSymbolSet.addAll(allSymbolSet);
    }
    public NFA convertToNFA(String regex) {
        Stack<NFA> operands = new Stack<>();
        Stack<Character> operators = new Stack<>();
        Stack<NFA> concatStack = new Stack<>();
        boolean concatFlag = false;
        char op, c;
        int paraCount = 0;
        NFA nfa1, nfa2;
        String cellUnit = "";
        for (int i = 0; i < regex.length(); i++) {
            c = regex.charAt(i);
            if (Character.isLetter(c) || c == '?') {
                if(c == '?'){
                    //do soemthing //look ahead
                    int idx = i + 3;
                    StringBuilder transition = new StringBuilder();
                    transition.append(regex.charAt(idx));

                    while (i + 1 < regex.length() &&
                            (Character.isLetterOrDigit(regex.charAt(i + 1)) || regex.charAt(i + 1) == '_')) {
                        idx++;
                        transition.append(regex.charAt(idx));
                    }
                    cellUnit = transition.toString();
                    //operands.push(singleTransition(cellUnit));
                    //operands.push(singleTransition("^"+cellUnit));
                    operands.push(singleTransition("ANY")); //whole set of symbols
                }
                else{
                    StringBuilder transition = new StringBuilder();
                    transition.append(c);
                    while (i + 1 < regex.length() &&
                            (Character.isLetterOrDigit(regex.charAt(i + 1)) || regex.charAt(i + 1) == '_')) {
                        i++;
                        transition.append(regex.charAt(i));
                    }
                    cellUnit = transition.toString();
                    operands.push(singleTransition(cellUnit));
                }
                // Read full transition name (e.g., r1, r2)

            } else if (c == ')') {
                concatFlag = false;
                if (paraCount == 0) {
                    throw new IllegalArgumentException("More closing parentheses than opening");
                } else {
                    paraCount--;
                }
                while (!operators.empty() && operators.peek() != '(') {
                    op = operators.pop();
                    if (op == '.') {
                        nfa2 = operands.pop();
                        nfa1 = operands.pop();
                        operands.push(concatenation(nfa1, nfa2));
                    } else if (op == '|') {
                        nfa2 = operands.pop();
                        if (!operators.empty() && operators.peek() == '.') {
                            concatStack.push(operands.pop());
                            while (!operators.empty() && operators.peek() == '.') {
                                concatStack.push(operands.pop());
                                operators.pop();
                            }
                            nfa1 = concatenation(concatStack.pop(), concatStack.pop());
                            while (!concatStack.isEmpty()) {
                                nfa1 = concatenation(nfa1, concatStack.pop());
                            }
                        } else {
                            nfa1 = operands.pop();
                        }
                        operands.push(union(nfa1, nfa2));
                    }
                }
            }

            if (c == '*') {
                operands.push(kleeneStar(operands.pop()));
                concatFlag = true;
            }
            else if (c == '+') { // **NEWLY ADDED: Handle the + operator**
                operands.push(oneOrMore(operands.pop())); // **NEWLY ADDED**
                concatFlag = true; // **NEWLY ADDED**
            }
            else if (c == '|') {
                operators.push(c);
            } else if (c == '(') {
                operators.push(c);
                paraCount++;
            } else if (c == '.') {
                operators.push(c);
                concatFlag = true;
            }
        }

        while (!operators.isEmpty()) {
            if (operands.isEmpty()) {
                throw new IllegalArgumentException("Imbalance between operands and operators");
            }
            op = operators.pop();
            if (op == '.') {
                nfa2 = operands.pop();
                nfa1 = operands.pop();
                operands.push(concatenation(nfa1, nfa2));
            } else if (op == '|') {
                nfa2 = operands.pop();
                if (!operators.empty() && operators.peek() == '.') {
                    concatStack.push(operands.pop());
                    while (!operators.isEmpty() && operators.peek() == '.') {
                        concatStack.push(operands.pop());
                        operators.pop();
                    }
                    nfa1 = concatenation(concatStack.pop(), concatStack.pop());
                    while (!concatStack.isEmpty()) {
                        nfa1 = concatenation(nfa1, concatStack.pop());
                    }
                } else {
                    nfa1 = operands.pop();
                }
                operands.push(union(nfa1, nfa2));
            }
        }
        NFA finalNFA = operands.pop();
        finalNFA.setAcceptedIDs();
        return finalNFA;
    }

    // Constructs an NFA for a single transition
    private NFA singleTransition(String transition) {
        NFA nfa = new NFA(allSymbolSet);
        int start = nfa.getStartState();
        int accept = StateIDGenerator.generateStateID();
        nfa.addTransition(start, transition, accept);
        nfa.markAsAccepting(accept);
        return nfa;
    }

    // Concatenates two NFAs
    private NFA concatenation(NFA nfa1, NFA nfa2) {
        NFA resultNFA = new NFA(allSymbolSet);
        resultNFA.getTransitions().putAll(nfa1.getTransitions());
        resultNFA.setStartState(nfa1.getStartState());
        // Add epsilon transitions from each accept state of nfa1 to the start state of nfa2
        for (int acceptState : nfa1.getAcceptStates()) {
            resultNFA.addTransition(acceptState, "ε", nfa2.getStartState());
        }

        // Add all transitions from nfa2 to the result NFA
        resultNFA.getTransitions().putAll(nfa2.getTransitions());

        // Set accept states of the result NFA to nfa2's accept states
        resultNFA.getAcceptStates().addAll(nfa2.getAcceptStates());
        return resultNFA;
    }


    // Unions two NFAs
    private NFA union(NFA nfa1, NFA nfa2) {
        NFA resultNFA = new NFA(allSymbolSet);
        int newStart = resultNFA.getStartState();
        int newAccept = StateIDGenerator.generateStateID();

        // Add epsilon transitions from the new start to the start states of nfa1 and nfa2
        resultNFA.addTransition(newStart, "ε", nfa1.getStartState());
        resultNFA.addTransition(newStart, "ε", nfa2.getStartState());

        // Add all transitions from nfa1 and nfa2 to resultNFA
        resultNFA.getTransitions().putAll(nfa1.getTransitions());
        resultNFA.getTransitions().putAll(nfa2.getTransitions());

        // Add epsilon transitions from all accept states of nfa1 and nfa2 to the new accept state
        for (int acceptState : nfa1.getAcceptStates()) {
            resultNFA.addTransition(acceptState, "ε", newAccept);
        }
        for (int acceptState : nfa2.getAcceptStates()) {
            resultNFA.addTransition(acceptState, "ε", newAccept);
        }

        // Set the new accept state as the accept state for resultNFA
        resultNFA.markAsAccepting(newAccept);
        return resultNFA;
    }


    // Applies the Kleene star operation to an NFA
    private NFA kleeneStar(NFA nfa) {
        NFA starNFA = new NFA(allSymbolSet);
        int newStart = starNFA.getStartState();
        int newAccept = StateIDGenerator.generateStateID();

        // Add epsilon transition from new start state to nfa's start state and to new accept state
        starNFA.addTransition(newStart, "ε", nfa.getStartState());
        starNFA.addTransition(newStart, "ε", newAccept);

        // Copy all transitions from nfa to starNFA
        starNFA.getTransitions().putAll(nfa.getTransitions());

        // Add epsilon transitions from all accept states of nfa to its start state and to new accept state
        for (int acceptState : nfa.getAcceptStates()) {
            starNFA.addTransition(acceptState, "ε", nfa.getStartState());
            starNFA.addTransition(acceptState, "ε", newAccept);
        }

        // Set the new accept state as the accept state for starNFA
        starNFA.markAsAccepting(newAccept);
        return starNFA;
    }
    // **NEWLY ADDED: Applies the + operator (one or more occurrences) to an NFA**
    private NFA oneOrMore(NFA nfa) {
        NFA plusNFA = new NFA(allSymbolSet);
        int newStart = plusNFA.getStartState();
        int newAccept = StateIDGenerator.generateStateID();

        // Add epsilon transition from the new start state to the NFA's start state
        plusNFA.addTransition(newStart, "ε", nfa.getStartState());

        // Copy all transitions from the original NFA
        plusNFA.getTransitions().putAll(nfa.getTransitions());

        // Add epsilon transitions from all accept states of the NFA back to its start state
        for (int acceptState : nfa.getAcceptStates()) {
            plusNFA.addTransition(acceptState, "ε", nfa.getStartState());
            plusNFA.addTransition(acceptState, "ε", newAccept);
        }

        // Mark the new accept state as the accept state for plusNFA
        plusNFA.markAsAccepting(newAccept);

        return plusNFA;
    }

    // Converts an NFA to a DFA
    public DFA convertToDFA(NFA nfa) {
        Map<Set<Integer>, Integer> dfaStates = new HashMap<>();
        Queue<Set<Integer>> unprocessedStates = new LinkedList<>();
        Set<String> allSet = nfa.allSymbolSet;
        Set<Integer> startStateSet = epsilonClosure(Collections.singleton(nfa.getStartState()), nfa);
        int startStateID = StateIDGenerator.generateStateID();
        dfaStates.put(startStateSet, startStateID);
        unprocessedStates.add(startStateSet);

        DFA dfa = new DFA(nfa.allSymbolSet);
        dfa.setStartState(startStateID);
        dfa.setID(nfa.getId());

        while (!unprocessedStates.isEmpty()) {
            Set<Integer> currentSet = unprocessedStates.poll();
            int currentStateID = dfaStates.get(currentSet);

            for (String symbol : getNonEpsilonLabels(currentSet, nfa)) {
                Set<Integer> nextStateSet = new HashSet<>();
                for (int nfaState : currentSet) {
                    nextStateSet.addAll(nfa.move(nfaState, symbol));
                }
                nextStateSet = epsilonClosure(nextStateSet, nfa);

                if (!nextStateSet.isEmpty()) {
                    int newStateID;
                    if (dfaStates.containsKey(nextStateSet)) {
                        // If the key exists, retrieve the corresponding value
                        newStateID = dfaStates.get(nextStateSet);
                    } else {
                        // If the key does not exist, compute a new state ID and put it in the map
                        newStateID = StateIDGenerator.generateStateID();
                        dfaStates.put(nextStateSet, newStateID);
                        unprocessedStates.add(nextStateSet);
                        // see new state is accepting or not
                        for (int stateID : nextStateSet) {
                            if (nfa.getAcceptStates().contains(stateID)) {
                                dfa.markAsAccepting(newStateID);
                                dfa.acceptedNFAIDMap.computeIfAbsent(newStateID, k -> new HashSet<>()).addAll(nfa.acceptedNFAIDMap.get(stateID));
                                //break;
                            }
                        }

                    }
                    dfa.addTransition(currentStateID, symbol, newStateID);
                }
            }
        }

        return dfa;
    }

    // Helper method to get non-epsilon labels for DFA conversion
    private Set<String> getNonEpsilonLabels(Set<Integer> states, NFA nfa) {
        Set<String> nonEpsilonLabels = new HashSet<>();
        for (int state : states) {
            for (Set<String> label : nfa.getTransitions().getOrDefault(state, new HashMap<>()).keySet()) {
                if(label.contains("ANY")){
                    return nfa.allSymbolSet;
                }
                if (!label.contains("ε")) {
                    nonEpsilonLabels.addAll(label);
                }
            }
        }
        return nonEpsilonLabels;
    }

    // Epsilon closure for a set of states
    private Set<Integer> epsilonClosure(Set<Integer> states, NFA nfa) {
        Set<Integer> closure = new HashSet<>(states);
        Stack<Integer> stack = new Stack<>();

        stack.addAll(states);

        while (!stack.isEmpty()) {
            int state = stack.pop();
            Set<Integer> epsilonTransitions = nfa.getTransitions()
                    .getOrDefault(state, Collections.emptyMap())
                    .getOrDefault(Collections.singleton("ε"), Collections.emptySet());

            for (int nextState : epsilonTransitions) {
                if (closure.add(nextState)) {
                    stack.push(nextState);
                }
            }
        }
        return closure;
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
        NFA nfa1 = converter.convertToNFA("?*.r1.?*.r2.?*.r3");
        //nfa1.generateDiagram("nfa_1");
        NFA nfa2 = converter.convertToNFA("?*.r1.?*.r2.?*.r4");
        //NFA nfa3 = converter.convertToNFA("r1.?*.r2.?*.r8");
        //nfa2.generateDiagram("nfa_2");
//        NFA nfa3 = converter.convertToNFA("r2.?*.r8.?*.r12");
//        nfa3.generateDiagram("nfa_3");
//        //nfa.setID(1);
        DFA dfa1 = converter.convertToDFA(nfa1);
        //dfa1.minimizeDFA();
        //dfa1.generateDiagram("dfa_1");
        dfa1.minimizeDFA();
        dfa1.generateDiagram("min_dfa_1");

//
        DFA dfa2 = converter.convertToDFA(nfa2);
        //dfa2.generateDiagram("normal_dfa_2");
        dfa2.minimizeDFA();
        dfa2.generateDiagram("dfa_2");
        //DFA dfa3 = converter.convertToDFA(nfa3);
        //dfa3.minimizeDFA();
        NFA mergedNFA = new NFA(converter.allSymbolSet);
        int mergedStart = mergedNFA.getStartState();
//        mergedNFA.addTransition(mergedStart, "ε", nfa1.getStartState());
//        mergedNFA.addTransition(mergedStart, "ε", nfa2.getStartState());
////      mergedNFA.addTransition(mergedStart, "ε", nfa3.getStartState());
//        mergedNFA.getTransitions().putAll(nfa1.getTransitions());
//        mergedNFA.getTransitions().putAll(nfa2.getTransitions());
////      mergedNFA.getTransitions().putAll(nfa3.getTransitions());
//        mergedNFA.getAcceptStates().addAll(nfa1.getAcceptStates());
//        mergedNFA.getAcceptStates().addAll(nfa2.getAcceptStates());
////      mergedNFA.getAcceptStates().addAll(nfa3.getAcceptStates());
//        mergedNFA.addAllAcceptedStatesFromOtherNFA(nfa1);
//        mergedNFA.addAllAcceptedStatesFromOtherNFA(nfa2);
////      mergedNFA.addAllAcceptedStatesFromOtherNFA(nfa3);
//        mergedNFA.generateDiagram("Merged_NFA");
        mergedNFA.addTransition(mergedStart, "ε", dfa1.getStartState());
        mergedNFA.addTransition(mergedStart, "ε", dfa2.getStartState());
        //mergedNFA.addTransition(mergedStart, "ε", dfa3.getStartState());
        mergedNFA.getTransitions().putAll(dfa1.getTransitions());
        mergedNFA.getTransitions().putAll(dfa2.getTransitions());
        //mergedNFA.getTransitions().putAll(dfa3.getTransitions());
        mergedNFA.getAcceptStates().addAll(dfa1.getAcceptStates());
        mergedNFA.getAcceptStates().addAll(dfa2.getAcceptStates());
        //mergedNFA.getAcceptStates().addAll(dfa3.getAcceptStates());
        mergedNFA.addAllAcceptedStatesFromOtherDFA(dfa1);
        mergedNFA.addAllAcceptedStatesFromOtherDFA(dfa2);
        //mergedNFA.addAllAcceptedStatesFromOtherDFA(dfa3);
        mergedNFA.generateDiagram("Merged_NFA");
        DFA mergedDFA = converter.convertToDFA(mergedNFA);
        //mergedDFA.minimizeDFA();
////        DFA dfa1 = converter.convertToDFA(nfa3);
////        System.out.println(dfa.getId());
////        dfa.generateDiagram("dfa_thompson");
        //mergedDFA.minimizeDFA();
        mergedDFA.generateDiagram("merged_DFA");
        System.out.println("Printed");
    }
}
