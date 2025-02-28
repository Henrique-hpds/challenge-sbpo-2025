package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChallengeSolver {
    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes
    
    private int latency;
    private List<String> memoryItems;
    private List<String> memoryAisles;

    private float timeBestRatio;
    private boolean verbose;
    private int resetThreshold;
    private List<Integer> totalItemsOrder;
    private List<Integer> totalItemsRequired;

    private List<Integer> idxOrders;
    private List<String> nameNodeOrders;

    private Matrix matrixOrders;
    private Matrix matrixAisles;

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nOrders;
    protected int nAisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nOrders = orders.size();
        this.nAisles = aisles.size();
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;

        this.resetThreshold = Math.max(nOrders, Math.max(nItems, nAisles));
        
        this.memoryItems = new ArrayList<String>();
        this.memoryAisles = new ArrayList<String>();

        for (int i = 0; i < nItems; i++)
            this.memoryItems.add("item_" + i);
        for (int i = 0; i < nAisles; i++)
            this.memoryAisles.add("aisle_" + i);

        this.matrixOrders = BasicFunctions.createMatrixOrders(orders, nItems, nOrders);
        this.matrixAisles = BasicFunctions.createMatrixAisles(aisles, nItems, nAisles);

        this.totalItemsOrder = matrixOrders.sumRow();
        this.totalItemsRequired = matrixOrders.sumColumn();

        Pair<List<Integer>, List<String>> pair = BasicFunctions.initIdxOrders(matrixOrders);
        this.idxOrders = pair.getKey();
        this.nameNodeOrders = pair.getValue();
        
        System.out.println("a");
    }

    public ChallengeSolution solve(StopWatch stopWatch, boolean verbose) {
        System.out.println("Limits: " + waveSizeLB + " <= flow <= " + waveSizeUB);
        
        Statistics.expectedRatioLimit(matrixAisles, waveSizeLB, waveSizeUB);
        BasicFunctions.removeImpossibleOrders(matrixOrders, matrixAisles);
        
        if (verbose){
            Statistics.statistics(matrixOrders, "pedido");
            System.out.println("\n");
            Statistics.statistics(matrixAisles, "corredor");
        }

        Graph graph = new Graph(matrixAisles, matrixOrders);

        return null;
    }

    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }

    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }

        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];

        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }

        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }

        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }

        return true;
    }

    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
