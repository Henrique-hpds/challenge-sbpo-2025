package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StateMachine {
    Integer latency;
    Graph graph;
    Graph bestGraph;
    Integer iterations;
    Integer totalFlow;
    Integer bestFlow;
    Integer bestIteration;
    List<List<Integer>> matrixOrders;
    List<List<Integer>> matrixCorridors;

    List<Integer> memoryItems;
    int upIndexItem;

    final private List<Integer> totalAvailable;
    final private List<Integer> totalRequired;

    boolean VERBOSE;

    public StateMachine(Graph graph) {
        this.latency = Math.max((int) (Math.log(graph.nItems + graph.nCorridors + graph.nOrders) / Math.log(2)), 3); // Temporary value
        this.iterations = 0;
        this.graph = graph;
        bestGraph = graph;
        matrixCorridors = graph.matrixCorridors;
        matrixOrders = graph.matrixOrders;
        totalFlow = 0;
        bestFlow = 0;
        bestIteration = 0;
        totalAvailable = new ArrayList<>(Collections.nCopies(this.graph.nItems, 0));
        totalRequired = new ArrayList<>(Collections.nCopies(this.graph.nItems, 0));
        VERBOSE = graph.VERBOSE;
    }

    public ChallengeSolution run() {
        

        List<Integer> corridorPriority = choiceCorridorPriority(graph);
        List<Integer> itemPriority = choiceItemPriority(graph);
        List<Integer> orderPriority = choiceOrderPriority(graph);

        while (true) {
            Map<Integer, Integer> parent = graph.findAugmentingPath(corridorPriority, itemPriority, orderPriority);
            
            if(graph.augmentFlow(parent)) {
                printParent(parent);
            } else {
                System.out.println("Flow not augmented");
                break;
            }

            System.out.println("Total flow: " + graph.totalFlow);
        }

        analyzeFlow(2, 500);
        System.out.println("Objective function: " + objectiveFunction());

        System.out.println("Used corridors: " + usedCorridors);
        System.out.println("Orders completed: " + ordersCompleted);

        Set<Integer> setOrdersCompleted = new HashSet<>(ordersCompleted);
        Set<Integer> setUsedCorridors = new HashSet<>(usedCorridors);

        return new ChallengeSolution(setOrdersCompleted, setUsedCorridors);
    }

    private float objectiveFunction() {
        int items = 0;
        for (int order : ordersCompleted) {
            items += totalSumList(matrixOrders.get(order));
        }
        if (VERBOSE) {
            System.out.println("Items: " + items + " -- Upper bound: " + graph.waveSizeUB + " -- Lower bound: " + graph.waveSizeLB);
        }
        if (items >= graph.waveSizeLB || usedCorridors.size() > 0){
            return (float) items / usedCorridors.size();
        }
        return 0;
    }

    private void printParent(Map<Integer, Integer> parent) {
        int corridor = parent.get(graph.getSinkId());
        int item = parent.get(corridor);
        int order = parent.get(item);
        System.out.printf(
            "Parent: %s %d --(%d/%d)-> %s %d --(%d/%d)-> %s %d --(%d/%d)-> %s %d --(%d/%d)-> %s %d\n", 
            graph.getVertexType(graph.getSourceId()), graph.getSourceId(),
            graph.getVertex(graph.getSourceId()).getFlow(order), graph.getVertex(graph.getSourceId()).getCapacity(order),
            graph.getVertexType(order), graph.getVertexNumber(order),
            graph.getVertex(order).getFlow(item), graph.getVertex(order).getCapacity(item),
            graph.getVertexType(item), graph.getVertexNumber(item),
            graph.getVertex(item).getFlow(corridor), graph.getVertex(item).getCapacity(corridor),
            graph.getVertexType(corridor), graph.getVertexNumber(corridor),
            graph.getVertex(corridor).getFlow(graph.getSinkId()), graph.getVertex(corridor).getCapacity(graph.getSinkId()),
            graph.getVertexType(graph.getSinkId()), graph.getSinkId()
        );
    }

    private List<Integer> choiceCorridorPriority(Graph graph) {
        List<List<Integer>> corridorData = new ArrayList<>();
        int idSink = graph.getSinkId();
        for (Integer idCorridor : graph.corridors) {
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
                    idCorridor,
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

        if (VERBOSE) 
            System.out.println("Corridor priority: " + sortedCorridors);

        return sortedCorridors;
    }

    private List<Integer> choiceItemPriority(Graph graph) {
        List<List<Integer>> itemData = new ArrayList<>();
        Integer capacity = 0;
        for (Integer idItem : graph.items) {
            int flowToCorridors = 0;
            int corridorsWithCapacity = 0;
            boolean zeroCapacityToNeighbors = true;

            for (Integer corridor : graph.getVertex(idItem).getEdges().keySet()) {
                capacity = graph.getVertex(idItem).getCapacity(corridor);
                flowToCorridors += capacity;
                if (capacity > 0) {
                    corridorsWithCapacity++;
                }
            }

            for (Edge edge : graph.getVertex(idItem).getEdges().values()) {
                if (edge.getCapacity() > 0) {
                    zeroCapacityToNeighbors = false;
                    break;
                }
            }

            itemData.add(
                List.of(
                    idItem,
                    flowToCorridors,
                    corridorsWithCapacity,
                    zeroCapacityToNeighbors ? 1 : 0
                )
            );
        }

        itemData.sort((a, b) -> {
            int cmp = b.get(1).compareTo(a.get(1));
            if (cmp != 0) return cmp;
            cmp = a.get(2).compareTo(b.get(2));
            if (cmp != 0) return cmp;
            return b.get(3).compareTo(a.get(3));
        });

        List<Integer> sortedItems = new ArrayList<>();
        if (memoryItems == null || memoryItems.isEmpty()) {
            memoryItems = new ArrayList<>();
            for (List<Integer> data : itemData) {
                memoryItems.add(data.get(0));
            }
        }
        if (upIndexItem < 0) {
            upIndexItem = 0;
        }
        sortedItems.addAll(memoryItems.subList(upIndexItem, memoryItems.size()));

        if (VERBOSE)
            System.out.print("Item priority: " + sortedItems + "\n");

        return sortedItems;
    }

    private List<Integer> choiceOrderPriority(Graph graph) {
        List<List<Integer>> orderData = new ArrayList<>();

        for (Integer order : graph.orders) {
            Map<Integer, Edge> connectedItems = graph.getVertex(order).getEdges();
            int capacityForSelectedItems = 0;
            int selectedItemsCount = 0;
            int flowForSelectedItems = 0;
            int totalCapacity = 0;

            for (Map.Entry<Integer, Edge> entry : connectedItems.entrySet()) {
            Integer item = entry.getKey();
            Edge edge = entry.getValue();
            if (graph.items.contains(item)) {
                capacityForSelectedItems += edge.getCapacity();
                if (edge.getCapacity() > 0) {
                selectedItemsCount++;
                }
                flowForSelectedItems += edge.getFlow();
            }
            totalCapacity += edge.getCapacity();
            }

            orderData.add(
            List.of(
                order,
                capacityForSelectedItems,
                selectedItemsCount,
                flowForSelectedItems,
                totalCapacity
            )
            );
        }

        orderData.sort((a, b) -> {
            int cmp = b.get(1).compareTo(a.get(1));
            if (cmp != 0) return cmp;
            cmp = a.get(2).compareTo(b.get(2));
            if (cmp != 0) return cmp;
            cmp = a.get(3).compareTo(b.get(3));
            if (cmp != 0) return cmp;
            return a.get(4).compareTo(b.get(4));
        });

        List<Integer> sortedOrders = new ArrayList<>();
        for (List<Integer> data : orderData) {
            sortedOrders.add(data.get(0));
        }

        if (VERBOSE)
            System.out.println("Order priority: " + sortedOrders);

        return sortedOrders;
    }

    private Map<Integer, Integer> updatePriorities() {
        if (VERBOSE) {
            System.out.println("Updating priorities...");
        }

        List<Integer> corridorPriority = choiceCorridorPriority(graph);
        List<Integer> itemPriority = choiceItemPriority(graph);
        List<Integer> orderPriority = choiceOrderPriority(graph);

        return graph.findAugmentingPath(corridorPriority, itemPriority, orderPriority);
    }

    private void sumVectors(List<Integer> list1, List<Integer> list2) {
        if (list1.size() != list2.size()) {
            System.out.println("Lists must have the same size");
            return;
        }
        for (int i = 0; i < list1.size(); i++) {
            list1.set(i, list1.get(i) + list2.get(i));
        }
    }

    private void findCompletedOrders() {
        for (int orderVertex : graph.orders) {
            int order = graph.getOrderNumber(orderVertex);
            if (graph.getVertex(graph.getSourceId()).getFlow(orderVertex) == graph.getVertex(graph.getSourceId()).getCapacity(orderVertex)) {
                ordersCompleted.add(order);
                sumVectors(totalRequired, matrixOrders.get(order));
            }
        }
    }

    private int totalSumList(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).sum();
    }

    private List<Integer> usedCorridors;
    private List<Integer> ordersCompleted;

    private void analyzeFlow(
        int maxLenCombinations,
        int maxCombinations
    ) {
        usedCorridors = new ArrayList<>();
    
        Collections.fill(totalAvailable, 0);
        Collections.fill(totalRequired, 0);
    
        for (int corridor : graph.corridors) {
            if (graph.getVertex(corridor).getFlow(graph.getSinkId()) > 0) {
                usedCorridors.add(graph.getCorridorNumber(corridor));
            }
        }
    
        for (int corridor : usedCorridors) {
            sumVectors(totalAvailable, matrixCorridors.get(corridor));
        }
    
        ordersCompleted = new ArrayList<>();
        findCompletedOrders();
    
        /* TODO: Remove unnecessary corridors */
    }

}

