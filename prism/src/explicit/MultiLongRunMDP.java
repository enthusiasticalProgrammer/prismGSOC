//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Christopher Ziegler <ga25suc@mytum.de>
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import prism.PrismException;
import strat.MultiLongRunStrategy;
import strat.XiNStrategy;

/**
 * This class verifies multi-long-run properties for MDPs. 
 */
class MultiLongRunMDP extends MultiLongRun<MDP>
{

	public MultiLongRunMDP(MDP mdp, Collection<MDPConstraint> constraints, Collection<MDPObjective> objectives,
			Collection<MDPExpectationConstraint> expConstraints, String method, boolean isConjunctiveSat) throws PrismException
	{
		super(constraints, objectives, expConstraints, method, mdp, isConjunctiveSat);
	}

	@Override
	protected int prepareStateForReward(int state)
	{
		return state;
	}

	@Override
	public MultiLongRunStrategy getStrategy()
	{
		double[] lpResult;
		lpResult = solver.getVariableValues(); //computeStrategy actually just added some constraints, which were already there

		int numStates = model.getNumStates();
		Distribution[] transientDistribution = new Distribution[numStates];
		Distribution[] switchProbability = new Distribution[numStates];

		for (int state = 0; state < numStates; state++) {
			double transientSum = 0.0;
			for (int j = 0; j < model.getNumChoices(state); j++) {
				transientSum += lpResult[getVarY(state, j)];
			}
			transientDistribution[state] = getTransientDistributionAt(state, transientSum, lpResult);
		}

		for (BitSet mec : this.mecs) {
			Distribution mecDistribution = this.getSwitchProbabilityAt(mec);
			for (int state = mec.nextSetBit(0); state >= 0; state = mec.nextSetBit(state + 1)) {
				switchProbability[state] = mecDistribution;
			}
		}

		for (int state = 0; state < numStates; state++) {//To circumvent future NPEs
			if (switchProbability[state] == null) {
				switchProbability[state] = new Distribution();
			}
		}

		try {
			if (!solver.getBoolResult()) {
				//LP is infeasible => no strategy exists
				return null;
			}
		} catch (PrismException e) {
			return null;
		}
		return new MultiLongRunStrategy(transientDistribution, switchProbability, getReccurrentDistribution());
	}

	//indirection: N (as number)
	private Distribution getSwitchProbabilityAt(BitSet mec)
	{
		double[] inBetweenResult = new double[1 << getN()];
		for (int state = mec.nextSetBit(0); state >= 0; state = mec.nextSetBit(state + 1)) {
			for (int N = 0; N < 1 << getN(); N++) {
				inBetweenResult[N] += solver.getVariableValues()[getVarZ(state, N)];
			}
		}

		Distribution result = new Distribution();
		for (int i = 0; i < inBetweenResult.length; i++) {
			result.add(i, inBetweenResult[i]);
		}
		return result;
	}

	private XiNStrategy[] getReccurrentDistribution()
	{
		XiNStrategy[] strategies = new XiNStrategy[1 << getN()];
		for (int i = 0; i < 1 << getN(); i++) {
			strategies[i] = new XiNStrategy(solver, model, this, (isConjunctiveSat ? i : i * 1 << (getNBackend() - 1)));
		}

		return strategies;
	}

	private Distribution getTransientDistributionAt(int state, double transientSum, double[] resSt)
	{
		if (transientSum > 0.0) {
			Distribution result = new Distribution();

			for (int j = 0; j < model.getNumChoices(state); j++) {
				result.add(j, resSt[getVarY(state, j)]);
			}
			return result;
		}
		return null;
	}

	@Override
	protected Iterator<Entry<Integer, Double>> getTransitionIteratorOfModel(int state, int action)
	{
		return model.getTransitionsIterator(state, action);
	}
}
