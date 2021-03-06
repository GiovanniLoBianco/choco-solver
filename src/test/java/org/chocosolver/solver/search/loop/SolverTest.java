/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.search.loop;

import org.chocosolver.cutoffseq.LubyCutoffStrategy;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.search.limits.NodeCounter;
import org.chocosolver.solver.search.loop.lns.neighbors.RandomNeighborhood;
import org.chocosolver.solver.search.loop.move.MoveBinaryDFS;
import org.chocosolver.solver.search.loop.move.MoveBinaryLDS;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.testng.annotations.Test;

import static org.chocosolver.solver.search.strategy.Search.*;
import static org.chocosolver.util.ProblemMaker.makeGolombRuler;
import static org.chocosolver.util.ProblemMaker.makeNQueenWithOneAlldifferent;
import static org.testng.Assert.assertEquals;

/**
 * Created by cprudhom on 02/09/15.
 * Project: choco.
 */
public class SolverTest {

    @Test(groups="1s", timeOut=60000)
   	public void testReset() {
   		Model m = new Model();
   		IntVar x =m.intVar("X", 0, 9);
   		IntVar y= m.intVar("Y", 0, 9);

        Constraint x_lesser_y = m.arithm(y, ">", x);
        x_lesser_y.post();
   		m.member(x, new int[]{1,2}).post();

        // computeOptimum
        m.setObjective(Model.MAXIMIZE,x);
        while (m.getSolver().solve());
        assertEquals(m.getSolver().getSolutionCount(),1);

        // enumerate optima does not work because of previous cut
        m.getSolver().reset();
        m.getSolver().getObjectiveManager().setCutComputer((Number number) -> number);
        while (m.getSolver().solve());
   		assertEquals(m.getSolver().getSolutionCount(),7);

        // reset, remove constraint and enumerate solutions
        m.getSolver().reset();
        m.clearObjective();
   		m.unpost(x_lesser_y);
        m.getSolver().setSearch(Search.defaultSearch(m));
        while (m.getSolver().solve());
        assertEquals(m.getSolver().getSolutionCount(),20);
   	}

