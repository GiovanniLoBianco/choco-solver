package org.chocosolver.solver.search.strategy.countingbased.latinsquare;
import java.util.Arrays;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.countingbased.CountingBasedStrategy;
import org.chocosolver.solver.search.strategy.countingbased.CountingEstimators;
import org.chocosolver.solver.search.strategy.countingbased.MaxSD;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;


public class LatinSquare extends Square {

	public LatinSquare(int[][] instance) {
		super(instance.length, new IntVar[instance.length][instance.length], new Model());

		// Creation of variables
		for (int i = 0; i < this.getN(); i++) {
			for (int j = 0; j < this.getN(); j++) {
				if (instance[i][j] == -1) {
					this.getSquare()[i][j] = this.getModel().intVar(0, this.getN() - 1);
				} else {
					this.getSquare()[i][j] = this.getModel().intVar(instance[i][j]);
				}
			}
		}

		// Creation of constraints
		for (int i = 0; i < this.getN(); i++) {
			this.getModel().allDifferent(this.getSquare()[i], "AC").post();

		}
		for (int j = 0; j < this.getN(); j++) {
			IntVar[] col_j = new IntVar[this.getN()];
			for (int i = 0; i < this.getN(); i++) {
				col_j[i] = this.getSquare()[i][j];
			}
			this.getModel().allDifferent(col_j, "AC").post();
		}

		// this.getModel().getSolver().showDecisions();
	}
	
	@Override
	public void setCBSStrategy(String cas) {
		// TODO Auto-generated method stub
		CountingBasedStrategy search = new MaxSD(this.getModel());
		search.setEstimatorAlldifferent(CountingEstimators.ALLDIFFERENT_FDS);
		this.getModel().getSolver().setSearch(search);
	}
	
	public void setDOWDStrategy(String cas) {
		// TODO Auto-generated method stub
		this.getModel().getSolver().setSearch(Search.domOverWDegSearch(getModel().retrieveIntVars(true)));
	}

	public void solve() {
		Solver solver = this.getModel().getSolver();
		long time = System.currentTimeMillis();
		System.out.println("Start solving...");
		if (solver.solve()) {
			System.out.println("time: " + (System.currentTimeMillis() - time));

			for (int i = 0; i < this.getN(); i++) {
				String s = "";
				for (int j = 0; j < this.getN(); j++) {
					String nb = this.getSquare()[i][j].getValue() + "";
					if (nb.length() == 1) {
						nb += " ";
					}
					s += nb + " ";
				}
				System.out.println(s);
			}
			// do something, e.g. print out variable values
		} else if (solver.hasEndedUnexpectedly()) {
			System.out.println("The could not find a solution nor prove that none exists in the given limits");
		} else {
			System.out.println("The solver has proved the problem has no solution");
		}
	}


	public static void main(String[] args) {
		
		int n =40;
		long timelimit = 600000;

		float[] timesER = new float[n];
		float[] timesFDS = new float[n];
		float[] timesPQZ = new float[n];
		long[] backtracksER = new long[n];
		long[] backtracksFDS = new long[n];
		long[] backtracksPQZ = new long[n];
		

		String[] filepaths = new String[n];
		for (int i = 0; i < n/2; i++) {
			filepaths[i] = "instances/LatinSquare/qwh.o30.h374." + (i + 1) + ".pls";
			filepaths[i + 20] = "instances/LatinSquare/qwh.o30.h375." + (i + 1) + ".pls";
		}

		for (int i = 0; i < n; i++) {
			String filepath = filepaths[i];

			System.out.println("Construction des modèles pour l'instance "+i);
			LatinSquare ls = InstancesReader.readLatinSquareInstance(filepath);
			LatinSquare ls2 = InstancesReader.readLatinSquareInstance(filepath);
			LatinSquare ls3 = InstancesReader.readLatinSquareInstance(filepath);
			System.out.println("Instances construites");

			System.out.println("_____________________________________________________");
			System.out.println("Début résolution ER");
			ls.setCBSStrategy("");
			ls.getModel().getSolver().showStatistics();
			ls.getModel().getSolver().limitTime(timelimit);
			long time = System.currentTimeMillis();
			ls.solve();
			time=System.currentTimeMillis()-time;
			timesER[i]= time;
			backtracksER[i]=ls.getModel().getSolver().getBackTrackCount();

			System.out.println("_____________________________________________________");
			System.out.println("Début résolution FDS");
			ls2.setCBSStrategy("");
			ls2.getModel().getSolver().showStatistics();
			ls2.getModel().getSolver().limitTime(timelimit);
			time = System.currentTimeMillis();
			ls2.solve();
			time=System.currentTimeMillis()-time;
			timesFDS[i]=time;
			backtracksFDS[i]=ls2.getModel().getSolver().getBackTrackCount();

			System.out.println("_____________________________________________________");
			System.out.println("Début résolution PQZ");
			ls3.setDOWDStrategy("");
			ls3.getModel().getSolver().showStatistics();
			ls3.getModel().getSolver().limitTime(timelimit);
			time = System.currentTimeMillis();
			ls3.solve();
			time=System.currentTimeMillis()-time;
			timesPQZ[i]=time;
			backtracksPQZ[i]=ls3.getModel().getSolver().getBackTrackCount();
		}
		
		for(int i=0; i<n; i++){
			System.out.println("Instance "+i+" : ");
			System.out.println("ER -> time : "+timesER[i]+ " , backtracks : "+backtracksER[i]);
			System.out.println("FDS -> time : "+timesFDS[i]+ " , backtracks : "+backtracksFDS[i]);
			System.out.println("PQZ -> time : "+timesPQZ[i]+ " , backtracks : "+backtracksPQZ[i]);
		}
		
		System.out.println("-------------------------------------------");
		
		Arrays.sort(timesER);
		Arrays.sort(timesFDS);
		Arrays.sort(timesPQZ);
		Arrays.sort(backtracksER);
		Arrays.sort(backtracksFDS);
		Arrays.sort(backtracksPQZ);
		
		String timesERpaste = "";
		String timesFDSpaste = "";
		String timesPQZpaste = "";
		String backtracksERpaste = "";
		String backtracksFDSpaste = "";
		String backtracksPQZpaste = "";
		
		for(int i=0; i<n; i++){
			timesERpaste+=" ("+timesER[i]+", "+(i+1)*2.5+")";
			timesFDSpaste+=" ("+timesFDS[i]+", "+(i+1)*2.5+")";
			timesPQZpaste+=" ("+timesPQZ[i]+", "+(i+1)*2.5+")";
			backtracksERpaste+=" ("+backtracksER[i]+", "+(i+1)*2.5+")";
			backtracksFDSpaste+=" ("+backtracksFDS[i]+", "+(i+1)*2.5+")";
			backtracksPQZpaste+=" ("+backtracksPQZ[i]+", "+(i+1)*2.5+")";
		}
		
		System.out.println("times ER : " + timesERpaste);
		System.out.println("times FDS : " + timesFDSpaste);
		System.out.println("times PQZ : " + timesPQZpaste);
		System.out.println("backtracks ER : " + backtracksERpaste);
		System.out.println("backtracks FDS : " + backtracksFDSpaste);
		System.out.println("backtracks PQZ : " + backtracksPQZpaste);
		
	}
	

	

}
