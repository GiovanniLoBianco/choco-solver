/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.nvalue;

import gnu.trove.map.hash.TIntIntHashMap;

import org.chocosolver.solver.Cause;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.Countable;
import org.chocosolver.solver.search.strategy.countingbased.CountingEstimators;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.graphOperations.connectivity.StrongConnectivityFinder;
import org.chocosolver.util.objects.graphs.DirectedGraph;
import org.chocosolver.util.objects.setDataStructures.ISetIterator;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.procedure.UnaryIntProcedure;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.chocosolver.solver.constraints.PropagatorPriority.QUADRATIC;
import static org.chocosolver.util.tools.ArrayUtils.concat;

/**
 * AtLeastNValues Propagator (similar to SoftAllDiff) The number of distinct
 * values in vars is at least nValues Performs Generalized Arc Consistency based
 * on Maximum Bipartite Matching The worst case time complexity is O(nm) but
 * this is very pessimistic In practice it is more like O(m) where m is the
 * number of variable-value pairs
 * <p/>
 * BEWARE UNSAFE : BUG DETECTED THROUGH DOBBLE(3,4,6)
 * <p/>
 * !redundant propagator!
 *
 * @author Jean-Guillaume Fages
 */
public class PropAtLeastNValues_AC extends Propagator<IntVar> implements Countable{

	// ***********************************************************************************
	// VARIABLES
	// ***********************************************************************************

	private int n, n2;
	private DirectedGraph digraph;
	private int[] nodeSCC;
	private BitSet free;
	private UnaryIntProcedure<Integer> remProc;
	private final IIntDeltaMonitor[] idms;
	private StrongConnectivityFinder SCCfinder;
	// for augmenting matching (BFS)
	private int[] father;
	private BitSet in;
	private TIntIntHashMap map;
	private int[] fifo;

	// ***********************************************************************************
	// CONSTRUCTORS
	// ***********************************************************************************

	/**
	 * AtLeastNValues Propagator (similar to SoftAllDiff) The number of distinct
	 * values in vars is at least nValues Performs Generalized Arc Consistency
	 * based on Maximum Bipartite Matching The worst case time complexity is
	 * O(nm) but this is very pessimistic In practice it is more like O(m) where
	 * m is the number of variable-value pairs
	 *
	 * @param variables
	 *            array of integer variables
	 * @param nValues
	 *            integer variable
	 */
	public PropAtLeastNValues_AC(IntVar[] variables, int[] vals, IntVar nValues) {
		super(concat(variables, nValues), QUADRATIC, true);
		this.idms = new IIntDeltaMonitor[this.vars.length];
		for (int i = 0; i < this.vars.length; i++) {
			idms[i] = this.vars[i].monitorDelta(this);
		}
		n = variables.length;
		map = new TIntIntHashMap(vals.length);
		IntVar v;
		int ub;
		int idx = n;
		for (int i = 0; i < n; i++) {
			v = vars[i];
			ub = v.getUB();
			for (int j = v.getLB(); j <= ub; j = v.nextValue(j)) {
				if (!map.containsKey(j)) {
					map.put(j, idx);
					idx++;
				}
			}
		}
		n2 = idx;
		fifo = new int[n2];
		digraph = new DirectedGraph(model, n2 + 2, SetType.LINKED_LIST, false);
		free = new BitSet(n2);
		remProc = new DirectedRemProc();
		father = new int[n2];
		in = new BitSet(n2);
		SCCfinder = new StrongConnectivityFinder(digraph);
	}

	// ***********************************************************************************
	// Initialization
	// ***********************************************************************************

	private void buildDigraph() {
		for (int i = 0; i < n2; i++) {
			digraph.getSuccOf(i).clear();
			digraph.getPredOf(i).clear();
		}
		free.set(0, n2);
		int j, k, ub;
		IntVar v;
		for (int i = 0; i < n2 + 2; i++) {
			digraph.removeNode(i);
		}
		for (int i = 0; i < n; i++) {
			v = vars[i];
			ub = v.getUB();
			for (k = v.getLB(); k <= ub; k = v.nextValue(k)) {
				j = map.get(k);
				digraph.addArc(i, j);
			}
		}
	}

	// ***********************************************************************************
	// MATCHING
	// ***********************************************************************************

	private int repairMatching() throws ContradictionException {
		for (int i = free.nextSetBit(0); i >= 0 && i < n; i = free.nextSetBit(i + 1)) {
			tryToMatch(i);
		}
		int card = 0;
		for (int i = 0; i < n; i++) {
			if (digraph.getPredOf(i).size() > 0) {
				card++;
			}
		}
		return card;
	}

	private void tryToMatch(int i) throws ContradictionException {
		int mate = augmentPath_BFS(i);
		if (mate != -1) {
			free.clear(mate);
			free.clear(i);
			int tmp = mate;
			while (tmp != i) {
				digraph.removeArc(father[tmp], tmp);
				digraph.addArc(tmp, father[tmp]);
				tmp = father[tmp];
			}
		}
	}

	private int augmentPath_BFS(int root) {
		in.clear();
		int indexFirst = 0, indexLast = 0;
		fifo[indexLast++] = root;
		int x;
		ISetIterator succs;
		while (indexFirst != indexLast) {
			x = fifo[indexFirst++];
			succs = digraph.getSuccOf(x).iterator();
			while (succs.hasNext()) {
				int y = succs.nextInt();
				if (!in.get(y)) {
					father[y] = x;
					fifo[indexLast++] = y;
					in.set(y);
					if (free.get(y)) {
						return y;
					}
				}
			}
		}
		return -1;
	}

