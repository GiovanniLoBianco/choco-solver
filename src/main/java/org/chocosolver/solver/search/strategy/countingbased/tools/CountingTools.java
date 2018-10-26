/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2018, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.strategy.countingbased.tools;


/**
 * 
 * Tool class containing several methods used into counting algorithms
 * 
 * @author giovannilobianco
 * @since octobre 2018
 */
public class CountingTools {

	public static final int SIZE = 10000;
	public static final int SIZE_FACT = 171;

	private double[] factorsBM = new double[SIZE];
	private double[][] factorsLB = new double[SIZE][SIZE];
	private double[] fact = new double[SIZE_FACT];
	private double[][] arrangements = new double[SIZE_FACT][SIZE_FACT];

	/**
	 * Pre-compute a high number of Bregman-Minc facors, Liang-Bai factors,
	 * factorials and arrangements values
	 */
	public CountingTools() {
		factorsBM[0] = 1;
		for (int i = 1; i < SIZE; i++) {
			factorsBM[i] = Math.pow(i / factorsBM[i - 1], 1 / (i * 1.0)) * factorsBM[i - 1];
		}

		for (int i = 0; i < SIZE; i++) {
			for (int r = 0; r < SIZE; r++) {
				int q = (int) Math.min(Math.ceil((r + 1) / 2.0), Math.ceil((i + 1) / 2.0));
				factorsLB[i][r] = Math.sqrt(q * (r - q + 1));
			}
		}

		this.fact[0] = 1;
		for (int k = 1; k < SIZE_FACT; k++) {
			this.fact[k] = k * this.fact[k - 1];
		}

		for (int i = 0; i < SIZE_FACT; i++) {
			for (int j = 0; j <= i; j++) {
				this.arrangements[i][j] = this.fact[i] / this.fact[i - j];
			}
		}
	}

	/**
	 * 
	 * @param n
	 * @return the nth factor of Bregman-Minc
	 */
	public double computeBMFactors(int n) {
		// TODO
		return factorsBM[n];
	}

	/**
	 * 
	 * @param i
	 * @param r
	 * @return the Liang-Bai factor (i,r)
	 */
	public double computeLBFactors(int i, int r) {
		// TODO
		return factorsLB[i][r];
	}

	/**
	 * 
	 * @param k
	 * @return k!
	 */
	public double computeFactorial(int k) {
		return fact[k];
	}

	/**
	 * 
	 * @param n
	 * @param k
	 * @return the number of k arrangements among n
	 */
	public double computeArrangement(int n, int k) {
		return arrangements[n][k];
	}
	
	public static void main(String[] args) {
		CountingTools tools = new CountingTools();
	}

}
