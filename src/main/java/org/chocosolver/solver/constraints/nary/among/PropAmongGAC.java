/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.among;

import gnu.trove.set.hash.TIntHashSet;
import org.chocosolver.memory.IEnvironment;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.Countable;
import org.chocosolver.solver.search.strategy.countingbased.CountingEstimators;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.testng.reporters.jq.Main;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Incremental propagator for Among Constraint: Counts the number of decision
 * variables which take a value in the input value set GCCAT: NVAR is the number
 * of variables of the collection VARIABLES that take their value in VALUES.
 * <br/>
 * <a href="http://www.emn.fr/x-info/sdemasse/gccat/Camong.html">gccat among</a>
 * <br/>
 *
 * @author Jean-Guillaume Fages
 * @since 8/02/14
 */
public class PropAmongGAC extends Propagator<IntVar> implements Countable {

	// ***********************************************************************************
	// VARIABLES
	// ***********************************************************************************

	private final int nb_vars; // number of decision variables (excludes the
								// cardinality variable)
	private final int[] values; // value set (array)
	private TIntHashSet setValues; // value set (set)
	private ISet poss; // variable set possibly assigned to a value in the value
						// set
	private IStateInt nbSure; // number of variables that are assigned to such
								// value for sure

	// ***********************************************************************************
	// CONSTRUCTOR
	// ***********************************************************************************

	/**
	 * Creates a propagator for Among: Counts the number of decision variables
	 * which take a value in the input value set
	 *
	 * @param variables
	 *            {decision variables, cardinality variable}
	 * @param values
	 *            input value set
	 */
	public PropAmongGAC(IntVar[] variables, int[] values) {
		super(variables, PropagatorPriority.LINEAR, true);
		nb_vars = variables.length - 1;
		IEnvironment environment = model.getEnvironment();
		this.setValues = new TIntHashSet(values);
		this.values = setValues.toArray();
		Arrays.sort(this.values);
		poss = SetFactory.makeStoredSet(SetType.BIPARTITESET, 0, model);
		nbSure = environment.makeInt(0);
	}

	// ***********************************************************************************
	// METHODS
	// ***********************************************************************************

