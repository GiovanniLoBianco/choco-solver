package org.chocosolver.solver.search.strategy.countingbased;

import java.util.Map;

import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;

/**
 * 
 * This interface describes propagators on which we are able to count solutions.
 * It describes propagators and not constraints, because depending on the
 * constraint structure, different propagators can be used and the counting
 * algorithm must be adapted.
 * 
 * @author giovannilobianco
 * @since octobre 2018
 *
 */
public interface Countable {

	/**
	 * For each possible assignment variable/value, compute the number of
	 * remaining solutions and, then, normalize it to obtain solution densities.
	 * We only consider the variables from the propagator, not the whole model.
	 * 
	 * @param estimator
	 *            the estimator to count solutions
	 * 
	 * @param tools
	 *            Counting tools that the countable propagator may use.
	 * 
	 */
	public Map<IntVarAssignment, Double> computeDensities(String estimator, CountingTools tools);

}
