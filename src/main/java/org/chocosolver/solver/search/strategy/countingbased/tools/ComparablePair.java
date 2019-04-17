/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.countingbased.tools;

public class ComparablePair<T1, T2 extends Comparable<T2>> implements Comparable<ComparablePair<T1,T2>> {

	private T1 item;
	private T2 comparable;
	
	public ComparablePair(T1 item, T2 comparable){
		this.item=item;
		this.comparable=comparable;
	}

	public T1 getItem(){
		return item;
	}
	
	public T2 getComparable(){
		return comparable;
	}

	@Override
	public int compareTo(ComparablePair<T1,T2> o) {
		// TODO Auto-generated method stub
		return comparable.compareTo(o.comparable);
	}
	
	
}
