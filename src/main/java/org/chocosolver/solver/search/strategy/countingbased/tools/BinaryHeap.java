package org.chocosolver.solver.search.strategy.countingbased.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a Binary Search Tree, which aims at sorting E elments
 * 
 * @author giovannilobianco
 *
 * @param <E>
 */
public class BinaryHeap<E extends Comparable<E>> {

	private E root;
	private BinaryHeap<E> h1;
	private BinaryHeap<E> h2;

	// ***********************************************************************************
	// CONSTRUCTORS
	// ***********************************************************************************

	public BinaryHeap(E e) {
		this.root = e;
		this.h1 = null;
		this.h2 = null;
	}

	public BinaryHeap() {
		this.root = null;
		this.h1 = null;
		this.h2 = null;
	}

	public BinaryHeap(List<E> l) {
		this.root = l.get(0);
		this.h1 = null;
		this.h2 = null;
		for (int i = 1; i < l.size(); i++) {
			this.add(l.get(i));
		}

	}

	// ***********************************************************************************
	// METHODS
	// ***********************************************************************************

	public void add(E e) {
		if (root == null) {
			root = e;
		} else if (e.compareTo(root) > 0) {
			if (this.h2 == null) {
				this.h2 = new BinaryHeap<E>(e);
			} else {
				this.h2.add(e);
			}
		} else {
			if (this.h1 == null) {
				this.h1 = new BinaryHeap<E>(e);
			} else {
				this.h1.add(e);
			}
		}
	}
	
	/**
	 * 
	 * @return the sorted list of elements in ascending order
	 */
	public List<E> read() {
		List<E> l = new ArrayList<E>();
		this.inOrder(l);
		System.out.println(l.size());
		return l;
	}

	public void inOrder(List<E> l) {
		if (root != null) {
			if (this.h1 == null && this.h2 == null) {
				l.add(root);
			} else if (this.h1 == null) {
				l.add(root);
				this.h2.inOrder(l);
			} else if (this.h2 == null) {
				this.h1.inOrder(l);
				l.add(root);
			} else {
				this.h1.inOrder(l);
				l.add(root);
				this.h2.inOrder(l);
			}
		}
	}

}
