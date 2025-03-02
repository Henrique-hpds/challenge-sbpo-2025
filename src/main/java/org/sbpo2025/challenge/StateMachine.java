package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StateMachine {
    Integer latency;
    Graph graph;
    Graph bestGraph;
    Integer iterations;
    Integer totalFlow;
    Integer bestFlow;
    Integer bestIteration;

    public StateMachine(Graph graph) {
        this.latency = Math.max((int) (Math.log(graph.nItems + graph.nCorridors + graph.nOrders) / Math.log(2)), 3); // Temporary value
        this.iterations = 0;
        this.graph = graph;
        bestGraph = graph;
        totalFlow = 0;
        bestFlow = 0;
        bestIteration = 0;
    }

    public ChallengeSolution run() {
        
        

        return null;
    }


    private List<Integer> choiceCorridorPriority(Graph graph) {
        List<List<Integer>> corridorData = new ArrayList<>();
        int idSink = graph.getSinkId();
        for (Integer corridor : graph.corridors) {
            int idCorridor = graph.getCorridorId(corridor);
            Map<Integer, Edge> items = graph.getVertex(idCorridor).getReverseEdges();
            int diverseItems = 0; // quantidade e intens não saturados
            for (Map.Entry<Integer, Edge> entry : items.entrySet()) {
                Edge item = entry.getValue();
                if (item.getCapacity() - item.getFlow() > 0) {
                    diverseItems++;
                }
            }
            int corridorCapacity = graph.getVertex(idCorridor).getCapacity(idSink);
            boolean maxFlowToSink = graph.getVertex(idCorridor).getCapacity(idSink) == 0;
            boolean maxFlowFromPred = items.values().stream().mapToInt(item -> item.getCapacity()).sum() == 0;

            corridorData.add(
                List.of(
                    corridor,
                    diverseItems,
                    corridorCapacity,
                    maxFlowToSink ? 1 : 0,
                    maxFlowFromPred ? 1 : 0
                )
            );
        }

        corridorData.sort((a, b) -> {
            for (int i = 1; i < a.size(); i++) {
                int cmp = b.get(i).compareTo(a.get(i));
                if (cmp != 0) return cmp;
            }
            return 0;
        });

        List<Integer> sortedCorridors = new ArrayList<>();
        for (List<Integer> data : corridorData) {
            sortedCorridors.add(data.get(0));
        }

        System.out.println("Corridor priority order: " + sortedCorridors);

        return sortedCorridors;
    }
    
}
