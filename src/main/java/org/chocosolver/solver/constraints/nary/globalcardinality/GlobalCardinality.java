/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.nary.globalcardinality;

import gnu.trove.map.hash.TIntIntHashMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ConstraintsName;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Global Cardinality constraint
 *
 * @author Hadrien Cambazard, Charles Prud'homme, Jean-Guillaume Fages
 * @since 16/06/11
 */
public class GlobalCardinality extends Constraint {

	public GlobalCardinality(IntVar[] vars, int[] values, IntVar[] cards) {
		super(ConstraintsName.GCC, createProp(vars, values, cards));
	}

	/**
	 * 
	 * Constructor that add arithmetic constraints into the model. These
	 * deduction rules make the propagation more efficient for
	 * global_cardinality (see Global Constraint Catalog)
	 * 
	 * This must be used only when values is the union of the domain of
	 * variables of vars
	 * 
	 * @param vars
	 * @param values
	 * @param cards
	 * @param deductionRules
	 */
	public GlobalCardinality(IntVar[] vars, int[] values, IntVar[] cards, boolean deductionRules) {
		this(vars, values, cards);
		if (deductionRules) {
			Model m = vars[0].getModel();

			// First deduction rule
			m.sum(cards, "=", vars.length).post();

			// Second deduction rule
			int sumUBVars = 0;
			for (int i = 0; i < vars.length; i++) {
				sumUBVars += vars[i].getUB();
			}
			IntVar scalar = m.intVar(0, sumUBVars);
			m.scalar(cards, values, "=", scalar).post();
			m.sum(vars, "=", scalar);
		}
	}

	private static Propagator createProp(IntVar[] vars, int[] values, IntVar[] cards) {
		assert values.length == cards.length;
		TIntIntHashMap map = new TIntIntHashMap();
		int idx = 0;
		for (int v : values) {
			if (!map.containsKey(v)) {
				map.put(v, idx);
				idx++;
			} else {
				throw new UnsupportedOperationException("ERROR: multiple occurrences of value: " + v);
			}
		}
		return new PropFastGCC(vars, values, map, cards);
	}

	public static Constraint reformulate(IntVar[] vars, IntVar[] card, Model model) {
		List<Constraint> cstrs = new ArrayList<>();
		for (int i = 0; i < card.length; i++) {
			IntVar cste = model.intVar(i);
			BoolVar[] bs = model.boolVarArray("b_" + i, vars.length);
			for (int j = 0; j < vars.length; j++) {
				model.ifThenElse(bs[j], model.arithm(vars[j], "=", cste), model.arithm(vars[j], "!=", cste));
			}
			cstrs.add(model.sum(bs, "=", card[i]));
		}
		return Constraint.merge("reformulatedGCC", cstrs.toArray(new Constraint[cstrs.size()]));
	}
}
