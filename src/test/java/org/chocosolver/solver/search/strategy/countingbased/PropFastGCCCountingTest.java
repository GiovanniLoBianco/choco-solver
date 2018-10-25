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
import java.util.Random;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.nary.globalcardinality.GlobalCardinality;
import org.chocosolver.solver.constraints.nary.globalcardinality.PropFastGCC;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.testng.annotations.Test;

public class PropFastGCCCountingTest {

	public static GlobalCardinality generateRandomGCC(Model model, IntVar[] vars, int[] vals, long seed) {
		int m = vals.length;

		int k = 0;
		while (k < m && vals[k] == k) {
			k++;
		}
		assert k == m : "The array of values must be such that, forall j, vals[j]=j. As it is a test tool, the values must be normalized this way.";

		int[] sizeInvertedDomains = new int[m];
		for (int i = 0; i < vars.length; i++) {
			for (int y = vars[i].getLB(); y <= vars[i].getUB(); y = vars[i].nextValue(y)) {
				sizeInvertedDomains[y]++;
			}
		}
		int[] randomLB = new int[m];
		int[] randomUB = new int[m];
		Random rnd = new Random(seed);
		for (int j = 0; j < m; j++) {
			int r1 = rnd.nextInt(sizeInvertedDomains[j] + 1);
			int r2 = rnd.nextInt(sizeInvertedDomains[j] + 1);
			if (r1 < r2) {
				randomLB[j] = r1;
				randomUB[j] = r2;
			} else {
				randomLB[j] = r2;
				randomUB[j] = r1;
			}
		}
		int expectedSumLB = rnd.nextInt(vars.length + 1);
		int sumLB = 0;
		for (int j = 0; j < m; j++) {
			sumLB += randomLB[j];
		}
		ArrayList<Integer> canBeDecreased = new ArrayList<Integer>();
		for (int j = 0; j < m; j++) {
			if (randomLB[j] > 0) {
				canBeDecreased.add(j);
			}
		}
		while (sumLB > expectedSumLB) {
			int toBeDecreased = rnd.nextInt(canBeDecreased.size());
			int index = canBeDecreased.get(toBeDecreased);
			randomLB[index]--;
			if (randomLB[index] == 0) {
				canBeDecreased.remove(toBeDecreased);
			}
			sumLB--;
		}
		IntVar[] occurrences = new IntVar[m];
		for (int j = 0; j < m; j++) {
			occurrences[j] = model.intVar(randomLB[j], randomUB[j]);
		}

		GlobalCardinality gcc = new GlobalCardinality(vars, vals, occurrences, true);
		return gcc;
	}

	public static Model generateRandomValueGraph(int n, int m, double p, long seed) {
		Model model = new Model();
		IntVar[] vars = new IntVar[n];
		Random rnd = new Random(seed);
		for (int i = 0; i < n; i++) {
			ArrayList<Integer> l_dom = new ArrayList<Integer>();
			for (int j = 0; j < m; j++) {
				if (rnd.nextDouble() < p) {
					l_dom.add(j);
				}
			}
			if (l_dom.size() == 0) {
				l_dom.add(rnd.nextInt(m));
			}
			int[] dom = new int[l_dom.size()];
			for (int k = 0; k < l_dom.size(); k++) {
				dom[k] = l_dom.get(k);
			}
			vars[i] = model.intVar(dom);
		}
		return model;
	}

	public static GlobalCardinality generateInstance1(){
		Model model = new Model();
		
		IntVar[] vars = new IntVar[7];
		vars[0] = model.intVar(new int[] {1,2,5});
		vars[1] = model.intVar(new int[] {0,1,3,6});
		vars[2] = model.intVar(new int[] {0,1});
		vars[3] = model.intVar(new int[] {1,2,5});
		vars[4] = model.intVar(4);
		vars[5] = model.intVar(new int[] {0,3,4,6});
		vars[6] = model.intVar(new int[] {4,5});
		
		int[] values = new int[] {0,1,3,5};
		
		IntVar[] cards = new IntVar[4];
		cards[0] = model.intVar(1,3);
		cards[1] = model.intVar(0,1);
		cards[2] = model.intVar(1,2);
		cards[3] = model.intVar(0,2);
		
		return (GlobalCardinality) model.globalCardinality(vars, values, cards, false);
	}
	
	
	
	@Test
	public void f() {
	}

	public static void main(String[] args) {
		CountingTools tools = new CountingTools();
		System.out.println("ça commence");
		int n = 20;
		int m = 200;
		double p = 0.9;

		Random rnd = new Random();
		long seed = rnd.nextLong();
		System.out.println("SEED : " + seed);

		Model model = generateRandomValueGraph(n, m, p, seed);

		IntVar[] vars = model.retrieveIntVars(true);

		int[] vals = new int[m];
		for (int j = 0; j < m; j++) {
			vals[j] = j;
		}
		GlobalCardinality gcc = generateRandomGCC(model, vars, vals, seed);
		
		//GlobalCardinality gcc = generateInstance1();
		PropFastGCC prop = (PropFastGCC) gcc.getPropagator(0);
		
		try {
			prop.propagate(PropagatorEventType.FULL_PROPAGATION.getMask());
			
			System.out.println(prop.estimateNbSolutions(CountingEstimators.GCC_CORRECTION, tools));
		} catch (ContradictionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
	}

}
