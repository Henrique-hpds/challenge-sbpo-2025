package org.sbpo2025.challenge;

import java.lang.Math;
import java.util.List;

public class Statistics{

    private class Calculate{
        private double media;
        private double variancia;
        private double desvioPadrao;

        public Calculate(List<Integer> vector){
            this.media = vector.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            this.variancia = vector.stream().mapToDouble(x -> Math.pow(x - media, 2)).average().orElse(0.0);
            this.desvioPadrao = Math.sqrt(variancia);
        }
    }
}