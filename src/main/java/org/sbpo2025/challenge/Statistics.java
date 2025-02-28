package org.sbpo2025.challenge;

import java.util.Collections;
import java.util.List;
import java.lang.Math;
import java.text.DecimalFormat;

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
}