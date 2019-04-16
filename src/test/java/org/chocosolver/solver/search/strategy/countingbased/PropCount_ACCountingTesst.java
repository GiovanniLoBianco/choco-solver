package org.chocosolver.solver.search.strategy.countingbased;

import java.io.IOException;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.nary.count.PropCount_AC;
import org.chocosolver.solver.constraints.nary.nvalue.PropAMNV;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.variables.IntVar;
import org.testng.annotations.Test;
import org.testng.reporters.jq.Main;

public class PropCount_ACCountingTesst {

	public static CountingTools tools = new CountingTools();

	public static PropCount_AC createInstance1() {
		Model model = new Model();
		IntVar[] vars = new IntVar[7];
		vars[0] = model.intVar(new int[] { 2, 4, 6 });
		vars[1] = model.intVar(new int[] { 3, 4, 9 });
		vars[2] = model.intVar(new int[] { 5, 7, 8 });
		vars[3] = model.intVar(new int[] { 3, 4, 5, 6, 8 });
		vars[4] = model.intVar(new int[] { 1, 4, 6 });
		vars[5] = model.intVar(new int[] { 3, 6, 9 });
		vars[6] = model.intVar(new int[] { 2, 4, 6, 7 });

		IntVar N = model.intVar(2);

		Constraint c = model.count(3, vars, N);
		c.post();
		return (PropCount_AC) c.getPropagator(0);
	}

	@Test
	public void f() {
	}

	public static void main(String[] args) {
		PropCount_AC prop = createInstance1();

		try {
			double estim = prop.estimateNbSolutions("", tools);
			System.out.println(estim);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
