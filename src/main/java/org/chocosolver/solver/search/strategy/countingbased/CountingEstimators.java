/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
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
	public static final String ALLDIFFERENT_EXACT = "alldifferent_Exact";

	// --------- GLOBAL_CARDINALITY --------------
	public static final String GCC_PQZ = "gcc_PQZ";
	public static final String GCC_CORRECTION = "gcc_Correction";
	public static final String GCC_EXACT = "gcc_Exact";

}
