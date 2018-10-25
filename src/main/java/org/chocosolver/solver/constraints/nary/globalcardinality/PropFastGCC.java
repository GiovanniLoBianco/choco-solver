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
import org.chocosolver.solver.search.strategy.countingbased.CountingEstimators;
import org.chocosolver.solver.search.strategy.countingbased.tools.BinaryHeap;
import org.chocosolver.solver.search.strategy.countingbased.tools.ComparablePair;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

				// If any instantiations of var lead to 0 solutions, then the
				// constraint cannot be satisfied.
				if (total == 0) {
					return null;
				}

				// We put into map the solution densities for var
				for (IntVarAssignment assignment : varMap.keySet()) {
					map.put(assignment, varMap.get(assignment) / total);
				}
			}
		}
		return map;
	}

	public double estimateNbSolutions(String estimator, CountingTools tools) {
		// TODO Auto-generated method stub

		// Creation of array of variables of the problem
		IntVar[] vars = Arrays.copyOf(this.getVars(), n);
		
	/*	System.out.println("-----------------------------------------------");
		for(int i=0; i<this.getVars().length; i++){
			System.out.println(this.getVar(i));
			if(i==n-1){
				System.out.println("----");
			}
		}*/

		// Creation of the bitset of values of the union of the domains and
		// correspoding lower and upper bounds
		int minValue = vars[0].getLB();
		int maxValue = vars[0].getUB();
		for (int i = 1; i < n; i++) {
			minValue = Math.min(vars[i].getLB(), minValue);
			maxValue = Math.max(vars[i].getUB(), maxValue);
		}

		boolean[] valuesBitSet = new boolean[maxValue - minValue + 1];
		int[] l = new int[maxValue - minValue + 1];
		int[] u = new int[maxValue - minValue + 1];
		for (int i = 0; i < n; i++) {
			for (int y = vars[i].getLB(); y <= vars[i].getUB(); y = vars[i].nextValue(y)) {
				valuesBitSet[y - minValue] = true;
				u[y - minValue]++;// For now l[y]=0 and u[y]=nbNeighbors
			}
		}
		// For the constrained values, we input the right bounds
		for (int j = 0; j < values.length; j++) {
			int val = values[j];
			// We do not consider value for values if any domain contains it
			if (val >= minValue && val <= maxValue) {
				l[val - minValue] = this.getVar(n + j).getLB();
				u[val - minValue] = this.getVar(n + j).getUB();
			}
		}

		// We deal with fixed variables
		for (int i = 0; i < n; i++) {
			if (vars[i].isInstantiated()) {
				int val = vars[i].getValue();
				l[val - minValue]--;
				if (l[val - minValue] < 0) {
					l[val - minValue] = 0;
				}
				u[val - minValue]--;
			}
		}

		// Number of values in the Lower Bound Graph
		int nbValueLBG = 0;
		for (int j = 0; j < values.length; j++) {
			int val = values[j];
			if (val >= minValue && val <= maxValue && l[val - minValue] > 0) {
				nbValueLBG+=l[val - minValue];
			}
		}

		return estimateLowerBound(vars, l, minValue, nbValueLBG, tools)
				* estimateResidualUpperBound(vars, l, u, minValue, nbValueLBG, estimator, tools);
	}

	/**
	 * 
	 * @param vars
	 * @param l
	 * @param minValue
	 * @param nbValueLBG
	 * @param tools
	 * @return an estimation of the number of perfect matching in the Lower Bound Graph
	 */
	private double estimateLowerBound(IntVar[] vars, int[] l, int minValue, int nbValueLBG, CountingTools tools) {
		// TODO Auto-generated method stub

		// We compute a list of the number of neighbors for each variables that
		// are in the Lower Bound Graph
		ArrayList<Integer> listNbNeighbors = new ArrayList<Integer>();
		for (int i = 0; i < vars.length; i++) {
			if (!vars[i].isInstantiated()) {
				int nbNeighbors = 0;
				for (int y = vars[i].getLB(); y <= vars[i].getUB(); y = vars[i].nextValue(y)) {
					nbNeighbors += l[y - minValue];
				}
				if (nbNeighbors > 0) {
					listNbNeighbors.add(nbNeighbors);
				}
			}
		}

		// We compute the list of the factors for every value (and fake value).
		// There must be as many elements in listNbNeighbors as in
		// listRightFactors.
		int nbFakeNodes = listNbNeighbors.size() - nbValueLBG;
		if(nbFakeNodes<0){
			return 0;
		}
		ArrayList<Integer> listRightFactors = new ArrayList<Integer>();
		for (int j = 0; j < l.length; j++) {
			for (int k = 1; k <= l[j]; k++) {
				listRightFactors.add(k);
			}
		}
		for (int k = 1; k <= nbFakeNodes; k++) {
			listRightFactors.add(k);
		}

		// We compute the product of every Bregman-Minc factors, dealing one by
		// one with each symmetry, such that we do not have to compute numbers
		// that are too big
		double estim = 1.0;
		for (int k = 0; k < listNbNeighbors.size(); k++) {
			estim *= tools.computeBMFactors(listNbNeighbors.get(k) + nbFakeNodes) / listRightFactors.get(k);
		}
		

		return estim;
	}

	/**
	 * 
	 * @param vars
	 * @param l
	 * @param u
	 * @param minValue
	 * @param nbValueLBG
	 * @param estimator
	 * @param tools
	 * @return an estimation of the number of perfect matching in the Residual Upper Bound Graph
	 */
	private double estimateResidualUpperBound(IntVar[] vars, int[] l, int[] u, int minValue, int nbValueLBG,
			String estimator, CountingTools tools) {
		// TODO Auto-generated method stub

		// We store the variables from the Upper Bound Graph and their degrees
		// in a binary heap, so we can sort them
		BinaryHeap<ComparablePair<IntVar, Integer>> heap = new BinaryHeap<ComparablePair<IntVar, Integer>>();
		for (int i = 0; i < vars.length; i++) {
			if (!vars[i].isInstantiated()) {
				int nbNeighbors = 0;
				for (int y = vars[i].getLB(); y <= vars[i].getUB(); y = vars[i].nextValue(y)) {
					nbNeighbors += u[y - minValue] - l[y - minValue];
				}
				if (nbNeighbors > 0) {
					heap.add(new ComparablePair<IntVar, Integer>(vars[i], nbNeighbors));
				}
			}
		}

		// We sort the variables by their degree in ascending order
		List<ComparablePair<IntVar, Integer>> orderedListDegrees = heap.read();

		// We store the variables that are in the residual upper bound graph and
		// their degrees
		ArrayList<IntVar> residualVars = new ArrayList<IntVar>();
		ArrayList<Integer> residualDegrees = new ArrayList<Integer>();
		for (int i = nbValueLBG; i < orderedListDegrees.size(); i++) {
			residualVars.add(orderedListDegrees.get(i).getItem());
			residualDegrees.add(orderedListDegrees.get(i).getComparable());
		}

		// We count the number of value nodes in the Residual Upper Bound
		// Graph and we store in remainingValuesOccurrences the number of
		// occurrences of each remaining values
		boolean[] remainingValues = new boolean[u.length];
		ArrayList<Integer> remainingValuesOccurrences = new ArrayList<Integer>();
		int nbResidualRightNodes = 0;
		for (int i = 0; i < residualVars.size(); i++) {
			IntVar x = residualVars.get(i);
			for (int y = x.getLB(); y <= x.getUB(); y = x.nextValue(y)) {
				if (!remainingValues[y - minValue]) {
					remainingValues[y - minValue] = true;
					nbResidualRightNodes += u[y - minValue] - l[y - minValue];
					remainingValuesOccurrences.add(u[y - minValue] - l[y - minValue]);
				}
			}
		}

		
		// We compute an estimation of the number of perfect matchings in the
		// Residual Upper Bound Graph
		int nbFakeVariables = nbResidualRightNodes - residualVars.size();
		if(nbFakeVariables<0){
			return 0;
		}
		double estim = 1.0;
		for (int deg : residualDegrees) {
			estim *= tools.computeBMFactors(deg);
		}
		// We deal with fake variables symmetry iteratively
		for (int k = 1; k <= nbFakeVariables; k++) {
			estim *= tools.computeBMFactors(nbResidualRightNodes) / k;
		}
		// We deal with valued duplication symmetry depending on the estimator
		switch (estimator) {
		case CountingEstimators.GCC_PQZ:
			for (int j = 0; j < remainingValuesOccurrences.size(); j++) {
				int occ = remainingValuesOccurrences.get(j);
				estim /= tools.computeFactorial(occ);
			}
			break;
		case CountingEstimators.GCC_CORRECTION:
			Collections.sort(remainingValuesOccurrences);
			int remainingSlots = residualVars.size();
			int index = 0;
			while (remainingSlots > 0) {
				if (remainingSlots >= remainingValuesOccurrences.get(index)) {
					estim /= tools.computeBMFactors(remainingValuesOccurrences.get(index));
					remainingSlots -= remainingValuesOccurrences.get(index);
					index++;
				} else {
					estim /= tools.computeArrangement(remainingValuesOccurrences.get(index), remainingSlots);
					remainingSlots = 0;
				}
			}
			break;
		}

		return estim;

	}

}
