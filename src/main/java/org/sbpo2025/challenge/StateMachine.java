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
    List<Integer> memoryCorridors;
    List<Integer> memoryOrders;

    private long startTime;

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
        usedCorridors = new ArrayList<>();
        ordersCompleted = new ArrayList<>();
        maximumFlowEnable = false;
        startTime = (long) ((double) System.currentTimeMillis() / (long) 1000);
    }

    public ChallengeSolution run() {
        Map<Integer, Integer> parent;

        int maxUB = 5, counterUB = 0;

        while (true) {
            parent = priorityChoice();
            if (parent.isEmpty()) {
                if (VERBOSE) {
                    System.out.println("\033[1m\033[91mNo augmenting path found.\033[0m");
                }
                break;
            }

            if (!graph.expandFlowByCorridors(usedCorridors)) {
                if (VERBOSE) {
                    System.out.println("\033[1m\033[91mImpossível expandir.\033[0m");
                }
            }

            /* TODO: folga nos corredores */
            
            graph.augmentFlow(parent);
            if (VERBOSE)
                printParent(parent);
            
            if (VERBOSE){
                System.out.println("Flow: " + graph.totalFlow);
                System.out.println("");
            }
            
            if (resetCondition()) {
                if (VERBOSE) {
                    System.out.println("\033[1m\033[91mReset condition met.\033[0m");
                }
                resetGraph();
            }

            if (graph.waveSizeLB <= graph.totalFlow) {
                analyzeFlow(2, 500);
                if (totalItems >= graph.waveSizeLB) {
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
                System.out.println("Iteration: " + iterations + " -- Flow: " + graph.totalFlow + " -- Current ratio " + currentInfo.ratio + " -- #Corridors " + usedCorridors.size() + " -- #Orders " + ordersCompleted.size() + " -- Total items " + totalItems + " -- Best: " + bestInfo.ratio + " -- Time: " + ((long) ((double) System.currentTimeMillis() / (long) 1000) - startTime));
            }

            if (stoppingCondition()) {
                if (VERBOSE) {
                    System.out.println("\"\\033[1m\\033[91mStopping condition met\\033[0m\"");
                }
                break;
            }
            
            iterations++;
        }

        Set<Integer> setOrdersCompleted = new HashSet<>(bestInfo.usedOrders);
        Set<Integer> setUsedCorridors = new HashSet<>(bestInfo.usedCorridors);

        int items = 0;
        for (int order : setOrdersCompleted) {
            items += totalSumList(matrixOrders.get(order));
        }
        System.out.println("Total items: " + items);
        System.out.println("Orders completed: " + setOrdersCompleted);
        System.out.println("Used corridors: " + setUsedCorridors);

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
        //teste, antes era  total flow
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
        return false;
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

    private void sortPriorities(List<List<Integer>> priorities) {
        priorities.sort((a, b) -> {
            for (int i = 1; i < a.size(); i++) {
                int cmp = b.get(i).compareTo(a.get(i));
                if (cmp != 0) return cmp;
            }
            return 0;
        });
    }

    private Map<Integer, Integer> priorityChoice() {
        if (iterations % latency == 0) {
            if (VERBOSE) {
                System.out.println("Updating priorities...");
            }
            List<Integer> corridors = choiceCorridorPriority(graph);
            List<Integer> items = choiceItemPriority(graph);
            List<Integer> orders = choiceOrderPriority(graph);

            memoryItems = items;
            memoryCorridors = corridors;
            memoryOrders = orders;
            return graph.findAugmentingPath(corridors, items, orders);
        } else {
            Map<Integer, Integer> parent = graph.findAugmentingPath(memoryCorridors, memoryItems, memoryOrders);
            if (parent.isEmpty()) {
                if (VERBOSE) {
                    System.out.println("Updating priorities...");
                }
                List<Integer> corridors = choiceCorridorPriority(graph);
                List<Integer> items = choiceItemPriority(graph);
                List<Integer> orders = choiceOrderPriority(graph);

                memoryItems = items;
                memoryCorridors = corridors;
                memoryOrders = orders;

                return graph.findAugmentingPath(corridors, items, orders);
            }
            return parent;
        }
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
                    diverseItems += item.getCapacity() - item.getFlow();
                }
            }
            int corridorCapacity = graph.getVertex(idCorridor).getCapacity(idSink);// - graph.getVertex(idCorridor).getFlow(idSink);

            corridorData.add(
                List.of(
                    idCorridor,
                    -diverseItems,
                    corridorCapacity
                )
            );
        }

        sortPriorities(corridorData);

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

            for (Integer corridor : graph.getVertex(idItem).getEdges().keySet()) {
                capacity = graph.getVertex(idItem).getFlow(corridor);
                flowToCorridors += capacity;
            }

            itemData.add(
                List.of(
                    idItem,
                    flowToCorridors
                )
            );
        }

        sortPriorities(itemData);

        List<Integer> sortedItems = new ArrayList<>();
        for (List<Integer> data : itemData) {
            sortedItems.add(data.get(0));
        }

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

            for (Map.Entry<Integer, Edge> entry : connectedItems.entrySet()) {
                Integer item = entry.getKey();
                Edge edge = entry.getValue();
                if (graph.items.contains(item)) {
                    capacityForSelectedItems += edge.getCapacity();
                    if (edge.getCapacity() > 0) {
                        selectedItemsCount++;
                    }
                    flowForSelectedItems += edge.getFlow();//(edge.getCapacity() - edge.getFlow())/edge.getCapacity();
                }
            }

            orderData.add(
                List.of(
                    order,
                    capacityForSelectedItems,
                    selectedItemsCount,
                    flowForSelectedItems
                )
            );
        }

        sortPriorities(orderData);

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
        int sum = 0;
        for (int value : list) {
            sum += value;
        }
        return sum;
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

        // usedCorridors = removeUnnecessaryCorridors(
        //     usedCorridors,
        //     maxCombinations,
        //     maxLenCombinations
        // );

        totalItems = totalSumList(totalRequired);
    
        /* TODO: Remove unnecessary corridors */
    }

    private int findIndex(List<Integer> list, int value) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == value) {
                return i;
            }
        }
        return -1;
    }

    private List<Integer> removeUnnecessaryCorridors(
        List<Integer> usedCorridors,
        int maxCombinations,
        int maxLenCombinations
    ) {
        int maxLenCombination = Math.max(3, maxLenCombinations);
        int totalOfCombinations = (int) ((Math.pow(2, maxLenCombination) - 1) * usedCorridors.size());
        if (totalOfCombinations > maxCombinations) {
            maxLenCombination = (int) (Math.log(maxCombinations / usedCorridors.size() + 1) / Math.log(2));
            totalOfCombinations = (int) ((Math.pow(2, maxLenCombination) - 1) * usedCorridors.size());
        }

        int rMemory = 0;
        boolean foundCombinations = false;

        for (int r = maxLenCombination; r > 0; r--) {
            if (usedCorridors.size() == 1 || foundCombinations || ordersCompleted.isEmpty() || totalSumList(totalRequired) < graph.waveSizeLB) {
                break;
            }
            rMemory = r;
            List<List<Integer>> combinations = generateCombinations(new ArrayList<>(usedCorridors), r);
            for (List<Integer> combo : combinations) {
                List<Integer> totalCombo = new ArrayList<>(Collections.nCopies(graph.nItems, 0));
                for (int corridor : combo) {
                    sumVectors(totalCombo, matrixCorridors.get(corridor));
                }
                boolean remove = true;
                for (int i = 0; i < totalAvailable.size(); i++) {
                    if (totalAvailable.get(i) - totalCombo.get(i) < totalRequired.get(i)) {
                        remove = false;
                        break;
                    }
                }

                if (remove) {
                    if (VERBOSE) {
                        System.out.println("\033[1m\033[94m" + combo.size() + "/" + usedCorridors.size() + " is unnecessary \033[0m");
                    }
                    for (int corridor : combo) {
                        if (VERBOSE)
                            System.out.println("Removing corridor " + corridor);
                        usedCorridors.remove(findIndex(usedCorridors, corridor));
                        List<Integer> aux = new ArrayList<>();
                        for (int value : matrixCorridors.get(corridor)) {
                            aux.add(-1 * value);
                        }
                        sumVectors(totalAvailable, aux);
                    }
                    foundCombinations = true;
                    break;
                }
            }
        }

        if (rMemory >= maxLenCombination) {
            maxLenCombination = rMemory + 1;
        } else {
            maxLenCombination = Math.max(1, rMemory - 1);
        }

        return usedCorridors;
    }

    private List<List<Integer>> generateCombinations(List<Integer> corridors, int r) {
        List<List<Integer>> combinations = new ArrayList<>();
        int n = corridors.size();
        int[] indices = new int[r];
        for (int i = 0; i < r; i++) {
            indices[i] = i;
        }
        while (indices[r - 1] < n) {
            List<Integer> combination = new ArrayList<>();
            for (int i = 0; i < r; i++) {
                combination.add(corridors.get(indices[i]));
            }
            combinations.add(combination);
            int t = r - 1;
            while (t != 0 && indices[t] == n - r + t) {
                t--;
            }
            indices[t]++;
            for (int i = t + 1; i < r; i++) {
                indices[i] = indices[i - 1] + 1;
            }
        }
        return combinations;
    }

}

