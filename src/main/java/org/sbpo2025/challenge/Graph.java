package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Edge {
    int from, to, capacity, flow;

    public Edge(int from, int to, int capacity, int flow) {
        this.from = from;
        this.to = to;
        this.capacity = capacity;
        this.flow = 0;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getFlow() {
        return flow;
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
    Map<Integer, Edge> edges;
    Map<Integer, Edge> reverseEdges;

    public Vertex(int id) {
        this.id = id;
        this.edges = new HashMap<>();
        this.reverseEdges = new HashMap<>();
    }

    public Map<Integer, Edge> getEdges() {
        return edges;
    }

    public Map<Integer, Edge> getReverseEdges() {
        return reverseEdges;
    }

    public int getFlow(int to) {
        Edge edge = edges.get(to);
        return edge != null ? edge.flow : 0;
    }

    public int getCapacity(int to) {
        Edge edge = edges.get(to);
        return edge != null ? edge.capacity : 0;
    }

    public void addEdge(Edge edge) {
        edges.put(edge.to, edge);
    }

    public void addReverseEdge(Edge edge) {
        reverseEdges.put(edge.to, edge);
    }
}

class Graph {
    final boolean VERBOSE = true;

    public Map<Integer, Vertex> vertices;
    public Integer totalFlow;

    public List<Map<Integer, Integer>> solverOrders;
    public List<Map<Integer, Integer>> solverCorridors;
    public List<List<Integer>> matrixOrders;
    public List<List<Integer>> matrixCorridors;
    public Integer nItems;
    public Integer nCorridors;
    public Integer nOrders;
    public List<Integer> orders;
    public List<Integer> corridors;
    public List<Integer> items;
    public Integer waveSizeLB;
    public Integer waveSizeUB;
    public ChallengeSolver solver;

    public Graph(
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
        this.totalFlow = 0;

        createMatrixOrders();
        createMatrixCorridors();

        removeImpossibleOrders();

        linkOrders();
        linkCorridors();

        printNetwork();
    }

    public Graph(
        Map<Integer, Vertex> vertices,
        List<Integer> items,
        List<Integer> orders,
        List<Integer> corridors,
        List<List<Integer>> matrixOrders,
        List<List<Integer>> matrixCorridors,
        int nItems,
        int waveSizeLB,
        int waveSizeUB
    ) {
        int id;
        Vertex vertex;
        for (var entry : vertices.entrySet()) {            
            id = entry.getKey();
            vertex = entry.getValue();
            this.vertices.put(id, new Vertex(id));
            for (Map.Entry<Integer, Edge> edgeEntry : vertex.edges.entrySet()) {
                Edge edge = edgeEntry.getValue();
                this.vertices.get(id).addEdge(
                    new Edge(
                        edge.from,
                        edge.to,
                        edge.capacity,
                        edge.flow
                    )
                );
            }
            for (Map.Entry<Integer, Edge> reverseEntry : vertex.reverseEdges.entrySet()) {
                Edge edge = reverseEntry.getValue();
                this.vertices.get(id).addReverseEdge(
                    new Edge(
                        edge.from,
                        edge.to,
                        edge.capacity,
                        edge.flow
                    )
                );
            }
        }
        this.matrixOrders = matrixOrders;
        this.matrixCorridors = matrixCorridors;
        this.items = items;
        this.orders = orders;
        this.corridors = corridors;
        this.nItems = nItems;
        this.nCorridors = corridors.size();
        this.nOrders = orders.size();
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        this.solver = null;
        this.solverOrders = null;
        this.solverCorridors = null;
    }

    public Map<Integer, Vertex> getVertices() {
        return new HashMap<>(vertices);
    }

    public Vertex getVertex(int id) {
        return vertices.get(id);
    }

    public int getSourceId() {
        return 0;
    }

    public int getSinkId() {
        return 1;
    }

    public int getOrderId(int index) {
        return index + nItems + nCorridors + 2;
    }

    public int getOrderNumber(int orderId) {
        return orderId - (nItems + nCorridors + 2);
    }

    public int getItemId(int index) {
        return index + 2;
    }

    public int getCorridorId(int index) {
        return index + nItems + 2;
    }

    public int getCorridorNumber(int corridorId) {
        return corridorId - nItems - 2;
    }

    public Graph clone() {
        return new Graph(
            vertices, items, orders, corridors, matrixOrders, matrixCorridors, nItems, waveSizeLB, waveSizeUB
        );
    }

    public void linkOrders() {
        for (int i = 0; i < matrixOrders.size(); i++) {
            List<Integer> order = matrixOrders.get(i);
            int totalItems = order.stream().mapToInt(Integer::intValue).sum();
            
            boolean allZero = order.stream().allMatch(quantity -> quantity == 0);
            if (!allZero) {
                addOrder(i, totalItems);    
                for (Map.Entry<Integer, Integer> entry : solverOrders.get(i).entrySet()) {
                    int item = entry.getKey();
                    int quantity = entry.getValue();
                    if (!items.contains(item + 2)) {
                        addItem(item);
                    }
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
        Edge edge = new Edge(from, to, capacity, 0);
        vertices.get(from).addEdge(edge);

        Edge reverseEdge = new Edge(to, from, 0, 0);
        vertices.get(to).addReverseEdge(reverseEdge);

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

    public Map<Integer, Edge> getEdges(int vertexId) {
        return vertices.get(vertexId).getEdges();
    }

    public int getResidualCapacity(int from, int to) {
        Edge edge = vertices.get(from).getEdges().get(to);
        return edge != null ? edge.residualCapacity() : 0;
    }

    public void addFlow(int from, int to, int flow) {
        Edge edge = vertices.get(from).getEdges().get(to);
        if (edge != null) {
            edge.addFlow(flow);
        }
    }

    public void printNetwork() {
        for (Vertex vertex : vertices.values()) {
            for (Edge edge : vertex.edges.values()) {
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

    public String getVertexType(int id) {
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

    public int getVertexNumber(int id) {
        if (orders.contains(id)) {
            return id - (nItems + nCorridors + 2);
        } else if (corridors.contains(id)) {
            return id - (nItems + 2);
        } else if (items.contains(id)) {
            return id - 2;
        } else {
            return -1;
        }
    }

    public boolean augmentFlow(Map<Integer, Integer> parent) {
        int maxFlow = this.waveSizeUB - this.totalFlow;
        int flow = maxFlow;
        int v = getSinkId();
        Set<Integer> visitedNodes = new HashSet<>();

        while (v != getSourceId()) {
            if (visitedNodes.contains(v)) {
                throw new RuntimeException("Infinite loop detected in augmentFlow");
            }
            visitedNodes.add(v);
            int u = parent.get(v);
            flow = Math.min(flow, getResidualCapacity(u, v));
            if (flow < 0) {
                System.out.println("\033[1m\033[91mO Kant Fez merda\033[0m");
                System.exit(1);
            }
            v = u;
        }

        v = getSinkId();
        while (v != getSourceId()) {
            int u = parent.get(v);
            addFlow(u, v, flow);
            addFlow(v, u, -flow);
            v = u;
        }
        this.totalFlow += flow;

        return flow > 0;
    }

    public Map<Integer, Integer> findAugmentingPath(List<Integer> corridors, List<Integer> items, List<Integer> orders) {
        Map<Integer, Integer> parent = new HashMap<>();
        for (int corridor : corridors) {
            for (int item : items) {
                if (getResidualCapacity(item, corridor) > 0) {
                    for (int order : orders) {
                        if (getResidualCapacity(order, item) > 0) {
                            parent.put(getSinkId(), corridor);
                            parent.put(corridor, item);
                            parent.put(item, order);
                            parent.put(order, getSourceId());
                            return parent;
                        }
                    }
                }
            }
        }

        if (VERBOSE) 
            System.out.println("No augmenting path found");

        return parent;
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

    public Map<Integer, Vertex> getVerticesCopy() {
        Map<Integer, Vertex> copy = new HashMap<>();
        for (Map.Entry<Integer, Vertex> entry : vertices.entrySet()) {
            int id = entry.getKey();
            Vertex vertex = entry.getValue();
            copy.put(id, new Vertex(id));
            for (Map.Entry<Integer, Edge> edgeEntry : vertex.edges.entrySet()) {
                Edge edge = edgeEntry.getValue();
                copy.get(id).addEdge(
                    new Edge(
                        edge.from,
                        edge.to,
                        edge.capacity,
                        edge.flow
                    )
                );
            }
            for (Map.Entry<Integer, Edge> reverseEntry : vertex.reverseEdges.entrySet()) {
                Edge edge = reverseEntry.getValue();
                copy.get(id).addReverseEdge(
                    new Edge(
                        edge.from,
                        edge.to,
                        edge.capacity,
                        edge.flow
                    )
                );
            }
        }
        return copy;
    }

}