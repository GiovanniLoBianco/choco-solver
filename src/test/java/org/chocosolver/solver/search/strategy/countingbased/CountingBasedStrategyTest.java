package org.chocosolver.solver.search.strategy.countingbased;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.nary.alldifferent.AllDifferent;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffAC;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.variables.IntVar;
import org.testng.annotations.Test;

public class CountingBasedStrategyTest {

	static CountingTools tools = new CountingTools();

	public static Model buildModel() {
		Model model = new Model();

		IntVar var0 = model.intVar(new int[] { 2, 4, 6 });
		IntVar var1 = model.intVar(new int[] { 3, 4 });
		IntVar var2 = model.intVar(new int[] { 5, 7 });
		IntVar var3 = model.intVar(new int[] { 3, 4, 5, 6 });
		IntVar var4 = model.intVar(new int[] { 1, 4, 6 });
		IntVar var5 = model.intVar(new int[] { 3, 4, 5 });
		IntVar var6 = model.intVar(new int[] { 1, 2, 4, 5 });
		IntVar var7 = model.intVar(new int[] { 1, 6, 7 });

		IntVar[] alldiff1 = new IntVar[] { var0, var1, var2, var3, var4 };
		IntVar[] alldiff2 = new IntVar[] { var3, var4, var5, var6, var7 };

		AllDifferent c1 = (AllDifferent) model.allDifferent(alldiff1, "AC");
		c1.post();
		AllDifferent c2 = (AllDifferent) model.allDifferent(alldiff2, "AC");
		c2.post();

		PropAllDiffAC prop1 = (PropAllDiffAC) c1.getPropagator(1);
		PropAllDiffAC prop2 = (PropAllDiffAC) c2.getPropagator(1);

		Map<IntVarAssignment, Double> map1 = prop1.computeDensities(CountingEstimators.ALLDIFFERENT_PQZ, tools);
		Map<IntVarAssignment, Double> map2 = prop2.computeDensities(CountingEstimators.ALLDIFFERENT_PQZ, tools);

		return model;
	}

	@Test
	public void maxSDTest() {
		Model model = buildModel();
		IntVar[] vars = model.retrieveIntVars(true);
		MaxSD maxSD = new MaxSD(model);
		maxSD.computeOrder();
		List<IntVarAssignment> listInst = new ArrayList<IntVarAssignment>();

		listInst.add(new IntVarAssignment(vars[2], 7));
		listInst.add(new IntVarAssignment(vars[4], 1));
		listInst.add(new IntVarAssignment(vars[0], 2));

		listInst.add(new IntVarAssignment(vars[1], 3));
		listInst.add(new IntVarAssignment(vars[7], 7));
		listInst.add(new IntVarAssignment(vars[6], 2));

		listInst.add(new IntVarAssignment(vars[5], 3));
		listInst.add(new IntVarAssignment(vars[1], 4));
		listInst.add(new IntVarAssignment(vars[4], 6));

		listInst.add(new IntVarAssignment(vars[3], 6));
		listInst.add(new IntVarAssignment(vars[5], 5));
		listInst.add(new IntVarAssignment(vars[2], 5));

		listInst.add(new IntVarAssignment(vars[3], 3));
		listInst.add(new IntVarAssignment(vars[3], 5));
		listInst.add(new IntVarAssignment(vars[4], 4));

		listInst.add(new IntVarAssignment(vars[5], 4));
		listInst.add(new IntVarAssignment(vars[0], 6));
		listInst.add(new IntVarAssignment(vars[7], 1));

		listInst.add(new IntVarAssignment(vars[7], 6));
		listInst.add(new IntVarAssignment(vars[3], 4));
		listInst.add(new IntVarAssignment(vars[6], 5));

		listInst.add(new IntVarAssignment(vars[6], 1));
		listInst.add(new IntVarAssignment(vars[6], 4));
		listInst.add(new IntVarAssignment(vars[0], 4));

		for (int k = 0; k < listInst.size(); k++) {
			assertEquals(maxSD.order[k], listInst.get(k));
		}

	}

}
