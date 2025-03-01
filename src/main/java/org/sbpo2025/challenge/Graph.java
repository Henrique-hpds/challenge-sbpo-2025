package org.sbpo2025.challenge;

import java.util.ArrayList;
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
    Map<Integer, Vertex> vertices;
    List<Map<Integer, Integer>> solverOrders;
    List<Map<Integer, Integer>> solverCorridors;
    List<Integer> orders;
    List<Integer> corridors;
    List<Integer> items;
    Integer nItems;
    Integer nCorridors;
    Integer nOrders;
    public List<Map<Integer, Integer>> matrixOrders;
    public List<Map<Integer, Integer>> matrixCorridors;
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

        createMatrixOrders();
        createMatrixCorridors();

        for (int i = 0; i < matrixOrders.size(); i++) {
            Map<Integer, Integer> order = matrixOrders.get(i);
            int totalItems = order.values().stream().mapToInt(Integer::intValue).sum();
            addOrder(i, totalItems);

            for (Map.Entry<Integer, Integer> entry : order.entrySet()) {
                int item = entry.getKey();
                int quantity = entry.getValue();
                addItem(item);
                linkOrderToItem(i, item, quantity);
            }
        }

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

        printNetwork();

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

                System.out.println("Edge from " + vertexTypeFrom + " " + displayFrom + " to " + vertexTypeTo + " " + displayTo + " with capacity " + edge.capacity + " and flow " + edge.flow);
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

    public void createMatrixOrders() {
        System.out.println("-------------- Creating matrix orders --------------");
        this.matrixOrders = new ArrayList<>();
        this.solverOrders.forEach(order -> {
            Map<Integer, Integer> orderMap = new HashMap<>();
            order.forEach((item, quantity) -> {
                orderMap.put(item, quantity);
            });
            this.matrixOrders.add(orderMap);
        });
    }

    public void createMatrixCorridors() {
        System.out.println("-------------- Creating matrix corridors --------------");
        this.matrixCorridors = new ArrayList<>();
        this.solverCorridors.forEach(corridor -> {
            Map<Integer, Integer> corridorMap = new HashMap<>();
            corridor.forEach((item, quantity) -> {
                corridorMap.put(item, quantity);
            });
            this.matrixCorridors.add(corridorMap);
        });
    }
}
