/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.globalcardinality;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.Countable;
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
import org.chocosolver.util.tools.ArrayUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Propagator for Global Cardinality Constraint (GCC) for integer variables
 * Basic filter: no particular consistency but fast and with a correct checker
 *
 * @author Jean-Guillaume Fages
 */
public class PropFastGCC extends Propagator<IntVar> implements Countable {

	// ***********************************************************************************
	// VARIABLES
	// ***********************************************************************************

	private int n, n2;
	private int[] values;
	private ISet[] possibles, mandatories;
	private ISet valueToCompute;
	private TIntIntHashMap map;
	private TIntArrayList boundVar;

	// ***********************************************************************************
	// CONSTRUCTORS
	// ***********************************************************************************

	/**
	 * Propagator for Global Cardinality Constraint (GCC) for integer variables
	 * Basic filter: no particular consistency but fast and with a correct
	 * checker
	 *
	 * @param decvars
	 *            array of integer variables
	 * @param restrictedValues
	 *            array of int
	 * @param map
	 *            mapping
	 * @param valueCardinalities
	 *            array of integer variables
	 */
	public PropFastGCC(IntVar[] decvars, int[] restrictedValues, TIntIntHashMap map, IntVar[] valueCardinalities) {
		super(ArrayUtils.append(decvars, valueCardinalities), PropagatorPriority.LINEAR, false);
		if (restrictedValues.length != valueCardinalities.length) {
			throw new UnsupportedOperationException();
		}
		this.values = restrictedValues;
		this.n = decvars.length;
		this.n2 = values.length;
		this.possibles = new ISet[n2];
		this.mandatories = new ISet[n2];
		this.map = map;
		for (int idx = 0; idx < n2; idx++) {
			mandatories[idx] = SetFactory.makeStoredSet(SetType.BITSET, 0, model);
			possibles[idx] = SetFactory.makeStoredSet(SetType.BITSET, 0, model);
		}
		this.valueToCompute = SetFactory.makeStoredSet(SetType.BITSET, 0, model);
		this.boundVar = new TIntArrayList();
		for (int i = 0; i < n; i++) {
			if (!vars[i].hasEnumeratedDomain()) {
				boundVar.add(i);
			}
		}
	}

