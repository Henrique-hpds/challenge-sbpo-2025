package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BasicFunctions {

    protected static List<Integer> sumVector(List<Integer> a, List<Integer> b){
        List<Integer> sum = new ArrayList<Integer>();
        for (int i = 0; i < a.size(); i++)
            sum.add(a.get(i) + b.get(i));
        return sum;
    }

    protected static Matrix createMatrixOrders(List<Map<Integer, Integer>> orders, int nItems, int nOrders) {
        List<List<Integer>> matrixOrders = new ArrayList<>();

        for (int i = 0; i < nOrders; i++){
            Map<Integer, Integer> order = orders.get(i);
            List<Integer> listOrder = new ArrayList<>(Collections.nCopies(nItems, 0));


            for (Map.Entry<Integer, Integer> entry : order.entrySet()) {
                Integer key = entry.getKey();
                Integer value = entry.getValue();
                listOrder.set(key, value);
            }

            matrixOrders.add(listOrder);
        }
        return new Matrix(matrixOrders);
    }

    protected static Matrix createMatrixAisles(List<Map<Integer, Integer>> aisles, int nItems, int nAisles) {
        List<List<Integer>> matrixAisles = new ArrayList<>();

        for (int i = 0; i < nAisles; i++){
            Map<Integer, Integer> aisle = aisles.get(i);
            List<Integer> listAisles = new ArrayList<>(Collections.nCopies(nItems, 0));


            for (Map.Entry<Integer, Integer> entry : aisle.entrySet()) {
                Integer key = entry.getKey();
                Integer value = entry.getValue();
                listAisles.set(key, value);
            }

            matrixAisles.add(listAisles);
        }
        return new Matrix(matrixAisles);
    }
}
