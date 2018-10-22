package org.chocosolver.solver.search.strategy.countingbased.tools;

import org.chocosolver.solver.variables.IntVar;

/**
 * 
 * Represents an assignment of a integer variable to a value.
 * 
 * @author giovannilobianco
 *
 */
public class IntVarAssignment {

	private IntVar var;
	private int val;

	public IntVarAssignment(IntVar var, int val) {
		super();
		this.var = var;
		this.val = val;
	}

	public IntVar getVar() {
		return var;
	}

	public int getVal() {
		return val;
	}

	
	public boolean equals(Object o) {
		if (o instanceof IntVarAssignment) {
			IntVarAssignment a = (IntVarAssignment)o;
			return a.getVar().getId()==var.getId() && a.getVal() == val;
		} else {
			return false;
		}
	}

	public String toString() {
		return var.getName() + "<-" + val;
	}


}
