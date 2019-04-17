/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.count;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.search.strategy.countingbased.Countable;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;

import static org.chocosolver.solver.constraints.PropagatorPriority.LINEAR;
import static org.chocosolver.util.tools.ArrayUtils.concat;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Define a COUNT constraint setting size{forall v in lvars | v = occval} =
 * occVar assumes the occVar variable to be the last of the variables of the
 * constraint: vars = [lvars | occVar] Arc Consistent algorithm with lvars =
 * list of variables for which the occurrence of occval in their domain is
 * constrained <br/>
 *
 * @author Jean-Guillaume Fages
 */
public class PropCount_AC extends Propagator<IntVar> implements Countable{

	// ***********************************************************************************
	// VARIABLES
	// ***********************************************************************************

	private int n;
	private int value;
	private ISet possibles, mandatories;

	// ***********************************************************************************
	// CONSTRUCTORS
	// ***********************************************************************************

	/**
	 * Propagator for Count Constraint for integer variables Performs Arc
	 * Consistency
	 *
	 * @param decvars
	 *            array of integer variables
	 * @param restrictedValue
	 *            int
	 * @param valueCardinality
	 *            integer variable
	 */
	public PropCount_AC(IntVar[] decvars, int restrictedValue, IntVar valueCardinality) {
		super(concat(decvars, valueCardinality), LINEAR, true);
		this.value = restrictedValue;
		this.n = decvars.length;
		this.possibles = SetFactory.makeStoredSet(SetType.BITSET, 0, model);
		this.mandatories = SetFactory.makeStoredSet(SetType.BITSET, 0, model);
	}

	@Override
	public String toString() {
		StringBuilder st = new StringBuilder();
		st.append("PropFastCount_(");
		int i = 0;
		for (; i < Math.min(4, vars.length - 1); i++) {
			st.append(vars[i].getName()).append(", ");
		}
		if (i < vars.length - 2) {
			st.append("..., ");
		}
		st.append("limit=").append(vars[vars.length - 1].getName());
		st.append(", value=").append(value).append(')');
		return st.toString();
	}

