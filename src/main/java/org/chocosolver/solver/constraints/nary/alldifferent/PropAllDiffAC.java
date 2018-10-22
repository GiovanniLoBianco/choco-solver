/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.alldifferent;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.constraints.nary.alldifferent.algo.AlgoAllDiffAC;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.Countable;
import org.chocosolver.solver.search.strategy.countingbased.CountingEstimators;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
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
	public Map<IntVarAssignment, Double> computeDensities(String estimator, CountingTools tools) {
		// TODO Auto-generated method stub

		// Map containing the solution densities for each possible assignment
		// variable/value.
		Map<IntVarAssignment, Double> map = new HashMap<IntVarAssignment, Double>();

		// We try every possible assignment and we propagate. From the resulting
		// state of the model, we estimate the number of remaining solutions.
		for (IntVar var : this.getVars()) {

			if (!var.isInstantiated()) {

				// Map containing every estimation of number of remaining tuples
				// for each possible assignment for var.
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
						double estimNbRemainingSolutions = estimateNbSolutions(estimator, tools);
						varMap.put(new IntVarAssignment(var, val), estimNbRemainingSolutions);
						total += estimNbRemainingSolutions;

					} catch (ContradictionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
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

	/**
	 * 
	 * @param estimator
	 * @param tools
	 * @return an estimation of the number of remaining tupes for the current
	 *         alldifferent constraint
	 * @throws IOException if the estimator is not defined
	 */
	public double estimateNbSolutions(String estimator, CountingTools tools) throws IOException {

		if (this.isCompletelyInstantiated()) {
			return 1.0;
		} else {

			// We count the number of remaining variables and values
			// in order to know how many fake values we must add
			int nbRemainingVars = 0;
			Set<Integer> remainingValueSet = new HashSet<Integer>();
			for (IntVar x : vars) {
				if (!x.isInstantiated()) {
					nbRemainingVars++;
					for (int y = x.getLB(); y <= x.getUB(); y = x.nextValue(y)) {
						remainingValueSet.add(y);
					}
				}
			}
			int nbFakeVars = remainingValueSet.size() - nbRemainingVars;

			// We compute the estimation depending on estimator
			double estim = 1.0;
			switch (estimator) {
			case CountingEstimators.ALLDIFFERENT_PQZ:
				for (IntVar x : vars) {
					if (!x.isInstantiated()) {
						estim *= tools.computeBMFactors(x.getDomainSize());
					}
				}
				for (int k = 1; k <= nbFakeVars; k++) {
					estim *= tools.computeBMFactors(remainingValueSet.size()) / k;
				}
				break;
			case CountingEstimators.ALLDIFFERENT_ER:
				int sumDomain = 0;
				for (IntVar x : vars) {
					if (!x.isInstantiated()) {
						sumDomain += x.getDomainSize();
					}
				}
				double p = sumDomain / (1.0 * nbRemainingVars * remainingValueSet.size());
				for (int k = nbFakeVars + 1; k <= remainingValueSet.size(); k++) {
					estim *= p * k;
				}
				break;
			case CountingEstimators.ALLDIFFERENT_FDS:
				int k = nbFakeVars + 1;
				for (IntVar x : vars) {
					if (!x.isInstantiated()) {
						estim *= x.getDomainSize() * k * 1.0 / remainingValueSet.size();
						k++;
					}
				}
				break;
			default:
				throw new IOException("Estimator undefined");
			}
			return estim;
		}
	}

}
