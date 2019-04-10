package org.chocosolver.solver.search.strategy.countingbased;

import java.io.IOException;
import java.util.Map;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.nary.among.PropAmongGAC;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.testng.annotations.Test;

public class PropAmongGACCountingTest {

	public static CountingTools tools = new CountingTools();

	public static PropAmongGAC createInstance1() {
		Model model = new Model();
		IntVar[] vars = new IntVar[7];
		vars[0] = model.intVar(new int[] { 2, 4, 6 });
		vars[1] = model.intVar(new int[] { 3, 4, 9 });
		vars[2] = model.intVar(new int[] { 5, 7, 8 });
		vars[3] = model.intVar(new int[] { 3, 4, 5, 6, 8 });
		vars[4] = model.intVar(new int[] { 1, 4, 6 });
		vars[5] = model.intVar(new int[] { 3, 6, 9 });
		vars[6] = model.intVar(new int[] { 2, 4, 6, 7 });
		
		IntVar N = model.intVar(1,3);

		int[] values = new int[]{2,3,5};
		
		Constraint c =  model.among(N, vars, values);
		c.post();
		return (PropAmongGAC) c.getPropagator(0);
	}

	@Test
	public void testEstimateSolutionsAmong() {
		PropAmongGAC prop = createInstance1();
		try {
			prop.propagate(PropagatorEventType.FULL_PROPAGATION.getMask());
			Model m = prop.getModel();
			System.out.println(m);
			double estim= prop.estimateNbSolutions("", tools);
			System.out.println(estim);
		} catch (ContradictionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testComputeDensitiesAmong() {
		PropAmongGAC prop = createInstance1();
		try {
			prop.propagate(PropagatorEventType.FULL_PROPAGATION.getMask());
			Model m = prop.getModel();
			System.out.println(m);
			Map<IntVarAssignment, Double> map = prop.computeDensities("", tools);
		} catch (ContradictionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
		
}
