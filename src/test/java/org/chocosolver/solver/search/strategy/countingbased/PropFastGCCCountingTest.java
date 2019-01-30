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

}
