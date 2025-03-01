package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Edge {
    int from, to, capacity, flow;

    public Edge(int from, int to, int capacity) {
        this.from = from;
        this.to = to;
        this.capacity = capacity;
        this.flow = 0;
    }

    public int residualCapacity() {
        return capacity - flow;
    }

    public void addFlow(int addedFlow) {
        this.flow += addedFlow;
    }
}

class Vertex {
    int id;
    List<Edge> edges;

    public Vertex(int id) {
        this.id = id;
        this.edges = new ArrayList<>();
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
    }
}

class FlowNetwork {
    final boolean VERBOSE = true;

    Map<Integer, Vertex> vertices;
    List<Map<Integer, Integer>> solverOrders;
    List<Map<Integer, Integer>> solverCorridors;
    List<Integer> orders;
    List<Integer> corridors;
    List<Integer> items;
    Integer nItems;
    Integer nCorridors;
    Integer nOrders;
    Integer waveSizeLB;
    Integer waveSizeUB;
    List<List<Integer>> matrixOrders;
    List<List<Integer>> matrixCorridors;
    ChallengeSolver solver;

    public FlowNetwork(
        List<Map<Integer, Integer>> orders,
        List<Map<Integer, Integer>> aisles,
        int nItems,
        int waveSizeLB,
        int waveSizeUB
    ) {
        this.vertices = new HashMap<>();
        this.solverOrders = orders;
        this.solverCorridors = aisles;
        this.orders = new ArrayList<>();
        this.corridors = new ArrayList<>();
        this.items = new ArrayList<>();
        this.nItems = nItems;
        this.nCorridors = aisles.size();
        this.nOrders = orders.size();
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;

        createMatrixOrders();
        createMatrixCorridors();

        removeImpossibleOrders();

        linkOrders();
        linkCorridors();

        printNetwork();

    }

    public void linkOrders() {
        for (int i = 0; i < matrixOrders.size(); i++) {
            List<Integer> order = matrixOrders.get(i);
            int totalItems = order.stream().mapToInt(Integer::intValue).sum();
            addOrder(i, totalItems);

            boolean allZero = order.stream().allMatch(quantity -> quantity == 0);
            if (!allZero) {
                for (Map.Entry<Integer, Integer> entry : solverOrders.get(i).entrySet()) {
                    int item = entry.getKey();
                    int quantity = entry.getValue();
                    addItem(item);
                    linkOrderToItem(i, item, quantity);
                }
            }
        }
    }

    public void linkCorridors() {
        for (int i = 0; i < solverCorridors.size(); i++) {
            Map<Integer, Integer> aisle = solverCorridors.get(i);
            int aisleId = i;
            addCorridor(aisleId);

            for (Map.Entry<Integer, Integer> entry : aisle.entrySet()) {
                int item = entry.getKey();
                linkItemToCorridor(item, aisleId, entry.getValue());
            }

            int totalItemsInCorridor = aisle.values().stream().mapToInt(Integer::intValue).sum();
            linkCorridorToSink(aisleId, totalItemsInCorridor);
        }
    }

    

    public void addVertex(int id) {
        if (!vertices.containsKey(id)) {
            vertices.put(id, new Vertex(id));
        }
    }

    public void addEdge(int from, int to, int capacity) {
        addVertex(from);
        addVertex(to);
        Edge edge = new Edge(from, to, capacity);
        vertices.get(from).addEdge(edge);
    }

    public void addOrder(int id, int totalItems) {
        int orderId = id + nItems + nCorridors + 2;
        addVertex(orderId);
        orders.add(orderId);
        addEdge(0, orderId, totalItems);
    }

    public void addItem(int id) {
        int itemId = id + 2;
        addVertex(itemId);
        items.add(itemId);
    }

    public void addCorridor(int id) {
        int corridorId = id + nItems + 2;
        addVertex(corridorId);
        corridors.add(corridorId);
    }

    public void linkOrderToItem(int orderId, int itemId, int totalItem) {
        int adjustedOrderId = orderId + nItems + nCorridors + 2;
        int adjustedItemId = itemId + 2;
        addEdge(adjustedOrderId, adjustedItemId, totalItem);
    }

    public void linkItemToCorridor(int itemId, int corridorId, int totalItem) {
        int adjustedCorridorId = corridorId + nItems + 2;
        int adjustedItemId = itemId + 2;
        addEdge(adjustedItemId, adjustedCorridorId, totalItem);
    }

    public void linkCorridorToSink(int corridorId, int totalItems) {
        int adjustedCorridorId = corridorId + nItems + 2;
        addEdge(adjustedCorridorId, 1, totalItems);
    }

    public List<Edge> getEdges(int vertexId) {
        return vertices.get(vertexId).edges;
    }

