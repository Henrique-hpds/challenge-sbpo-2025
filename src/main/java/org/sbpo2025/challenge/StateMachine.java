package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.List;

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
        List<Integer> corridorData = new ArrayList<>();
        for (Integer corridor : graph.corridors) {
            List<Edge> neighbors = graph.vertices.get(corridor + graph.nItems + 2).reverseEdges;
            int diverseItems = neighbors.size();
            int itemCapacity = graph.vertices.get(corridor + graph.nItems + 2).get()
            boolean maxFlowToSink = graph.matrixCorridors.get(corridor).get(graph.nOrders) == 0;
            boolean maxFlowFromPred = graph.vertices.get(corridor).predecessors.stream()
                    .mapToInt(neighbor -> graph.matrixCorridors.get(neighbor).get(corridor))
                    .sum() == 0;

            corridorData.add(corridor);
        }

        corridorData.sort((a, b) -> {
            int compare = Integer.compare(graph.vertices.get(b).diverseItems, graph.vertices.get(a).diverseItems);
            if (compare == 0) {
                compare = Integer.compare(graph.vertices.get(b).itemCapacity, graph.vertices.get(a).itemCapacity);
                if (compare == 0) {
                    compare = Boolean.compare(graph.vertices.get(b).maxFlowToSink, graph.vertices.get(a).maxFlowToSink);
                    if (compare == 0) {
                        compare = Boolean.compare(graph.vertices.get(b).maxFlowFromPred, graph.vertices.get(a).maxFlowFromPred);
                    }
                }
            }
            return compare;
        });

        return corridorData;
    }

    private List<Integer> choiceItemPriority(Graph graph, List<Integer> corridors) {
        List<Integer> itemData = new ArrayList<>();
        for (Integer item : graph.items) {
            int flowToCorridors = corridors.stream()
                    .filter(corridor -> graph.matrixOrders.get(item).get(corridor) > 0)
                    .mapToInt(corridor -> graph.matrixOrders.get(item).get(corridor))
                    .sum();
            int corridorsWithCapacity = (int) corridors.stream()
                    .filter(corridor -> graph.matrixOrders.get(item).get(corridor) > 0)
                    .count();
            boolean zeroCapacityToNeighbors = graph.vertices.get(item).successors.stream()
                    .mapToInt(neighbor -> graph.matrixOrders.get(item).get(neighbor))
                    .sum() == 0;

            itemData.add(item);
        }

        itemData.sort((a, b) -> {
            int compare = Integer.compare(graph.vertices.get(b).flowToCorridors, graph.vertices.get(a).flowToCorridors);
            if (compare == 0) {
                compare = Integer.compare(graph.vertices.get(a).corridorsWithCapacity, graph.vertices.get(b).corridorsWithCapacity);
                if (compare == 0) {
                    compare = Boolean.compare(graph.vertices.get(b).zeroCapacityToNeighbors, graph.vertices.get(a).zeroCapacityToNeighbors);
                }
            }
            return compare;
        });

        return itemData;
    }

    private List<Integer> choiceOrderPriority(Graph graph, List<Integer> items) {
        List<Integer> orderData = new ArrayList<>();
        for (Integer order : graph.orders) {
            int capacityForSelectedItems = items.stream()
                    .filter(item -> graph.matrixOrders.get(order).get(item) > 0)
                    .mapToInt(item -> graph.matrixOrders.get(order).get(item))
                    .sum();
            int selectedItemsCount = (int) items.stream()
                    .filter(item -> graph.matrixOrders.get(order).get(item) > 0)
                    .count();
            int flowForSelectedItems = items.stream()
                    .filter(item -> graph.matrixOrders.get(order).get(item) > 0)
                    .mapToInt(item -> graph.matrixOrders.get(order).get(item))
                    .sum();
            int totalCapacity = graph.vertices.get(order).successors.stream()
                    .mapToInt(neighbor -> graph.matrixOrders.get(order).get(neighbor))
                    .sum();

            orderData.add(order);
        }

        orderData.sort((a, b) -> {
            int compare = Integer.compare(graph.vertices.get(b).capacityForSelectedItems, graph.vertices.get(a).capacityForSelectedItems);
            if (compare == 0) {
                compare = Integer.compare(graph.vertices.get(b).selectedItemsCount, graph.vertices.get(a).selectedItemsCount);
                if (compare == 0) {
                    compare = Integer.compare(graph.vertices.get(b).flowForSelectedItems, graph.vertices.get(a).flowForSelectedItems);
                    if (compare == 0) {
                        compare = Integer.compare(graph.vertices.get(b).totalCapacity, graph.vertices.get(a).totalCapacity);
                    }
                }
            }
            return compare;
        });

        return orderData;
    }

    private Map<Integer, Integer> priorityChoice(Graph graph) {
        Map<Integer, Integer> parent = new HashMap<>();
        for (Integer node : graph.vertices.keySet()) {
            parent.put(node, null);
        }

        if (iterations % latency == 0) {
            List<Integer> corridors = choiceCorridorPriority(graph);
            List<Integer> items = choiceItemPriority(graph, corridors);
            List<Integer> orders = choiceOrderPriority(graph, items);


            graph.solverCorridors = corridors;
            graph.solverItems = items;
            graph.solverOrders = orders;

            return findAugmentingPath(graph, corridors, items, orders, parent);
        } else {
            parent = findAugmentingPath(graph, graph.solverCorridors, graph.solverItems, graph.solverOrders, parent);
            if (parent == null) {
                List<Integer> corridors = choiceCorridorPriority(graph);
                List<Integer> items = choiceItemPriority(graph, corridors);
                List<Integer> orders = choiceOrderPriority(graph, items);

                graph.solverCorridors = corridors;
                graph.solverItems = items;
                graph.solverOrders = orders;

                return findAugmentingPath(graph, corridors, items, orders, parent);
            }

            return parent;
        }
    }
}
