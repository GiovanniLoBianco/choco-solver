package org.chocosolver.solver.search.strategy.countingbased.latinsquare;

public class Tools {
	
	public static final int SIZE = 100;
	public static final int SIZE_FACT = 171;
	
	private double[] factorsBM = new double[SIZE];
	private double[][] factorsLB = new double[SIZE][SIZE];
	private double[] fact = new double[SIZE_FACT];
	
	
	public Tools(){
		factorsBM[0] = 1;
		for(int i=1; i<SIZE; i++){
			factorsBM[i]= Math.pow(i/factorsBM[i-1], 1/(i*1.0))*factorsBM[i-1];
		}
		
		for(int i=0; i<SIZE; i++){
			for(int r=0; r<SIZE; r++){
				int q = (int) Math.min(Math.ceil((r+1)/2.0), Math.ceil((i+1)/2.0));
				factorsLB[i][r]=Math.sqrt(q*(r-q+1));
			}
		}
		
		this.fact[0]=1;
		for(int k=1; k<SIZE_FACT; k++){
			this.fact[k] = k*this.fact[k-1];
		}
	}
	
	public double computeBMFactors(int n){
		//TODO
		return factorsBM[n];
	}
	
	public double computeLBFactors(int i, int r){
		//TODO
		return factorsLB[i][r];
	}

	public double computeFactorial(int k){
		return fact[k];
	}
	

}
