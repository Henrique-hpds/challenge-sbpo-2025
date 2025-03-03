package org.sbpo2025.challenge;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class IterationInfo implements Comparable<IterationInfo> {
    int totalFlow;
    public Map<Integer, Vertex> vertices;
    Graph graph;
    float ratio;
    float time;
    int iteration;
    List<Integer> usedCorridors;
    List<Integer> usedOrders;
    final float startTime;

    public IterationInfo(
        int totalFlow, Graph graph, float ratio, int iteration, float startTime,
        List<Integer> usedCorridors, List<Integer> usedOrders
    ) {
        this.totalFlow = totalFlow;
        this.vertices = graph.getVerticesCopy();
        this.graph = graph;
        this.ratio = ratio;
        this.time = System.currentTimeMillis() / 1000;
        this.iteration = iteration;
        this.startTime = startTime;
        this.usedOrders = new ArrayList<>();
        this.usedCorridors = new ArrayList<>();
        for (int i = 0; i < usedOrders.size(); i++) {
            this.usedOrders.add(usedOrders.get(i));
        }
        for (int i = 0; i < usedCorridors.size(); i++) {
            this.usedCorridors.add(usedCorridors.get(i));
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
