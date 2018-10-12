/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.alldifferent;

import java.util.HashMap;
import java.util.Map;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.constraints.nary.alldifferent.algo.AlgoAllDiffAC;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.Countable;
import org.chocosolver.solver.search.strategy.countingbased.CountingEstimators;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;

/**
 * Propagator for AllDifferent AC constraint for integer variables
 * <p/>
 * Uses Regin algorithm Runs in O(m.n) worst case time for the initial
 * propagation but has a good average behavior in practice
 * <p/>
 * Runs incrementally for maintaining a matching
 * <p/>
 *
 * @author Jean-Guillaume Fages
 */
public class PropAllDiffAC extends Propagator<IntVar> implements Countable {

	// ***********************************************************************************
	// VARIABLES
	// ***********************************************************************************

	protected AlgoAllDiffAC filter;

	// ***********************************************************************************
	// CONSTRUCTORS
	// ***********************************************************************************

	/**
	 * AllDifferent constraint for integer variables enables to control the
	 * cardinality of the matching
	 *
	 * @param variables
	 *            array of integer variables
	 */
	public PropAllDiffAC(IntVar[] variables) {
		super(variables, PropagatorPriority.QUADRATIC, false);
		this.filter = new AlgoAllDiffAC(variables, this);
	}

	// ***********************************************************************************
	// PROPAGATION
	// ***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		filter.propagate();
	}

	@Override
	public ESat isEntailed() {
		return ESat.TRUE; // redundant propagator (used with PropAllDiffInst)
	}

	// ***********************************************************************************
	// COUNTING ALGORITHM
	// ***********************************************************************************

	@Override
	public Map<IntVarAssignment, Double> computeDensities(String estimator) {
		// TODO Auto-generated method stub

		// Map containing the solution densities for each possible assignment
		// variable/value.
		Map<IntVarAssignment, Double> map = new HashMap<IntVarAssignment, Double>();

		// We try every possible assignment and we propagate. From the resulting
		// state of the model, we estimate the number of remaining solutions.
		for (IntVar var : this.getVars()) {

			if (!var.isInstantiated()) {

				// Map containing every estimation of number of solution for
				// each possible assignment for var.
				Map<IntVarAssignment, Double> varMap = new HashMap<IntVarAssignment, Double>();

				// total stores the total number of remaining solution, which
				// the sum of every estimation made on var.
				double total = 0;

				for (int val = var.getLB(); val <= var.getUB(); val = var.nextValue(val)) {

					// We save the state of the solver, so we can backtrack
					// after testing var<-val
					this.getModel().getEnvironment().worldPush();

					try {
						var.instantiateTo(val, Cause.Null);

						// We force GAC after the instantiation
						Constraint c = this.getConstraint();
						for (Propagator p : c.getPropagators()) {
							p.propagate(PropagatorEventType.FULL_PROPAGATION.getMask());
						}

						// We compute an estimation of the number of remaining
						// solutions and we update total and varMap
						double estimNbRemainingSolutions = estimateNbSolutions(estimator);
						varMap.put(new IntVarAssignment(var, val), estimNbRemainingSolutions);
						total += estimNbRemainingSolutions;

					} catch (ContradictionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// We delete the Event Queue in the propafation engine and
					// we backtrack
					this.getModel().getSolver().getEngine().ignoreModifications();
					this.getModel().getEnvironment().worldPop();

				}
				
				// We put into map the solution densities for var
				for (IntVarAssignment assignment : varMap.keySet()) {
					map.put(assignment, varMap.get(assignment) / total);
				}
			}
		}
		return map;
	}

	private double estimateNbSolutions(String estimator) {
		if(this.isCompletelyInstantiated()){
			return 1.0;
		} else {
			switch(estimator){
			case CountingEstimators.ALLDIFFERENT_PQZ : break;
			case CountingEstimators.ALLDIFFERENT_ER : break;
			case CountingEstimators.ALLDIFFERENT_FDS : break;
			default : break;
			}
		}
	}

}
