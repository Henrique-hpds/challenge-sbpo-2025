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
    Integer iterations;
    Integer bestIteration;
    List<List<Integer>> matrixOrders;
    List<List<Integer>> matrixCorridors;
    IterationInfo bestInfo, currentInfo, prevInfo;
    boolean maximumFlowEnable;
    int resetThreshold = 1000;
    int restartGraph = 0;
    int hardReset = 0;

    List<Integer> memoryItems;
    int upIndexItem;

    final private float startTime = System.currentTimeMillis() / 1000;

    final private List<Integer> totalAvailable;
    final private List<Integer> totalRequired;
    private List<Integer> usedCorridors;
    private List<Integer> ordersCompleted;
    int totalItems;

    boolean VERBOSE;

    public StateMachine(Graph graph) {
        this.latency = Math.max((int) (Math.log(graph.nItems + graph.nCorridors + graph.nOrders) / Math.log(2)), 3); // Temporary value
        this.iterations = 0;
        this.graph = graph;
        matrixCorridors = graph.matrixCorridors;
        matrixOrders = graph.matrixOrders;
        totalAvailable = new ArrayList<>(Collections.nCopies(this.graph.nItems, 0));
        totalRequired = new ArrayList<>(Collections.nCopies(this.graph.nItems, 0));
        VERBOSE = graph.VERBOSE;
        bestInfo = new IterationInfo(0, graph, 0, 0, startTime, new ArrayList<>(), new ArrayList<>());
        currentInfo = bestInfo;
        prevInfo = bestInfo;
        maximumFlowEnable = true;
    }

    public ChallengeSolution run() {
        List<Integer> corridorPriority = choiceCorridorPriority(graph);
        List<Integer> itemPriority = choiceItemPriority(graph);
        List<Integer> orderPriority = choiceOrderPriority(graph);
        Map<Integer, Integer> parent;

        int maxUB = 5, counterUB = 0;

        while (true) {
            parent = graph.findAugmentingPath(corridorPriority, itemPriority, orderPriority);
            if (parent.isEmpty()) {
                if (VERBOSE) {
                    System.out.println("\033[1m\033[91mNo augmenting path found.\033[0m");
                }
                break;
            }

            graph.augmentFlow(parent);
            if (VERBOSE)
                printParent(parent);
            
            if (VERBOSE){
                System.out.println("Flow: " + graph.totalFlow);
                System.out.println("");
            }

            /* TODO: expansão baseada em corredores (Do Contra) */
            
            if (resetCondition()) {
                if (VERBOSE) {
                    System.out.println("\033[1m\033[91mReset condition met.\033[0m");
                }
                resetGraph();
            }

            if (graph.waveSizeLB <= graph.totalFlow) {
                analyzeFlow(2, 500);
                if (
                    usedCorridors.size() != 0 &&
                    graph.waveSizeLB <= totalItems &&
                    totalItems <= graph.waveSizeUB
                ) {
                    updateInfo();
                    if (VERBOSE) {
                        System.out.println("Info updated");
                        System.out.println("Objective function: " + currentInfo.ratio + " -- BEST: " + bestInfo.ratio);
                        System.out.println("Corridors used: " + usedCorridors);
                        System.out.println("Orders completed: " + ordersCompleted);
                    }
                }
            }

            if (iterations % 100 == 0) {
                System.out.println("Iteration: " + iterations + " -- Flow: " + graph.totalFlow + " -- Current ratio " + currentInfo.ratio + " -- Best: " + bestInfo.ratio + " -- Time: " + (System.currentTimeMillis() / 1000 - startTime));
            }

            if (stoppingCondition()) {
                if (VERBOSE) {
                    System.out.println("\"\\033[1m\\033[91mStopping condition met\\033[0m\"");
                }
                break;
            }
            
            iterations++;
        }

        Set<Integer> setOrdersCompleted = new HashSet<>(ordersCompleted);
        Set<Integer> setUsedCorridors = new HashSet<>(usedCorridors);

        return new ChallengeSolution(setOrdersCompleted, setUsedCorridors);
    }

    final private float endTime = 600;
    final private Integer maxIterations = 5000;

    private boolean stoppingCondition() {
        float currentTime = System.currentTimeMillis() / 1000;
        if (currentTime - startTime >= endTime * 0.95 || iterations >= maxIterations || hardReset > 5) 
            return true;
        return false;
    }

    private boolean resetCondition() {
        float currentRatio = currentInfo.ratio;
        float previousRatio = prevInfo.ratio;

        // TODO levar em conta a quantidade de corredores uteis, pois isso influencia o quanto o valor objetivo pode piorar

        // TODO... Há casos em que o fluxo esta muito superior a LB, porém a quantidade de items que estão sendo coletados é baixa,
        // talvez valha a pena criar parâmetros que detectem isso e tratem esse caso 

        // If the current ratio is less than 75% of the best ratio and more than 60 seconds have passed
        boolean boolReset = (System.currentTimeMillis() / 1000 - bestInfo.time > 60) && 
                (bestInfo.ratio != 0 && (currentRatio / bestInfo.ratio) < 0.75);
        // The current total flow is significantly greater than the best total flow and without the best ratio
        boolReset = boolReset || (((graph.totalFlow - bestInfo.totalFlow) > graph.waveSizeUB / 5) && 
                  bestInfo.ratio != 0 && currentRatio / bestInfo.ratio < 0.8);
        // Or the current ratio is much lower than the best ratio
        boolReset = boolReset || (bestInfo.ratio != 0 && (currentRatio / bestInfo.ratio) < 0.2);
        // And the best iteration is not infinity
        boolReset = boolReset && bestInfo.iteration != Integer.MAX_VALUE;
        // And the current ratio is less than 90% of the best ratio
        boolReset = (boolReset && bestInfo.ratio != 0 && (currentRatio / bestInfo.ratio) < 0.9);
        // And the current total flow is significantly greater than the best total flow
        boolReset = (boolReset && (graph.totalFlow - bestInfo.totalFlow) > graph.waveSizeUB / 10);
        // Or if the time is about to run out
        boolReset = boolReset || (System.currentTimeMillis() / 1000 - startTime >= endTime * 0.90);
        boolReset = boolReset || (previousRatio != 0 && Math.abs(currentRatio / previousRatio) < 0.3 && 
                  currentRatio != 0 && previousRatio > 10 && graph.totalFlow > graph.waveSizeLB * 1.2);
        // And the current ratio is less than 90% of the best ratio
        boolReset = boolReset && !(currentRatio > previousRatio * 1.03);
        boolReset = boolReset && !(currentRatio >= 0.9 * bestInfo.ratio);
        boolReset = boolReset && !(currentRatio <= 0.5);
        // Reset the graph when the flow reaches 90% of the maximum allowed flow, but only at the "beginning of the code"
        if (graph.totalFlow >= 0.9 * graph.waveSizeUB && !maximumFlowEnable) {
            boolReset = true;
        }
        // Define this RESET_THRESHOLD value better
        if (bestInfo.iteration + resetThreshold < iterations && bestInfo.ratio != 0) {
            boolReset = true;
            restartGraph = 10000; // this forces the hard reset
        }
        if (bestInfo.iteration + resetThreshold < iterations && bestInfo.ratio != 0) {
            boolReset = true;
            restartGraph = 10000;
        }
        return boolReset;
    }

    private void resetGraph() {
        restartGraph++;
        bestIteration = bestInfo.iteration;

        if (restartGraph > latency * 2) {
            hardReset++;
            if (VERBOSE) 
                System.out.println("Hard reset at the graph.");
            
            graph.resetFlow();
            restartGraph = 0;
            bestIteration = Integer.MAX_VALUE;
            currentInfo = new IterationInfo(0, graph, 0, iterations, startTime, new ArrayList<>(), new ArrayList<>());
            bestInfo = currentInfo;
        } else if (bestInfo.ratio != 0) {
            // TODO remover de forma aleátoria ou por prioridade os corredores que são usados, 
            // mas que menos comtribuem para o objetivo
            if (VERBOSE)
                System.out.println("Reset graph for maximum flow.");
            graph = bestInfo.graph.clone();
            currentInfo = bestInfo;
            restartGraph++;
        }
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
        return 0f;
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
            if (VERBOSE)
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

    private void updateInfo() {
        prevInfo = currentInfo;
        currentInfo = new IterationInfo(
            graph.totalFlow,
            graph,
            objectiveFunction(),
            iterations,
            startTime,
            usedCorridors,
            ordersCompleted
        );
        if (currentInfo.compareTo(bestInfo) > 0) {
            bestInfo = currentInfo;
        }
    }

    private void analyzeFlow(
        int maxLenCombinations,
        int maxCombinations
    ) {
        usedCorridors = new ArrayList<>();
        ordersCompleted = new ArrayList<>();
    
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
        
        findCompletedOrders();

        totalItems = totalSumList(totalRequired);
    
        /* TODO: Remove unnecessary corridors */
    }

}