	// ***********************************************************************************
	// PRUNING
	// ***********************************************************************************

	private void buildSCC() {
		digraph.removeNode(n2);
		digraph.removeNode(n2 + 1);
		digraph.addNode(n2);
		digraph.addNode(n2 + 1);
		// TODO CHECK THIS PART
		for (int i = 0; i < n; i++) {
			if (free.get(i)) {
				digraph.addArc(n2, i);
			} else {
				digraph.addArc(i, n2);
			}
		}
		for (int i = n; i < n2; i++) {
			if (free.get(i)) {
				digraph.addArc(i, n2 + 1);
			} else {
				digraph.addArc(n2 + 1, i);
			}
		}
		SCCfinder.findAllSCC();
		nodeSCC = SCCfinder.getNodesSCC();
		digraph.removeNode(n2);
		digraph.removeNode(n2 + 1);
	}

	private void filter() throws ContradictionException {
		buildSCC();
		int j, ub;
		IntVar v;
		for (int i = 0; i < n; i++) {
			v = vars[i];
			ub = v.getUB();
			for (int k = v.getLB(); k <= ub; k = v.nextValue(k)) {
				j = map.get(k);
				if (nodeSCC[i] != nodeSCC[j]) {
					if (digraph.getPredOf(i).contains(j)) {
						v.instantiateTo(k, this);
					} else {
						v.removeValue(k, this);
						digraph.removeArc(i, j);
					}
				}
			}
			if (!v.hasEnumeratedDomain()) {
				ub = v.getUB();
				for (int k = v.getLB(); k <= ub; k = v.nextValue(k)) {
					j = map.get(k);
					if (digraph.arcExists(i, j) || digraph.arcExists(j, i)) {
						break;
					} else {
						v.removeValue(k, this);
					}
				}
				int lb = v.getLB();
				for (int k = ub; k >= lb; k = v.previousValue(k)) {
					j = map.get(k);
					if (digraph.arcExists(i, j) || digraph.arcExists(j, i)) {
						break;
					} else {
						v.removeValue(k, this);
					}
				}
			}
		}
	}

	// ***********************************************************************************
	// PROPAGATION
	// ***********************************************************************************

	@Override
	public void propagate(int evtmask) throws ContradictionException {
		if (PropagatorEventType.isFullPropagation(evtmask)) {
			if (n2 < n + vars[n].getLB()) {
				fails(); // TODO: could be more precise, for explanation purpose
			}
			buildDigraph();
		}
		digraph.removeNode(n2);
		digraph.removeNode(n2 + 1);
		free.clear();
		for (int i = 0; i < n; i++) {
			if (digraph.getPredOf(i).size() == 0) {
				free.set(i);
			}
		}
		for (int i = n; i < n2; i++) {
			if (digraph.getSuccOf(i).size() == 0) {
				free.set(i);
			}
		}
		int card = repairMatching();
		vars[n].updateUpperBound(card, this);
		if (vars[n].getLB() == card) {
			filter();
		}
		for (int i = 0; i < idms.length; i++) {
			idms[i].unfreeze();
		}
	}

	@Override
	public void propagate(int varIdx, int mask) throws ContradictionException {
		if (varIdx < n) {
			idms[varIdx].freeze();
			idms[varIdx].forEachRemVal(remProc.set(varIdx));
			idms[varIdx].unfreeze();
		}
		forcePropagate(PropagatorEventType.CUSTOM_PROPAGATION);
	}

	// ***********************************************************************************
	// INFO
	// ***********************************************************************************

	@Override
	public ESat isEntailed() {
		BitSet values = new BitSet(n2);
		BitSet mandatoryValues = new BitSet(n2);
		IntVar v;
		int ub;
		for (int i = 0; i < n; i++) {
			v = vars[i];
			ub = v.getUB();
			if (v.isInstantiated()) {
				mandatoryValues.set(map.get(ub));
			}
			for (int j = v.getLB(); j <= ub; j++) {
				values.set(map.get(j));
			}
		}
		if (mandatoryValues.cardinality() >= vars[n].getUB()) {
			return ESat.TRUE;
		}
		if (values.cardinality() < vars[n].getLB()) {
			return ESat.FALSE;
		}
		return ESat.UNDEFINED;
	}

	private class DirectedRemProc implements UnaryIntProcedure<Integer> {

		private int idx;

		public void execute(int i) throws ContradictionException {
			digraph.removeArc(idx, map.get(i));
			digraph.removeArc(map.get(i), idx);
		}

		@Override
		public UnaryIntProcedure set(Integer integer) {
			this.idx = integer;
			return this;
		}
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
			int sumDomain = 0;
			for (int i = 0; i < n; i++) {
				IntVar x = this.getVar(i);
				sumDomain += x.getDomainSize();
			}
			double p = sumDomain / (1.0 * n * m);

			// Cardinality variable N
			IntVar N = this.getVar(n);
			int sum = 0;
			for (int card = N.getLB(); card <= n; card++) {
				sum += tools.computebinomCoeff(m, card) * tools.computetriangleCoef(n, card);
			}

			double estim = sum * Math.pow(p, n);

			return estim;
		}

	}
}