	// ***********************************************************************************
	// PROPAGATION
	// ***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		valueToCompute.clear();
		for (int i = 0; i < n2; i++) {
			mandatories[i].clear();
			possibles[i].clear();
			valueToCompute.add(i);
		}
		for (int i = 0; i < n; i++) {
			IntVar v = vars[i];
			int ub = v.getUB();
			if (v.isInstantiated()) {
				if (map.containsKey(v.getValue())) {
					int j = map.get(v.getValue());
					mandatories[j].add(i);
				}
			} else {
				for (int k = v.getLB(); k <= ub; k = v.nextValue(k)) {
					if (map.containsKey(k)) {
						int j = map.get(k);
						possibles[j].add(i);
					}
				}
			}
		}
		while (filter()) {
			ISetIterator valIt = valueToCompute.iterator();
			while (valIt.hasNext()) {
				int i = valIt.nextInt();
				ISetIterator varIt = possibles[i].iterator();
				while (varIt.hasNext()) {
					int var = varIt.nextInt();
					if (!vars[var].contains(values[i])) {
						possibles[i].remove(var);
					} else if (vars[var].isInstantiated()) {
						possibles[i].remove(var);
						mandatories[i].add(var);
					}
				}
			}
		}
	}

	private boolean filter() throws ContradictionException {
		boolean again = false;
		Iterator<Integer> iter = valueToCompute.iterator();
		while (iter.hasNext()) {
			int i = iter.next();
			again |= vars[n + i].updateLowerBound(mandatories[i].size(), this);
			again |= vars[n + i].updateUpperBound(mandatories[i].size() + possibles[i].size(), this);
			if (vars[n + i].isInstantiated()) {
				if (possibles[i].size() + mandatories[i].size() == vars[n + i].getLB()) {
					ISetIterator possIt = possibles[i].iterator();
					while (possIt.hasNext()) {
						int j = possIt.nextInt();
						mandatories[i].add(j);
						again |= vars[j].instantiateTo(values[i], this);
					}
					possibles[i].clear();
					valueToCompute.remove(i);// value[i] restriction entailed
				} else if (mandatories[i].size() == vars[n + i].getUB()) {
					ISetIterator possIt = possibles[i].iterator();
					while (possIt.hasNext()) {
						again |= vars[possIt.nextInt()].removeValue(values[i], this);
					}
					possibles[i].clear();
					valueToCompute.remove(i);// value[i] restriction entailed
				}
			}
		}
		// manage holes in bounded variables
		if (boundVar.size() > 0) {
			again |= filterBounds();
		}
		return again;
	}

	private boolean filterBounds() throws ContradictionException {
		boolean useful = false;
		for (int i = 0; i < boundVar.size(); i++) {
			int var = boundVar.get(i);
			if (!vars[var].isInstantiated()) {
				int lb = vars[var].getLB();
				int index = -1;
				if (map.containsKey(lb)) {
					index = map.get(lb);
				}
				boolean b = index != -1 && !(possibles[index].contains(var) || mandatories[index].contains(var));
				while (b) {
					useful = true;
					vars[var].removeValue(lb, this);
					lb = vars[var].getLB();
					index = -1;
					if (map.containsKey(lb)) {
						index = map.get(lb);
					}
					b = index != -1 && !(possibles[index].contains(var) || mandatories[index].contains(var));
				}
				int ub = vars[var].getUB();
				index = -1;
				if (map.containsKey(ub)) {
					index = map.get(ub);
				}
				b = index != -1 && !(possibles[index].contains(var) || mandatories[index].contains(var));
				while (b) {
					useful = true;
					vars[var].removeValue(ub, this);
					ub = vars[var].getUB();
					index = -1;
					if (map.containsKey(ub)) {
						index = map.get(ub);
					}
					b = index != -1 && !(possibles[index].contains(var) || mandatories[index].contains(var));
				}
			} else {
				int val = vars[var].getValue();
				if (map.containsKey(val)) {
					int index = map.get(val);
					if ((!possibles[index].contains(var) && !mandatories[index].contains(var))) {
						fails(); // TODO: could be more precise, for explanation
									// purpose
					}
				}
			}
		}
		return useful;
	}

	// ***********************************************************************************
	// GETTERS
	// ***********************************************************************************

	public int getNbDecVars() {
		return n;
	}

	public int getNbValue() {
		return n2;
	}

	public int[] getValues() {
		return values;
	}

	// ***********************************************************************************
	// INFO
	// ***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		if (vIdx >= n) {// cardinality variables
			return IntEventType.boundAndInst();
		}
		return IntEventType.all();
	}

	@Override
	public ESat isEntailed() {
		int[] min = new int[n2];
		int[] max = new int[n2];
		int j, k, ub;
		IntVar v;
		for (int i = 0; i < n; i++) {
			v = vars[i];
			ub = v.getUB();
			if (v.isInstantiated()) {
				if (map.containsKey(v.getValue())) {
					j = map.get(v.getValue());
					min[j]++;
					max[j]++;
				}
			} else {
				for (k = v.getLB(); k <= ub; k = v.nextValue(k)) {
					if (map.containsKey(k)) {
						j = map.get(k);
						max[j]++;
					}
				}
			}
		}
		for (int i = 0; i < n2; i++) {
			if (vars[n + i].getLB() > max[i] || vars[n + i].getUB() < min[i]) {
				return ESat.FALSE;
			}
		}
		for (int i = 0; i < n2; i++) {
			if (!(vars[n + i].isInstantiated() && max[i] == min[i])) {
				return ESat.UNDEFINED;
			}
		}
		return ESat.TRUE;
	}

	@Override
	public String toString() {
		StringBuilder st = new StringBuilder();
		st.append("PropFastGCC_(");
		int i = 0;
		for (; i < Math.min(4, vars.length); i++) {
			st.append(vars[i].getName()).append(", ");
		}
		if (i < vars.length - 2) {
			st.append("...,");
		}
		st.append(vars[vars.length - 1].getName()).append(")");
		return st.toString();
	}

	// ***********************************************************************************
	// COUNTING
	// ***********************************************************************************

	@Override
	public Map<IntVarAssignment, Double> computeDensities(String estimator, CountingTools tools) {
		// TODO Auto-generated method stub
		// Map containing the solution densities for each possible assignment
		// variable/value.
		Map<IntVarAssignment, Double> map = new HashMap<IntVarAssignment, Double>();

		IntVar[] vars = Arrays.copyOf(this.getVars(), n);
		
		// We try every possible assignment and we propagate. From the resulting
		// state of the model, we estimate the number of remaining solutions.
		for (IntVar var : vars) {

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

	private double estimateNbSolutions(String estimator, CountingTools tools) {
		// TODO Auto-generated method stub
		return 0;
	}

}
