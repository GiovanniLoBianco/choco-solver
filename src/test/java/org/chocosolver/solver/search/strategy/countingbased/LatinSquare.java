/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.countingbased;
import java.util.Arrays;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
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

	@Override
	public void setCBSStrategy(String cas) {
		// TODO Auto-generated method stub
		AverageSD strat = new AverageSD(model);
		this.getModel().getSolver().setSearch(strat);
		

	}
}
