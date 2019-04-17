/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.countingbased;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.nary.alldifferent.AllDifferent;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffAC;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.testng.annotations.Test;
import org.testng.reporters.jq.Main;

public class PropAllDiffACCountingTest {

	private static CountingTools tools = new CountingTools();

	public static PropAllDiffAC createInstance1() {
		Model model = new Model();
		IntVar[] vars = new IntVar[5];
		vars[0] = model.intVar(new int[] { 2, 4, 6 });
		vars[1] = model.intVar(new int[] { 3, 4 });
		vars[2] = model.intVar(new int[] { 5, 7 });
		vars[3] = model.intVar(new int[] { 3, 4, 5, 6 });
		vars[4] = model.intVar(new int[] { 1, 4, 6 });

		AllDifferent alldiff = (AllDifferent) model.allDifferent(vars, "AC");
		return (PropAllDiffAC) alldiff.getPropagator(1);
	}

	public static PropAllDiffAC createInstance2() {
		Model model = new Model();
		IntVar[] vars = new IntVar[5];
		vars[0] = model.intVar(new int[] { 2, 4, 6 });
		vars[1] = model.intVar(new int[] { 3 });
		vars[2] = model.intVar(new int[] { 5, 7 });
		vars[3] = model.intVar(new int[] { 4, 5, 6 });
		vars[4] = model.intVar(new int[] { 1, 4, 6 });

		AllDifferent alldiff = (AllDifferent) model.allDifferent(vars, "AC");
		return (PropAllDiffAC) alldiff.getPropagator(1);
	}

	public static PropAllDiffAC createInstance3() {
		Model model = new Model();
		IntVar[] vars = new IntVar[7];
		vars[0] = model.intVar(new int[] { 2 });
		vars[1] = model.intVar(new int[] { 3, 4 });
		vars[2] = model.intVar(new int[] { 5, 7 });
		vars[3] = model.intVar(new int[] { 3, 4, 5, 6 });
		vars[4] = model.intVar(new int[] { 1, 4, 6 });
		vars[5] = model.intVar(new int[] { 1, 3, 5, 7 });
		vars[6] = model.intVar(new int[] { 1, 4 });

		AllDifferent alldiff = (AllDifferent) model.allDifferent(vars, "AC");

		return (PropAllDiffAC) alldiff.getPropagator(1);
	}

	@Test
	public void testEstimateNbSolutionsPQZ() {
		PropAllDiffAC c1 = createInstance1();
		double estim1 = 1.0;
		estim1 *= tools.computeBMFactors(3);
		estim1 *= tools.computeBMFactors(2);
		estim1 *= tools.computeBMFactors(2);
		estim1 *= tools.computeBMFactors(4);
		estim1 *= tools.computeBMFactors(3);
		estim1 *= tools.computeBMFactors(7) / 1;
		estim1 *= tools.computeBMFactors(7) / 2;
		try {
			assertEquals(c1.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_PQZ, tools), estim1,
					"PQZ estimator correct");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PropAllDiffAC c2 = createInstance2();
		double estim2 = 1.0;
		estim2 *= tools.computeBMFactors(3);
		estim2 *= tools.computeBMFactors(2);
		estim2 *= tools.computeBMFactors(3);
		estim2 *= tools.computeBMFactors(3);
		estim2 *= tools.computeBMFactors(6) / 1;
		estim2 *= tools.computeBMFactors(6) / 2;
		try {
			assertEquals(c2.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_PQZ, tools), estim2,
					"PQZ estimator correct with fixed variables");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PropAllDiffAC c3 = createInstance3();
		double estim3 = 1.0;
		estim3 *= tools.computeBMFactors(2);
		estim3 *= tools.computeBMFactors(2);
		estim3 *= tools.computeBMFactors(4);
		estim3 *= tools.computeBMFactors(3);
		estim3 *= tools.computeBMFactors(4);
		estim3 *= tools.computeBMFactors(2);
		try {
			assertEquals(c3.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_PQZ, tools), estim3,
					"PQZ estimator correct with any fake variables");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testEstimateNbSolutionsER() {
		PropAllDiffAC c1 = createInstance1();
		double p1 = 1.0 * 14 / 35;
		double estim1 = 1.0;
		for (int k = 2 + 1; k <= 7; k++) {
			estim1 *= p1 * k;
		}

		try {
			assertEquals(c1.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_ER, tools), estim1,
					"ER estimator correct");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PropAllDiffAC c2 = createInstance2();
		double p2 = (1.0 * 11) / 24;
		double estim2 = 1.0;
		for (int k = 2 + 1; k <= 6; k++) {
			estim2 *= p2 * k;
		}

		try {
			assertEquals(c2.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_ER, tools), estim2,
					"ER estimator correct with fixed variables");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PropAllDiffAC c3 = createInstance3();
		double p3 = (1.0 * 17) / 36;
		double estim3 = 1.0;
		for (int k = 0 + 1; k <= 6; k++) {
			estim3 *= p3 * k;
		}

		try {
			assertEquals(c3.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_ER, tools), estim3,
					"ER estimator correct with any fake variables");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testEstimateNbSolutionsFDS() {
		PropAllDiffAC c1 = createInstance1();
		double estim1 = 1.0;
		estim1 *= 3 * 3 * 1.0 / 7;
		estim1 *= 2 * 4 * 1.0 / 7;
		estim1 *= 2 * 5 * 1.0 / 7;
		estim1 *= 4 * 6 * 1.0 / 7;
		estim1 *= 3 * 7 * 1.0 / 7;

		try {
			assertEquals(c1.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_FDS, tools), estim1,
					"FDS estimator correct");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PropAllDiffAC c2 = createInstance2();
		double estim2 = 1.0;
		estim2 *= 3 * 3 * 1.0 / 6;
		estim2 *= 2 * 4 * 1.0 / 6;
		estim2 *= 3 * 5 * 1.0 / 6;
		estim2 *= 3 * 6 * 1.0 / 6;


		try {
			assertEquals(c2.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_FDS, tools), estim2,
					"FDS estimator correct with fixed variables");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PropAllDiffAC c3 = createInstance3();
		double estim3 = 1.0;
		estim3 *= 2 * 1 * 1.0 / 6;
		estim3 *= 2 * 2 * 1.0 / 6;
		estim3 *= 4 * 3 * 1.0 / 6;
		estim3 *= 3 * 4 * 1.0 / 6;
		estim3 *= 4 * 5 * 1.0 / 6;
		estim3 *= 2 * 6 * 1.0 / 6;

		try {
			assertEquals(c3.estimateNbSolutions(CountingEstimators.ALLDIFFERENT_FDS, tools), estim3,
					"FDS estimator correct with any fake variables");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
