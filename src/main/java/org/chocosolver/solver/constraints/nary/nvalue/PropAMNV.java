/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.nvalue;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ConstraintsName;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.graph.G;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.mis.F;
import org.chocosolver.solver.constraints.nary.nvalue.amnv.rules.R;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.Countable;
import org.chocosolver.solver.search.strategy.countingbased.CountingEstimators;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;

import static org.chocosolver.solver.constraints.PropagatorPriority.CUBIC;
import static org.chocosolver.util.tools.ArrayUtils.concat;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Propagator for the atMostNValues constraint The number of distinct values in
 * the set of variables vars is at most equal to nValues
 *
 * @author Jean-Guillaume Fages
 * @since 01/01/2014
 */
public class PropAMNV extends Propagator<IntVar> implements Countable {

	// ***********************************************************************************
	// VARIABLES
	// ***********************************************************************************

	private G graph;
	private F heur;
	private R[] rules;

	// ***********************************************************************************
	// CONSTRUCTORS
	// ***********************************************************************************

	/**
	 * Creates a propagator for the atMostNValues constraint The number of
	 * distinct values in X is at most equal to N
	 */
	public PropAMNV(IntVar[] X, IntVar N, G graph, F heur, R[] rules) {
		super(concat(X, N), CUBIC, true);
		this.graph = graph;
		this.heur = heur;
		this.rules = rules;
		graph.build();
	}

	// ***********************************************************************************
	// ALGORITHMS
	// ***********************************************************************************

	@Override
	public int getPropagationConditions(int i) {
		return IntEventType.all();
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if (PropagatorEventType.isFullPropagation(evtmask)) {
			graph.update();
		}
		heur.prepare();
		do {
			heur.computeMIS();
			for (R rule : rules) {
				rule.filter(vars, graph, heur, this);
			}
		} while (heur.hasNextMIS());
	}

	@Override
	public void propagate(int idxVarInProp, int mask) throws ContradictionException {
		if (idxVarInProp < vars.length - 1) {
			graph.update(idxVarInProp);
		}
		forcePropagate(PropagatorEventType.CUSTOM_PROPAGATION);
	}

	// ***********************************************************************************
	// INFO
	// ***********************************************************************************

	@Override
	public ESat isEntailed() {
		// this is only a redundant propagator (solution checking uses the
		// default NValue propagator)
		return ESat.TRUE;
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

				// If we compute an estimation that is too big (infinity, we do
				// not consider every instantiations densisties associated to
				// the current variable var
				boolean haveEncounteredInfinity = false;

				for (int val = var.getLB(); val <= var.getUB(); val = var.nextValue(val)) {

					if (!haveEncounteredInfinity) {

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

							// We compute an estimation of the number of
							// remaining
							// solutions and we update total and varMap
							double estimNbRemainingSolutions = estimateNbSolutions(estimator, tools);
							if (Double.isInfinite(estimNbRemainingSolutions)) {
								haveEncounteredInfinity = true;
							} else {
								varMap.put(new IntVarAssignment(var, val), estimNbRemainingSolutions);
								total += estimNbRemainingSolutions;
							}

						} catch (ContradictionException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						// We delete the Event Queue in the propafation engine
						// and
						// we backtrack
						this.getModel().getSolver().getEngine().ignoreModifications();
						this.getModel().getEnvironment().worldPop();

						if (!haveEncounteredInfinity) {
							// We put into map the solution densities for var
							for (IntVarAssignment assignment : varMap.keySet()) {
								map.put(assignment, varMap.get(assignment) / total);
							}
						}
					}

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
	 * @throws IOException
	 *             if the estimator is not defined
	 */
	public double estimateNbSolutions(String estimator, CountingTools tools) throws IOException {

		if (this.isCompletelyInstantiated()) {
			return 1.0;
		} else {
			// We have to consider every variable (even instantiated variables)
			// because, we cannot deal with the remaining number of values
			// allowed this way.
			// Though, we can only get the remaining values.
			Set<Integer> remainingValueSet = new HashSet<Integer>();
			int n = this.getNbVars() - 1;// -1 because the last variable is N
			for (int i = 0; i < n; i++) {
				IntVar x = this.getVar(i);
				for (int y = x.getLB(); y <= x.getUB(); y = x.nextValue(y)) {
					remainingValueSet.add(y);
				}
			}
			int m = remainingValueSet.size();

			// We compute the estimation depending on estimator
			double estim = 1.0;
			int sumDomain = 0;
			for (int i = 0; i < n; i++) {
				IntVar x = this.getVar(i);
				sumDomain += x.getDomainSize();
			}
			double p = sumDomain / (1.0 * n * m);
			
			// Cardinality variable N
			IntVar N = this.getVar(n);
			int sum=0;
			switch(this.getConstraint().getName()){
			case ConstraintsName.NVALUES: 
				for(int card = N.getLB(); card<=N.getUB(); card = N.nextValue(card)){
					sum+=tools.computebinomCoeff(m, card)*tools.computetriangleCoef(n, card);
				}
				break;
			default:
				for(int card = 1; card<=N.getUB(); card++){
					sum+=tools.computebinomCoeff(m, card)*tools.computetriangleCoef(n, card);
				}
			}
			
			estim = sum*Math.pow(p, n);

			return estim;
		}

	}

}