	@Override
	public int getPropagationConditions(int idx) {
		if (idx == nb_vars) {
			return IntEventType.boundAndInst();
		}
		return IntEventType.all();
	}

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if (PropagatorEventType.isFullPropagation(evtmask)) {
			poss.clear();
			int nbMandForSure = 0;
			for (int i = 0; i < nb_vars; i++) {
				IntVar var = vars[i];
				int nb = 0;
				for (int j : values) {
					if (var.contains(j)) {
						nb++;
					}
				}
				if (nb == var.getDomainSize()) {
					nbMandForSure++;
				} else if (nb > 0) {
					poss.add(i);
				}
			}
			nbSure.set(nbMandForSure);
		}
		filter();
	}

	@Override
	public void propagate(int vidx, int evtmask) throws ContradictionException {
		if (vidx != nb_vars && poss.contains(vidx)) {
			IntVar var = vars[vidx];
			int nb = 0;
			for (int j : values) {
				if (var.contains(j)) {
					nb++;
				}
			}
			if (nb == var.getDomainSize()) {
				nbSure.add(1);
				poss.remove(vidx);
				vars[nb_vars].updateLowerBound(nbSure.get(), this);
			} else if (nb == 0) {
				poss.remove(vidx);
				vars[nb_vars].updateUpperBound(poss.size() + nbSure.get(), this);
			}
		}
		forcePropagate(PropagatorEventType.CUSTOM_PROPAGATION);
	}

	private void filter() throws ContradictionException {
		int lb = nbSure.get();
		int ub = poss.size() + lb;
		vars[nb_vars].updateBounds(lb, ub, this);
		if (vars[nb_vars].isInstantiated() && lb < ub) {
			if (vars[nb_vars].getValue() == lb) {
				backPropRemPoss();
			} else if (vars[nb_vars].getValue() == ub) {
				backPropForcePoss();
			}
		}
	}

	private void backPropRemPoss() throws ContradictionException {
		ISetIterator iter = poss.iterator();
		while (iter.hasNext()) {
			int i = iter.nextInt();
			IntVar v = vars[i];
			if (v.hasEnumeratedDomain()) {
				for (int value : values) {
					v.removeValue(value, this);
				}
				poss.remove(i);
			} else {
				int newLB = v.getLB();
				int newUB = v.getUB();
				for (int val = v.getLB(); val <= newUB; val = v.nextValue(val)) {
					if (setValues.contains(val)) {
						newLB = val + 1;
					} else {
						break;
					}
				}
				for (int val = newUB; val >= newLB; val = v.previousValue(val)) {
					if (setValues.contains(val)) {
						newUB = val - 1;
					} else {
						break;
					}
				}
				v.updateBounds(newLB, newUB, this);
				if (newLB > values[values.length - 1] || newUB < values[0]) {
					poss.remove(i);
				}
			}
		}
	}

	private void backPropForcePoss() throws ContradictionException {
		ISetIterator iter = poss.iterator();
		while (iter.hasNext()) {
			int i = iter.nextInt();
			IntVar v = vars[i];
			if (v.hasEnumeratedDomain()) {
				for (int val = v.getLB(); val <= v.getUB(); val = v.nextValue(val)) {
					if (!setValues.contains(val)) {
						v.removeValue(val, this);
					}
				}
				poss.remove(i);
				nbSure.add(1);
			} else {
				v.updateBounds(values[0], values[values.length - 1], this);
				int newLB = v.getLB();
				int newUB = v.getUB();
				for (int val = v.getLB(); val <= newUB; val = v.nextValue(val)) {
					if (!setValues.contains(val)) {
						newLB = val + 1;
					} else {
						break;
					}
				}
				for (int val = newUB; val >= newLB; val = v.previousValue(val)) {
					if (!setValues.contains(val)) {
						newUB = val - 1;
					} else {
						break;
					}
				}
				v.updateBounds(newLB, newUB, this);
				if (v.isInstantiated()) {
					poss.remove(i);
					nbSure.add(1);
				}
			}
		}
	}

	@Override
	public ESat isEntailed() {
		int min = 0;
		int max = 0;
		int nbInst = vars[nb_vars].isInstantiated() ? 1 : 0;
		for (int i = 0; i < nb_vars; i++) {
			IntVar var = vars[i];
			if (var.isInstantiated()) {
				nbInst++;
				if (setValues.contains(var.getValue())) {
					min++;
					max++;
				}
			} else {
				int nb = 0;
				for (int j : values) {
					if (var.contains(j)) {
						nb++;
					}
				}
				if (nb == var.getDomainSize()) {
					min++;
					max++;
				} else if (nb > 0) {
					max++;
				}
			}
		}
		if (min > vars[nb_vars].getUB() || max < vars[nb_vars].getLB()) {
			return ESat.FALSE;
		}
		if (nbInst == nb_vars + 1) {
			return ESat.TRUE;
		}
		return ESat.UNDEFINED;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("AMONG(");
		sb.append("[");
		for (int i = 0; i < nb_vars; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(vars[i].toString());
		}
		sb.append("],{");
		sb.append(Arrays.toString(values));
		sb.append("},");
		sb.append(vars[nb_vars].toString()).append(")");
		return sb.toString();
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
		for (int i = 0; i < nb_vars; i++) {

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
				// that we are sure that they take their values in values or
				// outside values. In that case the solution density is
				// 1/var.getDomainSize()
				int nbIn = 0;
				for (int val = var.getLB(); val <= var.getUB(); val = var.nextValue(val)) {
					if (setValues.contains(val)) {
						nbIn++;
					}
				}
				if (nbIn == var.getDomainSize() || nbIn == 0) {
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
			// and remaining values inside "values" that are still in some
			// domains

			// Nb of remaining variables (that are not instantiated)
			int n = 0;

			// Y
			Set<Integer> remainingValueSet = new HashSet<Integer>();

			// Nb of edges
			int sumDomainSize = 0;
			
			// Card taking into account already instantiated variables
			int alreadytaken = 0;

			for (int i = 0; i < nb_vars; i++) {
				IntVar x = this.getVar(i);
				if (!x.isInstantiated()) {
					n++;
					sumDomainSize += x.getDomainSize();
					for (int y = x.getLB(); y <= x.getUB(); y = x.nextValue(y)) {
						remainingValueSet.add(y);
					}
				} else if(setValues.contains(x.getValue())){
					alreadytaken ++;
				}
			}

			// Size of Y
			int m = remainingValueSet.size();
			// Edge density
			double p = sumDomainSize * 1.0 / (n * m);

			// Size of Y'
			int mprime = 0;
			for (Integer y : remainingValueSet) {
				if (setValues.contains(y)) {
					mprime++;
				}
			}

			// We compute #among for each possible cardinality N and we sum it
			// (disjoint sets of solutions for each N)
			int sum = 0;
			IntVar cardVar = this.getVar(nb_vars);
			for (int card = cardVar.getLB(); card <= cardVar.getUB(); card = cardVar.nextValue(card)) {
				int N = card-alreadytaken;
				sum += tools.computebinomCoeff(n, N) * Math.pow(mprime, N) * Math.pow(m - mprime, n - N);
			}

			// We compute the estimation according to Erdos-Renyi
			double estim = sum * Math.pow(p, n);

			return estim;
		}
	}
	

}
