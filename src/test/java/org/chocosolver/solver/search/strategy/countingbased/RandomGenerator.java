/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.countingbased;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.search.strategy.strategy.FullyRandom;
import org.chocosolver.solver.variables.IntVar;

public class RandomGenerator {

	public static LatinSquare generateRandomLatinSquare(int n, Random rnd, double d, int seed) {
		int[][] instance = new int[n][n];
		Model model = new Model();
		IntVar[][] square = new IntVar[n][n];
		IntVar[] inArray = new IntVar[n * n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				square[i][j] = model.intVar(0, n - 1);
				inArray[i * n + j] = square[i][j];
			}
		}
		for (int i = 0; i < n; i++) {
			model.allDifferent(square[i]).post();
		}
		for (int j = 0; j < n; j++) {
			IntVar[] col = new IntVar[n];
			for (int i = 0; i < n; i++) {
				col[i] = square[i][j];
			}
			model.allDifferent(col).post();
		}
		model.getSolver().setSearch(new FullyRandom(inArray, seed));
		model.getSolver().solve();

		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				if (rnd.nextDouble() < d) {
					instance[i][j] = -1;
				} else {
					instance[i][j] = square[i][j].getValue();
				}
			}
		}

		return new LatinSquare(instance);
	}

	
	public static void main(String[] args) {
		/*Random generator = new Random();
		// -1445325
		int seed = -2031710255; //generator.nextInt();
		System.out.println(seed);
		int n = 19;
		double d = 0.8;
		Random rnd = new Random(seed);*/

		LatinSquare ms = InstancesReader.readLatinSquareInstance("instances/LatinSquare/qwh.o30.h374.1.pls");
		ms.getModel().getSolver().showStatistics();
		//ms.getModel().getSolver().showDecisions(() -> "");
		//ms.getModel().getSolver().limitNode(100);
		ms.setCBSStrategy("");
		ms.solve();
		
		

		

	}

}
