/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.countingbased;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffAC;
import org.chocosolver.solver.constraints.nary.globalcardinality.PropFastGCC;
import org.chocosolver.solver.search.strategy.countingbased.tools.BinaryHeap;
import org.chocosolver.solver.search.strategy.countingbased.tools.ComparablePair;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;

public class AverageSD extends CountingBasedStrategy {

	public AverageSD(Model model) {
		super(model);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void computeOrder() {
		// TODO Auto-generated method stub
		// In this binary heap, we sort every computed densities. We deal with
		// these duplicates, that references a same assigment after.

		Map<IntVarAssignment, Double> sumDensities = new HashMap<IntVarAssignment, Double>();
		Map<IntVarAssignment, Integer> times = new HashMap<IntVarAssignment, Integer>();

		for (Countable c : this.getCountables()) {
			Map<IntVarAssignment, Double> densities;
			if (c instanceof PropAllDiffAC) {
				densities = c.computeDensities(this.getEstimatorAlldifferent(), this.getTools());
			} else if (c instanceof PropFastGCC) {
				densities = c.computeDensities(this.getEstimatorGCC(), this.getTools());
			} else {
				densities = c.computeDensities("", this.getTools());
			}
			if (densities == null) {
				order = null;
				return;
			}
			for (IntVarAssignment a : densities.keySet()) {
				if (sumDensities.containsKey(a)) {
					double s = sumDensities.get(a);
					int t = times.get(a);
					sumDensities.remove(a);
					times.remove(a);
					sumDensities.put(a, s + densities.get(a));
					times.put(a, 1 + t);
				} else {
					sumDensities.put(a, densities.get(a));
					times.put(a, 1);
				}
			}
		}

		BinaryHeap<ComparablePair<IntVarAssignment, Double>> heap = new BinaryHeap<ComparablePair<IntVarAssignment, Double>>();
		for (IntVarAssignment a : sumDensities.keySet()) {
			ComparablePair<IntVarAssignment, Double> pair = new ComparablePair<IntVarAssignment, Double>(a,
					sumDensities.get(a) / times.get(a));
		}

		// Sorted list of assignments in ascending order of densities
		List<ComparablePair<IntVarAssignment, Double>> listAssignments = heap.read();

		// We store the instantiations in the good order
		this.order = new IntVarAssignment[listAssignments.size()];
		for (int k = 0; k < this.order.length; k++) {
			this.order[k] = listAssignments.get(this.order.length - k - 1).getItem();
		}
	}

}