	// ***********************************************************************************
	// PROPAGATION
	// ***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if (PropagatorEventType.isFullPropagation(evtmask)) {// initialization
			mandatories.clear();
			possibles.clear();
			for (int i = 0; i < n; i++) {
				IntVar v = vars[i];
				int ub = v.getUB();
				if (v.isInstantiated()) {
					if (ub == value) {
						mandatories.add(i);
					}
				} else {
					if (v.contains(value)) {
						possibles.add(i);
					}
				}
			}
		}
		filter();
	}

	@Override
	public void propagate(int varIdx, int mask) throws ContradictionException {
		if (varIdx < n) {
			if (possibles.contains(varIdx)) {
				if (!vars[varIdx].contains(value)) {
					possibles.remove(varIdx);
					filter();
				} else if (vars[varIdx].isInstantiated()) {
					possibles.remove(varIdx);
					mandatories.add(varIdx);
					filter();
				}
			}
		} else {
			filter();
		}
	}

	private void filter() throws ContradictionException {
		vars[n].updateBounds(mandatories.size(), mandatories.size() + possibles.size(), this);
		if (vars[n].isInstantiated()) {
			int nb = vars[n].getValue();
			if (possibles.size() + mandatories.size() == nb) {
				ISetIterator iter = possibles.iterator();
				while (iter.hasNext()) {
					vars[iter.nextInt()].instantiateTo(value, this);
				}
				setPassive();
			} else if (mandatories.size() == nb) {
				ISetIterator iter = possibles.iterator();
				while (iter.hasNext()) {
					int j = iter.nextInt();
					if (vars[j].removeValue(value, this)) {
						possibles.remove(j);
					}
				}
				if (possibles.isEmpty()) {
					setPassive();
				}
			}
		}
	}

	// ***********************************************************************************
	// INFO
	// ***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx == vars.length - 1) {// cardinality variables
			return IntEventType.boundAndInst();
		}
		return IntEventType.all();
	}

	@Override
	public ESat isEntailed() {
		int min = 0;
		int max = 0;
		IntVar v;
		for (int i = 0; i < n; i++) {
			v = vars[i];
			if (v.isInstantiatedTo(value)) {
				min++;
				max++;
			} else {
				if (v.contains(value)) {
					max++;
				}
			}
		}
		if (vars[n].getLB() > max || vars[n].getUB() < min) {
			return ESat.FALSE;
		}
		if (!(vars[n].isInstantiated() && max == min)) {
			return ESat.UNDEFINED;
		}
		return ESat.TRUE;
	}

	@Override
	public boolean why(RuleStore ruleStore, IntVar var, IEventType evt, int value) {
		boolean nrules = ruleStore.addPropagatorActivationRule(this);
		if (var == vars[n]) {
			boolean isDecUpp = evt == IntEventType.DECUPP;
			for (int i = 0; i < n; i++) {
				if (vars[i].contains(value)) {
					if (vars[i].isInstantiated()) {
						nrules |= ruleStore.addFullDomainRule(vars[i]);
					}
				} else if (isDecUpp) {
					nrules |= ruleStore.addRemovalRule(vars[i], value);
				}
			}
		} else {
			nrules |= ruleStore.addBoundsRule(vars[n]);
			if (evt == IntEventType.REMOVE) {
				for (int i = 0; i < n; i++) {
					if (vars[i].isInstantiatedTo(value)) {
						nrules |= ruleStore.addFullDomainRule(vars[i]);
					}
				}
			} else {
				for (int i = 0; i < n; i++) {
					nrules |= ruleStore.addFullDomainRule(vars[i]);
				}
			}
		}
		return nrules;
	}

	// ***********************************************************************************
	// COUNTING ALGORITHM
	// ***********************************************************************************

	// si var est surement en dehors ou surement dedans --> 1/domainSize
	// sinon, on instancie, on progpage et on compte
	// -> quand on compte apres cette propagation, reduction problème ? -> farie
	// tests

	@Override
	public Map<IntVarAssignment, Double> computeDensities(String estimator, CountingTools tools) {
		// TODO Auto-generated method stub
		// Map containing the solution densities for each possible assignment
		// variable/value.
		Map<IntVarAssignment, Double> map = new HashMap<IntVarAssignment, Double>();

		// We try every possible assignment and we propagate. From the resulting
		// state of the model, we estimate the number of remaining solutions.

		// The last variable in this.getVars() is the cardinality variable that
		// we do no test.
		for (int i = 0; i < n; i++) {

			IntVar var = this.getVar(i);

			if (!var.isInstantiated()) {

				// Map containing every estimation of number of remaining tuples
				// for each possible assignment for var.
				Map<IntVarAssignment, Double> varMap = new HashMap<IntVarAssignment, Double>();

				// total stores the total number of remaining solution, which is
				// the sum of every estimation made on var.
				double total = 0;

				// If we compute an estimation that is too big (infinity, we do
				// not consider every instantiations densisties associated to
				// the current variable var
				boolean haveEncounteredInfinity = false;

				// We do not need to instantiate and propagate for variables
				// that have no influence on next filtering, that are variables
				// that we are sure that they cannot take the value "value" In
				// that case the solution density is 1/var.getDomainSize()
				
				if (!var.contains(value)) {
					for (int val = var.getLB(); val <= var.getUB(); val = var.nextValue(val)) {
						map.put(new IntVarAssignment(var, val), 1.0 / var.getDomainSize());
					}
				} else {
					// else, we instantiate and propagate
					for (int val = var.getLB(); val <= var.getUB(); val = var.nextValue(val)) {

						if (!haveEncounteredInfinity) {

							// We save the state of the solver, so we can
							// backtrack
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

							// We delete the Event Queue in the propafation
							// engine
							// and we backtrack
							this.getModel().getSolver().getEngine().ignoreModifications();
							this.getModel().getEnvironment().worldPop();

							if (!haveEncounteredInfinity) {
								// We put into map the solution densities for
								// var
								for (IntVarAssignment assignment : varMap.keySet()) {
									map.put(assignment, varMap.get(assignment) / total);
								}
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

			// We count the number of remaining variables and values

			// Nb of remaining variables (that are not instantiated)
			int remVars = 0;

			// Y
			Set<Integer> remainingValueSet = new HashSet<Integer>();

			// Nb of edges
			int sumDomainSize = 0;

			// Card taking into account already instantiated variables
			int alreadytaken = 0;

			for (int i = 0; i < n; i++) {
				IntVar x = this.getVar(i);
				if (!x.isInstantiated()) {
					remVars++;
					sumDomainSize += x.getDomainSize();
					for (int y = x.getLB(); y <= x.getUB(); y = x.nextValue(y)) {
						remainingValueSet.add(y);
					}
				} else if (x.getValue()==value) {
					alreadytaken++;
				}
			}

			// Size of Y
			int m = remainingValueSet.size();
			// Edge density
			double p = sumDomainSize * 1.0 / (remVars * m);


			// We compute #count for each possible cardinality N and we sum it
			// (disjoint sets of solutions for each N)
			int sum = 0;
			IntVar cardVar = this.getVar(n);
			for (int card = cardVar.getLB(); card <= cardVar.getUB(); card = cardVar.nextValue(card)) {
				int N = card - alreadytaken;
				sum += tools.computebinomCoeff(remVars, N)*Math.pow(m-1, remVars-N);
			}

			// We compute the estimation according to Erdos-Renyi
			double estim = sum * Math.pow(p, remVars);

			return estim;
		}
	}

}
