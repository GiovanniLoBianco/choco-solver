package org.chocosolver.solver.search.strategy.countingbased.latinsquare;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

public abstract class Square {
	
	private int n;
	private IntVar[][] square;
	private Model model;
	
	public Square(int n, IntVar[][] square, Model model){
		this.n=n;
		this.square=square;
		this.model=model;
	}
	
	public abstract void solve();
	
	public abstract void setCBSStrategy(String cas);

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public IntVar[][] getSquare() {
		return square;
	}

	public void setSquare(IntVar[][] square) {
		this.square = square;
	}

	public Model getModel() {
		return model;
	}

	public void setModel(Model model) {
		this.model = model;
	}
	
	public IntVar[] getVars(){
		IntVar[] vars = new IntVar[n*n];
		for(int i=0; i<n;i++){
			for(int j=0; j<n; j++){
				vars[i*n+j]=square[i][j];
			}
		}
		return vars;
	}
	
	

}
