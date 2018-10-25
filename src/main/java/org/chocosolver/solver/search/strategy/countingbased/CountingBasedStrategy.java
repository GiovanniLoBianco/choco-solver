package org.chocosolver.solver.search.strategy.countingbased;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperator;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.countingbased.tools.CountingTools;
import org.chocosolver.solver.search.strategy.countingbased.tools.IntVarAssignment;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.decision.IntDecision;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.PoolManager;

/**
 * A class to represent strategies that are based on couting solutions. It
 * computes solution densities for each constraint and uses them to guide the
 * search.
 * 
 * From these densities, we compute an ordered list of remaining assignments to
 * test. The ordering settings differs depending on the counting based strategy
 * that is used. This list is backtrackable.
 * 
 * When exploring a new subtree, we do not systematically recompute entirely the
 * ordering list, as it can be very costly. We only recompute it sometimes 1)
 * when reaching a certain depth in the search tree 2) or when domains size have
 * decreased enough
 * 
 * When backtracking to a parent node, we use the same ordering list that was
 * used back then, deleting of course the assignments that have been already
 * explored.
 * 
 * @author giovannilobianco
 * @since octobre 2018
 */
public abstract class CountingBasedStrategy extends AbstractStrategy<IntVar> {

	// ***********************************************************************************
	// VARIABLES
	// ***********************************************************************************

	/**
	 * Pool of decisions of the strategy
	 */
	private PoolManager<IntDecision> pool = new PoolManager<IntDecision>();

	/**
	 * Model of the problem that is solved
	 */
	private Model model;

	/**
	 * Array of every propagators in which solution densities can be computed
	 */
	private Countable[] countables;

	/**
	 * Tools to help computing counting algorithms i countable propagators
	 */
	private CountingTools tools;

	/**
	 * Sorted array containing assignments to explore starting with order[next]
	 */
	protected IntVarAssignment[] order;

	/**
	 * Index of the next assignment to explore in the order array
	 */
	private int next;

	// Those following variables define the estimator that will be used on the
	// concerned constraints
	private String estimatorAlldifferent = CountingEstimators.ALLDIFFERENT_PQZ;
	private String estimatorGCC = CountingEstimators.GCC_CORRECTION;

	// A threshold to define if order need to updated
	private double threshold = 1.0;

	// The product of the size of the cartesian product of every domain (must be
	// backtrackable)
	private long sizeDomains;

	// A decision that is trivially wrong and not refutable
	private final IntDecision WRONG;

	// ***********************************************************************************
	// CONSTRUCTORS
	// ***********************************************************************************

	/**
	 * Create a Counting Based Strategy, initializing the decision pool and the
	 * countable propagators array on which will be computed solution densities.
	 * The initialization of the ordering lists is done after in the method
	 * computeOrder().
	 * 
	 * @param model
	 *            the model of the problem that is solved
	 */
	public CountingBasedStrategy(Model model) {
		super(model.retrieveIntVars(true));
		this.pool = new PoolManager<IntDecision>();
		this.model = model;

		// Initializing the countables array
		List<Countable> countableList = new ArrayList<Countable>();
		for (Constraint c : model.getCstrs()) {
			for (Propagator prop : c.getPropagators())
				if (prop instanceof Countable) {
					Countable castProp = (Countable) prop;
					countableList.add(castProp);
				}
		}
		this.countables = new Countable[countableList.size()];
		for (int k = 0; k < countableList.size(); k++) {
			this.countables[k] = countableList.get(k);
		}

		// We initialize the size of the domains
		this.sizeDomains = 1;
		for (IntVar x : model.retrieveIntVars(true)) {
			this.sizeDomains *= x.getDomainSize();
		}

		this.tools = new CountingTools();

		WRONG = new IntDecision(pool);
		WRONG.set(model.intVar(0), 1, DecisionOperatorFactory.makeIntEq());
		// WRONG.rewind();
		WRONG.setRefutable(false);
	}

	// ***********************************************************************************
	// METHODS
	// ***********************************************************************************

	public Decision<IntVar> getDecision() {

		// We need to save the state of "next" and "order" in order to
		// retrieve it when we will backtrack
		int nextSave = next;
		IntVarAssignment[] orderSave = order;
		long sizeDomainsSave = sizeDomains;

		// If it is the first decision we make or if the order list need to
		// be updated, then we compute it
		if (order == null || needUpdate()) {
			computeOrder();
			if (order == null) {
				// This decision is wrong and cannot be refutable
				return WRONG;
			}
			next = 0;
		}

		// We look for the next possible assignment in order. As
		// we do not recompute the order array at each decision, some
		// assignments might be impossible at this stage.
		int k = next;
		while (k < order.length
				&& (order[k].getVar().isInstantiated() || !order[k].getVar().contains(order[k].getVal()))) {
			k++;
		}
		next = k;

		// Initializing the decision pool
		IntDecision d = pool.getE();
		if (d == null)
			d = new IntDecision(pool);

		if (next == order.length) {
			// If there remains no assignment, it means that every variable
			// that is contained in countable constraints is already
			// instantiated. Then we apply a default strategy to choose the
			// next assignment for the variables that we did not consider
			// yet.
			for (IntVar v : this.getVariables()) {
				if (!v.isInstantiated()) {
					d.set(v, v.getLB(), DecisionOperatorFactory.makeIntEq());
					return d;
				}
			}
			// TODO : renvoyer null et faire un séquenceur

		} else {
			// Otherwise, we find the next decision
			IntVarAssignment nextAssignment = order[next];
			d.set(nextAssignment.getVar(), nextAssignment.getVal(), DecisionOperatorFactory.makeIntEq());

			// We define how we backtrack
			model.getEnvironment().save(() -> {
				this.next = nextSave;
				this.order = orderSave;
				this.sizeDomains = sizeDomainsSave;
			});
			return d;
		}

		// If we get here, then every variables are instantiated, then we return
		// null
		return null;
	}

	abstract public void computeOrder();

	/**
	 * We consider that the order array needs to be updated if the ratio between
	 * the last sizeDomains and the current sizeDomains is less than or equal to
	 * the prefixed threshold
	 * 
	 * @return true if the order array need to be updated
	 */
	public boolean needUpdate() {

		return false;

		/*
		 * long current = 1; for (IntVar x : model.retrieveIntVars(true)) {
		 * current *= x.getDomainSize(); } if (current * 1.0 / sizeDomains <=
		 * threshold) { System.out.println("Update"); this.sizeDomains =
		 * current; return true; } else { return false; }
		 */
	}

	// ***********************************************************************************
	// GETTERS & SETTERS
	// ***********************************************************************************

	public Countable[] getCountables() {
		return countables;
	}

	public void setEstimatorAlldifferent(String estimator) {
		this.estimatorAlldifferent = estimator;
	}

	public void setEstimatorGCC(String estimator) {
		this.estimatorGCC = estimator;
	}

	public String getEstimatorAlldifferent() {
		return estimatorAlldifferent;
	}

	public String getEstimatorGCC() {
		return estimatorGCC;
	}

	public CountingTools getTools() {
		return tools;
	}

	public void setThreshold(double t) {
		this.threshold = t;
	}

}