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
import java.util.List;
import java.util.Map;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffAC;
import org.chocosolver.solver.constraints.nary.globalcardinality.PropFastGCC;
import org.chocosolver.solver.search.strategy.countingbased.tools.BinaryHeap;
import org.chocosolver.solver.search.strategy.countingbased.tools.ComparablePair;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;

public class MinSD extends CountingBasedStrategy {

	public MinSD(Model model) {
		super(model);
		// TODO Auto-generated constructor stub
	}
	
	public MinSD(Model model, String updateOption){
		super(model);
		this.setUpdateOption(updateOption);
	}

	@Override
	public void computeOrder() {
		// TODO Auto-generated method stub

		// In this binary heap, we sort every computed densities. We deal with
		// these duplicates, that references a same assigment after.
		BinaryHeap<ComparablePair<IntVarAssignment, Double>> heap = new BinaryHeap<ComparablePair<IntVarAssignment, Double>>();

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
				ComparablePair<IntVarAssignment, Double> pair = new ComparablePair<IntVarAssignment, Double>(a,
						densities.get(a));
				heap.add(pair);
			}
		}

		// Sorted list of assignments in ascending order of densities
		List<ComparablePair<IntVarAssignment, Double>> listAssignments = heap.read();

		// In this list, we store the assignments that have already been dealt
		// with
		List<IntVarAssignment> alreadyStored = new ArrayList<IntVarAssignment>();

		// We start from the beginning to get the smallest densities first
		for (int k = 0; k < listAssignments.size(); k++) {
			ComparablePair<IntVarAssignment, Double> pair = listAssignments.get(k);
			if (!alreadyStored.contains(pair.getItem())) {
				alreadyStored.add(pair.getItem());
			} else {
				listAssignments.remove(k);
			}
		}

		// We store the instantiations in the good order
		this.order = new IntVarAssignment[listAssignments.size()];
		for (int k = 0; k < this.order.length; k++) {
			this.order[k] = listAssignments.get(k).getItem();
		}

	}

}
