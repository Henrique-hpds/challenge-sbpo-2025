package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.lang.Math;
import java.text.DecimalFormat;

public class Statistics{

    private static class Calculate{
        private float mean;
        private float variance;
        private float standardDeviation;

        public Calculate(List<Integer> vector){
            this.mean = (float) vector.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            this.variance = (float) vector.stream().mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(0.0);
            this.standardDeviation = (float) Math.sqrt(variance);
        }

        public float getMean(){
            return mean;
        }

        public float getVariance(){
            return variance;
        }

        public float getStandardDeviation(){
            return standardDeviation;
        }

    }

    protected static void expectedRatioLimit(Matrix matrixAisles, int lb, int ub){
        List<Integer> totalSorted = matrixAisles.sumRow();
        Collections.sort(totalSorted, Collections.reverseOrder());

        float sum = 0;
        int lenTotalSorted = totalSorted.size();
        DecimalFormat df = new DecimalFormat("#.00");
        
        for (int i = 0; i < lenTotalSorted; i++){
            sum += totalSorted.get(i);
            if (sum >= lb){
                if (sum < ub)
                    System.out.println("The maximum ratio is: " + df.format((double)(sum / (i + 1))));    
                else
                    System.out.println("The maximum ratio is: " + df.format((double)(ub / (i + 1))));
                break;
            }
        }

        float averageTotal = ((float) totalSorted.stream().mapToInt(Integer::intValue).sum()) / ((float)lenTotalSorted);
        sum = 0;

        for (int i = 0; i < lenTotalSorted; i++){
            sum += averageTotal;
            if (sum >= lb){
                if (sum < ub)
                    System.out.println("The average ratio is: " + df.format((double)(sum / (i + 1))));    
                else
                    System.out.println("The average ratio is: " + df.format((double)(ub / (i + 1))));
                break;
            }
        }

        sum = 0;

        for (int i = 0; i < lenTotalSorted; i++){
            sum += totalSorted.get((int)lenTotalSorted/2);
            if (sum >= lb){
                if (sum < ub)
                    System.out.println("The median ratio is: " + df.format((double)(sum / (i + 1))));    
                else
                    System.out.println("The median ratio is: " + df.format((double)(ub / (i + 1))));
                break;
            }
        }
        
        sum = 0;

        for (int i = 0; i < lenTotalSorted; i++){
            if (i % 2 == 0)
                sum += totalSorted.get(((int) lenTotalSorted / 2) - ((int) i / 2));
            else
                sum += totalSorted.get(((int) lenTotalSorted / 2) + ((int) i / 2));
            if (sum >= lb){
                if (sum < ub)
                    System.out.println("The median ratio is: " + df.format((double)(sum / (i + 1))));    
                else
                    System.out.println("The median ratio is: " + df.format((double)(ub / (i + 1))));
                break;
            }
        }

    }

    protected static List<Float> statistics(Matrix matrix, String type){
        
        System.out.println("Total de" + type + "s: " + matrix.getNRows());

        List<Integer> totalEachOrder = matrix.sumRow();
        List<Integer> totalTypeItems = new ArrayList<>();

        for (List<Integer> row : matrix.getMatrix()) {
            int count = 0;
            for (int item : row) {
                if (item != 0) {
                    count++;
                }
            }
            totalTypeItems.add(count);
        }

        DecimalFormat df = new DecimalFormat("#.00");
        
        Calculate calcEachOrder = new Calculate(totalEachOrder);
        float mean1 = calcEachOrder.getMean();
        float variance1 = calcEachOrder.getVariance();
        float standardDeviation1 = calcEachOrder.getStandardDeviation();
        System.out.println("Média de itens por " + type + ": " + df.format(mean1));
        System.out.println("Variância de itens por " + type + ": " + df.format(variance1));
        System.out.println("Desvio padrão de itens por " + type + ": " + df.format(standardDeviation1));
        
        Calculate calcTypeItems = new Calculate(totalTypeItems);
        float mean2 = calcTypeItems.getMean();
        float variance2 = calcTypeItems.getVariance();
        float standardDeviation2 = calcTypeItems.getStandardDeviation();
        System.out.println("Média de tipos de itens por " + type + ": " + df.format(mean2));
        System.out.println("Variância de tipos de itens por " + type + ": " + df.format(variance2));
        System.out.println("Desvio padrão de tipos de itens por " + type + ": " + df.format(standardDeviation2));

        if (standardDeviation2 * mean2 < 3)
            System.out.printf("Desvio padrão da quantidade de tipos de itens por %s: %.2f \033[1;32m✔\033[0m%n", type, standardDeviation2);
        else
            System.out.printf("Desvio padrão da quantidade de tipos de itens por %s: %.2f%n", type, standardDeviation2);

        List<Float> result = new ArrayList<>();
        result.add(mean1);
        result.add(standardDeviation1);
        result.add(mean2);
        result.add(standardDeviation2);
        return result;
    }
}