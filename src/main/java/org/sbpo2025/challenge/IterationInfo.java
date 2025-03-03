package org.sbpo2025.challenge;

import java.util.List;
import java.util.Map;

public class IterationInfo implements Comparable<IterationInfo> {
    int totalFlow;
    public Map<Integer, Vertex> vertices;
    Graph graph;
    float ratio;
    int time;
    int iteration;
    List<Integer> usedCorridors;
    List<Integer> usedOrders;
    final int start_time;

    public static IterationInfo bestIteration;

    public IterationInfo(
        int totalFlow, Graph graph, float ratio, int time, int iteration, int start_time,
        List<Integer> usedCorridors, List<Integer> usedOrders
    ) {
        this.totalFlow = totalFlow;
        this.vertices = graph.getVerticesCopy();
        this.graph = graph;
        this.ratio = ratio;
        this.time = time;
        this.iteration = iteration;
        this.start_time = start_time;
        for (int i = 0; i < usedOrders.size(); i++) {
            this.usedOrders.add(usedOrders.get(i));
        }
        for (int i = 0; i < usedCorridors.size(); i++) {
            this.usedCorridors.add(usedCorridors.get(i));
        }

        if (bestIteration == null || this.compareTo(bestIteration) > 0) {
            bestIteration = this;
        }
    }

    @Override
    public int compareTo(IterationInfo o) {
        if (this.ratio > o.ratio) {
            return 1;
        } else if (this.ratio < o.ratio) {
            return -1;
        } else {
            if (this.usedCorridors.size() < o.usedCorridors.size()) {
                return 1;
            } else if (this.usedCorridors.size() > o.usedCorridors.size()) {
                return -1;
            } else {
                if (this.usedOrders.size() < o.usedOrders.size()) {
                    return 1;
                } else if (this.usedOrders.size() > o.usedOrders.size()) {
                    return -1;
                }
            }
            return 0;
        }
    }
}