    public int getResidualCapacity(int from, int to) {
        for (Edge edge : vertices.get(from).edges) {
            if (edge.to == to) {
                return edge.residualCapacity();
            }
        }
        return 0;
    }

    public void addFlow(int from, int to, int flow) {
        for (Edge edge : vertices.get(from).edges) {
            if (edge.to == to) {
                edge.addFlow(flow);
                break;
            }
        }
    }

    public void printNetwork() {
        for (Vertex vertex : vertices.values()) {
            for (Edge edge : vertex.edges) {
                String vertexTypeFrom = getVertexType(edge.from);
                String vertexTypeTo = getVertexType(edge.to);
                int displayFrom = edge.from;
                int displayTo = edge.to;

                if (orders.contains(edge.from)) {
                    displayFrom -= (nItems + nCorridors + 2);
                } else if (corridors.contains(edge.from)) {
                    displayFrom -= (nItems + 2);
                } else if (items.contains(edge.from)) {
                    displayFrom -= 2;
                }

                if (orders.contains(edge.to)) {
                    displayTo -= (nItems + nCorridors + 2);
                } else if (corridors.contains(edge.to)) {
                    displayTo -= (nItems + 2);
                } else if (items.contains(edge.to)) {
                    displayTo -= 2;
                }
                
                if (!VERBOSE) {
                    System.out.println("Edge from " + vertexTypeFrom + " " + displayFrom + " to " + vertexTypeTo + " " + displayTo + " with capacity " + edge.capacity + " and flow " + edge.flow);
                }
            }
        }
    }

    private String getVertexType(int id) {
        if (id == 0) {
            return "Source";
        } else if (id == 1) {
            return "Sink";
        } else if (orders.contains(id)) {
            return "Order";
        } else if (corridors.contains(id)) {
            return "Corridor";
        } else if (items.contains(id)) {
            return "Item";
        } else {
            return "Unknown";
        }
    }

    private void removeImpossibleOrders() {
        List<Integer> itemsAvailable = new ArrayList<>();
        int nValidOrders = 0;
        int nInvalidOrders = 0;

        for (int i = 0; i < nItems; i++) {
            itemsAvailable.add(0);
        }

        for (List<Integer> corridor : matrixCorridors) {
            for (int idx = 0; idx < nItems; idx++) {
                itemsAvailable.set(idx, itemsAvailable.get(idx) + corridor.get(idx));
            }
        }

        if (VERBOSE) {
            System.out.println("ITEMS AVAILABLE:" + itemsAvailable);
        }

        for (List<Integer> order : matrixOrders) {
            boolean valid = true;
            for (int idx = 0; idx < order.size(); idx++) {
                if (order.get(idx) > itemsAvailable.get(idx)) {
                    valid = false;
                    break;
                }
            }
            if (valid) {
                nValidOrders++;
            } else {
                Collections.fill(order, 0);
                nInvalidOrders++;
            }
        }

        if (VERBOSE) {
            System.out.println("valid orders: " + nValidOrders + ", invalid orders: " + nInvalidOrders + " (all removed)");
        }
    }

    public void createMatrixOrders() {
        this.matrixOrders = new ArrayList<>();
        for (int i = 0; i < this.nOrders; i++) {
            List<Integer> orderList = new ArrayList<>();
            for (int j = 0; j < this.nItems; j++) {
                orderList.add(0);
            }
            this.matrixOrders.add(orderList);
        }

        for (int i = 0; i < this.solverOrders.size(); i++) {
            int index = i;
            Map<Integer, Integer> order = this.solverOrders.get(index);
            order.forEach((item, quantity) -> {
                this.matrixOrders.get(index).set(item, this.matrixOrders.get(index).get(item) + quantity);
            });
        }

        if (VERBOSE && matrixOrders.size() < 10) {
            System.out.println("-------------- MATRIX ORDERS --------------");
            printMatrix(matrixOrders);
        }
    }

    public void createMatrixCorridors() {
        this.matrixCorridors = new ArrayList<>();
        for (int i = 0; i < this.nCorridors; i++) {
            List<Integer> corridorList = new ArrayList<>();
            for (int j = 0; j < this.nItems; j++) {
                corridorList.add(0);
            }
            this.matrixCorridors.add(corridorList);
        }

        for (int i = 0; i < this.solverCorridors.size(); i++) {
            int index = i;
            Map<Integer, Integer> corridor = this.solverCorridors.get(index);
            corridor.forEach((item, quantity) -> {
                this.matrixCorridors.get(index).set(item, quantity);
            });
        }

        if (VERBOSE && matrixCorridors.size() < 10) {
            System.out.println("-------------- MATRIX CORRIDORS --------------");
            printMatrix(matrixCorridors);
        }
    }

    public void printMatrix(List<List<Integer>> matrix) {
        for (List<Integer> row : matrix) {
            System.out.println(row);
        }
    }

}