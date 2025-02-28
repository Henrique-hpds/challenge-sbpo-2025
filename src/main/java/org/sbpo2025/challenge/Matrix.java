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

    protected List<Integer> sumColumn(){
        
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

    protected List<Integer> sumRow(){
        
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
    
    protected int getNRows(){
        return nRows;
    }

    protected int getNCols(){
        return nCols;
    }

    protected int getElement(int i, int j){
        return matrix.get(i).get(j);
    }

    protected List<List<Integer>> getMatrix(){
        return matrix;
    }

    protected void setElement(int i, int j, int value){
        matrix.get(i).set(j, value);
    }
}
