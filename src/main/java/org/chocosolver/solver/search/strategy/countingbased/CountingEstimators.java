package org.chocosolver.solver.search.strategy.countingbased;

/**
 * 
 * Class containing several String constants describing a way to estimate the
 * number of solutions for different constraint
 * 
 * @author giovannilobianco
 * @since octobre 2018
 */
public class CountingEstimators {

	// --------- ALLDIFFERENT --------------
	public static final String ALLDIFFERENT_PQZ = "alldifferent_PQZ";
	public static final String ALLDIFFERENT_ER = "alldifferent_ER";
	public static final String ALLDIFFERENT_FDS = "alldifferent_FDS";

	// --------- GLOBAL_CARDINALITY --------------
	public static final String GCC_PQZ = "gcc_PQZ";
	public static final String GCC_CORRECTION = "gcc_Correction";

}
