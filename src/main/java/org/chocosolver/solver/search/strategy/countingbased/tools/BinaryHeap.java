package org.chocosolver.solver.search.strategy.countingbased.tools;

import java.util.ArrayList;
import java.util.List;

public class BinaryHeap<E extends Comparable<E>> {

	private E racine;
	private BinaryHeap<E> tas1;
	private BinaryHeap<E> tas2;

	public BinaryHeap(E c) {
		this.racine = c;
		this.tas1 = null;
		this.tas2 = null;
	}

	public BinaryHeap() {
		this.racine = null;
		this.tas1 = null;
		this.tas2 = null;
	}

	public BinaryHeap(List<E> l) {
		this.racine = l.get(0);
		this.tas1 = null;
		this.tas2 = null;
		for (int i = 1; i < l.size(); i++) {
			this.ajouter(l.get(i));
		}

	}

	public void ajouter(E c) {

		if (racine == null) {
			racine = c;
		} else if (c.compareTo(racine) > 0) {
			if (this.tas2 == null) {
				this.tas2 = new BinaryHeap<E>(c);
			} else {
				this.tas2.ajouter(c);
			}
		} else {
			if (this.tas1 == null) {
				this.tas1 = new BinaryHeap<E>(c);
			} else {
				this.tas1.ajouter(c);
			}
		}
	}

	public List<E> lire() {
		List<E> l = new ArrayList<E>();
		this.parcoursInfixe(l);
		return l;
	}

	public void parcoursInfixe(List<E> l) {
		if (racine != null) {
			if (this.tas1 == null && this.tas2 == null) {
				l.add(racine);
			} else if (this.tas1 == null) {
				l.add(racine);
				this.tas2.parcoursInfixe(l);
			} else if (this.tas2 == null) {
				this.tas1.parcoursInfixe(l);
				l.add(racine);
			} else {
				this.tas1.parcoursInfixe(l);
				l.add(racine);
				this.tas2.parcoursInfixe(l);
			}
		}
	}

}
