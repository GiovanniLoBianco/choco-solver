/**
 * This file is part of choco-solver, http://choco-solver.org/
 *
 * Copyright (c) 2019, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.solver.constraints.binary;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.explanations.RuleStore;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.solver.variables.events.IEventType;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.procedure.IntProcedure;

/**
 * X = Y + C
 * <p>
 * <br/>
 *
 * @author Charles Prud'homme, Jean-Guillaume Fages
 * @since 1 oct. 2010
 */

public final class PropEqualX_YC extends Propagator<IntVar> {


    private IntVar x, y;
    private final int cste;
    // incremental filtering of enumerated domains
    private boolean bothEnumerated;
    private IIntDeltaMonitor[] idms;
    private IntProcedure rem_proc;
    private int indexToFilter;
    private int offSet;

    @SuppressWarnings({"unchecked"})
    public PropEqualX_YC(IntVar[] vars, int c) {
        super(vars, PropagatorPriority.BINARY, true);
        this.x = vars[0];
        this.y = vars[1];
        this.cste = c;
        if (x.hasEnumeratedDomain() && y.hasEnumeratedDomain()) {
            bothEnumerated = true;
            idms = new IIntDeltaMonitor[2];
            idms[0] = vars[0].monitorDelta(this);
            idms[1] = vars[1].monitorDelta(this);
            rem_proc = i -> vars[indexToFilter].removeValue(i + offSet, this);
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        if (vars[0].hasEnumeratedDomain() && vars[1].hasEnumeratedDomain())
            return IntEventType.all();
        else
            return IntEventType.boundAndInst();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        updateBounds();
        // ensure that, in case of enumerated domains, holes are also propagated
        if (bothEnumerated) {
            int ub = x.getUB();
            for (int val = x.getLB(); val <= ub; val = x.nextValue(val)) {
                if (!y.contains(val - cste)) {
                    x.removeValue(val, this);
                }
            }
            ub = y.getUB();
            for (int val = y.getLB(); val <= ub; val = y.nextValue(val)) {
                if (!x.contains(val + cste)) {
                    y.removeValue(val, this);
                }
            }
            idms[0].unfreeze();
            idms[1].unfreeze();
        }
        if (x.isInstantiated()) {
            assert (y.isInstantiated());
            setPassive();
        }
    }

    @Override
    public void propagate(int varIdx, int mask) throws ContradictionException {
        updateBounds();
        if (x.isInstantiated()) {
            assert (y.isInstantiated());
            setPassive();
        } else if (bothEnumerated) {
            if (varIdx == 0) {
                indexToFilter = 1;
                offSet = -cste;
            } else {
                indexToFilter = 0;
                offSet = cste;
            }
            idms[varIdx].freeze();
            idms[varIdx].forEachRemVal(rem_proc);
            idms[varIdx].unfreeze();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void updateBounds() throws ContradictionException {
        while (x.updateLowerBound(y.getLB() + cste, this) | y.updateLowerBound(x.getLB() - cste, this)) ;
        while (x.updateUpperBound(y.getUB() + cste, this) | y.updateUpperBound(x.getUB() - cste, this)) ;
    }

    @Override
    public ESat isEntailed() {
        if ((x.getUB() < y.getLB() + cste) ||
                (x.getLB() > y.getUB() + cste) ||
                x.hasEnumeratedDomain() && y.hasEnumeratedDomain() && !match())
            return ESat.FALSE;
        else if (x.isInstantiated() &&
                y.isInstantiated() &&
                (x.getValue() == y.getValue() + cste))
            return ESat.TRUE;
        else
            return ESat.UNDEFINED;
    }


    private boolean match() {
        int lb = x.getLB();
        int ub = x.getUB();
        for (; lb <= ub; lb = x.nextValue(lb)) {
            if (y.contains(lb - cste)) return true;
        }
        return false;
    }

    @Override
    public boolean why(RuleStore ruleStore, IntVar var, IEventType evt, int value) {
        boolean newrules = ruleStore.addPropagatorActivationRule(this);
        if (var.equals(x)) {
            IntEventType ievt = (IntEventType) evt;
            switch (ievt) {
                case REMOVE:
                    newrules |= ruleStore.addRemovalRule(y, value - cste);
                    break;
                case DECUPP:
                    newrules |= ruleStore.addUpperBoundRule(y);
                    break;
                case INCLOW:
                    newrules |= ruleStore.addLowerBoundRule(y);
                    break;
                case INSTANTIATE:
                    newrules |= ruleStore.addFullDomainRule(y);
                    break;
                default: break;// nothing to do
            }
        } else if (var.equals(y)) {
            IntEventType ievt = (IntEventType) evt;
            switch (ievt) {
                case REMOVE:
                    newrules |= ruleStore.addRemovalRule(x, value + cste);
                    break;
                case DECUPP:
                    newrules |= ruleStore.addUpperBoundRule(x);
                    break;
                case INCLOW:
                    newrules |= ruleStore.addLowerBoundRule(x);
                    break;
                case INSTANTIATE:
                    newrules |= ruleStore.addFullDomainRule(x);
                    break;
                default: break;// nothing to do
            }
        } else {
            newrules |= super.why(ruleStore, var, evt, value);
        }
        return newrules;
    }

    @Override
    public String toString() {
        StringBuilder bf = new StringBuilder();
        bf.append("prop(").append(vars[0].getName()).append(".EQ.").append(vars[1].getName());
        if (cste != 0) {
            bf.append("+").append(cste);
        }
        bf.append(")");
        return bf.toString();
    }
}
