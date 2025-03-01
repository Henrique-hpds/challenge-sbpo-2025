package org.sbpo2025.challenge;

import java.util.List;
import java.util.Map;
import java.util.HashMap;


public class Graph {
    private List<Vertex> vertices;
    private Map<Vertex, Map<Vertex, Map<String, Integer>>> adjdict;

    private class Vertex{
        private String label;
        private int index;

        public Vertex(String label, int index){
            this.label = label;
            this.index = index;
        }

        public String getLabel(){
            return this.label;
        }

        public int getIndex(){
            return this.index;
        }

        public String getName(){
            return this.label + "_" + this.index;
        }
    }
        

    public Graph(Matrix matrixAisles, Matrix matrixOrders){

    }
}