    @Test(groups="1s", timeOut=60000)
    public void test1DFS() {
        Model model = makeNQueenWithOneAlldifferent(8);
        Solver r = model.getSolver();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(true)));
        r.setDFS();
        model.getSolver().showSolutions();
        model.getSolver().solve();
        assertEquals(r.getMeasures().getSolutionCount(), 1);
        assertEquals(r.getMeasures().getNodeCount(), 24);
    }

    @Test(groups="1s", timeOut=60000)
    public void test1LDS() {
        Model model = makeNQueenWithOneAlldifferent(8);
        Solver r = model.getSolver();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(false)));
        r.setLDS(4);
        model.getSolver().solve();
        assertEquals(r.getMeasures().getSolutionCount(), 1);
        assertEquals(r.getMeasures().getNodeCount(), 144);
    }

    @Test(groups="1s", timeOut=60000)
    public void test1DDS() {
        Model model = makeNQueenWithOneAlldifferent(8);
        Solver r = model.getSolver();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(true)));
        r.setDDS(4);
        model.getSolver().solve();
        assertEquals(r.getMeasures().getSolutionCount(), 1);
        assertEquals(r.getMeasures().getNodeCount(), 70);
    }

    @Test(groups="10s", timeOut=60000)
    public void test1HBFS() {
        Model model = makeGolombRuler(8);
        Solver r = model.getSolver();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(false)));
        r.setHBFS(.05, .1, 32);
        while(model.getSolver().solve());
        assertEquals(model.getSolver().getSolutionCount(), 7);
        assertEquals(model.getSolver().getNodeCount(), 5881);
    }

    @Test(groups="1s", timeOut=60000)
    public void test2DFS() {
        Model model = makeNQueenWithOneAlldifferent(8);
        Solver r = model.getSolver();
        r.setDFS();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(true)));
        while (model.getSolver().solve()) ;
        assertEquals(model.getSolver().getSolutionCount(), 92);
        assertEquals(model.getSolver().getNodeCount(), 480);
    }

    @Test(groups="1s", timeOut=60000)
    public void test2LDS() {
        Model model = makeNQueenWithOneAlldifferent(8);
        Solver r = model.getSolver();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(false)));
        r.setLDS(4);
        while (model.getSolver().solve()) ;
        assertEquals(model.getSolver().getSolutionCount(), 7);
        assertEquals(model.getSolver().getNodeCount(), 205);
    }

    @Test(groups="1s", timeOut=60000)
    public void test2DDS() {
        Model model = makeNQueenWithOneAlldifferent(8);
        Solver r = model.getSolver();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(false)));
        r.setDDS(5);
        while (model.getSolver().solve()) ;
        assertEquals(model.getSolver().getSolutionCount(), 2);
        assertEquals(model.getSolver().getNodeCount(), 130);
    }

    @Test(groups="1s", timeOut=60000)
    public void test2DDS2() {
        Model model = new Model();
        Solver r = model.getSolver();
        IntVar[] bs = model.boolVarArray("b", 4);
        r.setSearch(inputOrderLBSearch(bs));
        r.setDDS(3);
        while (model.getSolver().solve()) ;
        assertEquals(model.getSolver().getSolutionCount(), 8);
    }

    @Test(groups="1s", timeOut=60000)
    public void test3() {
        Model model = makeGolombRuler(6);
        Solver r = model.getSolver();
        r.setDFS();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(false)));
        r.showSolutions();
        while (model.getSolver().solve()) ;
        model.getSolver().printShortStatistics();
        assertEquals(model.getSolver().getSolutionCount(), 3);
        assertEquals(model.getSolver().getNodeCount(), 17);
    }

    @Test(groups="1s", timeOut=60000)
    public void test4() {
        Model model = makeNQueenWithOneAlldifferent(8);
        Solver r = model.getSolver();
        r.setDFS();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(false)));
        model.getSolver().setRestarts(limit -> model.getSolver().getNodeCount() >= limit, new LubyCutoffStrategy(2), 2);
        while (model.getSolver().solve()) ;
        model.getSolver().printShortStatistics();
        assertEquals(model.getSolver().getRestartCount(), 2);
    }

    @Test(groups="1s", timeOut=60000)
    public void test5() {
        Model model = makeGolombRuler(5);
        Solver r = model.getSolver();
        r.setDFS();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(false)));
        r.setLNS(new RandomNeighborhood(model.retrieveIntVars(false), 15, 0), new NodeCounter(model, 10));
        r.limitSearch(() -> r.getMeasures().getNodeCount() >= 1000);
        while (model.getSolver().solve()) ;
        model.getSolver().printShortStatistics();
        assertEquals(model.getSolver().getRestartCount(), 314);
    }

    @Test(groups="1s", timeOut=60000)
    public void test6() {
        Model model = makeGolombRuler(6);
        Solver r = model.getSolver();
        r.setDFS();
        r.setSearch(inputOrderLBSearch(model.retrieveIntVars(false)));
        model.getSolver().setLNS(new RandomNeighborhood(model.retrieveIntVars(false), 15, 0), new NodeCounter(model, 10));
        r.addStopCriterion(() -> r.getMeasures().getNodeCount() >= 1000);
        while (model.getSolver().solve()) ;
        model.getSolver().printShortStatistics();
        assertEquals(r.getMeasures().getRestartCount(), 972);
    }


    @Test(groups="1s", timeOut=60000)
    public void test7() {
        Model model = new Model();
        int n = 3;
        BoolVar[] B = model.boolVarArray("b", n - 1);
        Solver r = model.getSolver();
        r.setSearch(inputOrderUBSearch(B));
        r.setLDS(1);
        model.getSolver().showSolutions();
        model.getSolver().showDecisions();
        while (model.getSolver().solve()) ;
        assertEquals(model.getSolver().getSolutionCount(), 4);

    }

    @Test(groups="1s", timeOut=60000)
    public void test8() {
        Model model = new Model();
        int n = 3;
        IntVar[] X = model.intVarArray("X", n, 0, n, false);
        BoolVar[] B = model.boolVarArray("b", n - 1);
        for (int i = 0; i < n - 1; i++) {
            model.arithm(X[i], "<", X[i + 1]).reifyWith(B[i]);
        }
        Solver r = model.getSolver();
        r.setSearch(inputOrderUBSearch(B), greedySearch(inputOrderLBSearch(X)));
        r.setLDS(1);
        r.showDecisions();
        r.showSolutions();
        r.limitSolution(10);
        while (model.getSolver().solve()) ;
        assertEquals(model.getSolver().getSolutionCount(), 4);
    }

    @Test(groups="1s", timeOut=60000)
    public void test9() {
        Model model = new Model();
        int n = 3;
        IntVar[] X = model.intVarArray("X", n, 0, n, false);
        BoolVar[] B = model.boolVarArray("b", n - 1);
        for (int i = 0; i < n - 1; i++) {
            model.arithm(X[i], "<", X[i + 1]).reifyWith(B[i]);
        }

        Solver r = model.getSolver();
        r.setMove(new MoveBinaryLDS(inputOrderUBSearch(B), 1, model.getEnvironment()),
                new MoveBinaryDFS(greedySearch(inputOrderLBSearch(X)))
        );
        model.getSolver().showSolutions();
        model.getSolver().showDecisions();
        r.limitSolution(10);
        while (model.getSolver().solve()) ;
        assertEquals(r.getMeasures().getSolutionCount(), 4);
    }
}
