package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.List;

public class Matrix {
    private List<List<Integer>> matrix;
    private int nRows;
    private int nCols;

    public Matrix(List<List<Integer>> matrix){
        this.matrix = matrix;
        this.nRows = matrix.size();
        this.nCols = matrix.get(0).size();
    }

    public List<Integer> sumColumn(){
        
        List<Integer> listSum = new ArrayList<Integer>();
        
        for(int i = 0; i < nCols; i++){
            int sum = 0;
            for (int j = 0; j < nRows; j++){
                sum += matrix.get(j).get(i);
            }
            listSum.add(sum);
        }

        return listSum;
       
    }

    public List<Integer> sumRow(){
        
        List<Integer> listSum = new ArrayList<Integer>();
        
        for(int i = 0; i < nRows; i++){
            int sum = 0;
            for (int j = 0; j < nCols; j++){
                sum += matrix.get(i).get(j);
            }
            listSum.add(sum);
        }

        return listSum;
    }
    
    public int getNRows(){
        return nRows;
    }

    public int getNCols(){
        return nCols;
    }

    public int getElement(int i, int j){
        return matrix.get(i).get(j);
    }

    public void setElement(int i, int j, int value){
        matrix.get(i).set(j, value);
    }
}
