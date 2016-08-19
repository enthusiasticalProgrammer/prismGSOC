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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import prism.PrismException;
import strat.MDStrategyArray;
import strat.Strategy;

/**
 * This class verifies multi-long-run properties for DTMCProductMLRStrategyAndMDP,
 * which is obtained by taking the product of an MDP and a strategy for multi-long-run properties 
 */
class MultiLongRunDTMC extends MultiLongRun<ArtificialMdpFromDtmc>
{
	private final DTMCProductMLRStrategyAndMDP dtmc;

	public MultiLongRunDTMC(DTMCProductMLRStrategyAndMDP dtmc, Collection<MDPConstraint> constraints, Collection<MDPObjective> objectives,
			Collection<MDPExpectationConstraint> expConstraints, String method, boolean isConjunctiveSat) throws PrismException
	{
		super(constraints, objectives, expConstraints, method, new ArtificialMdpFromDtmc(dtmc), isConjunctiveSat);
		this.dtmc = dtmc;
	}

	@Override
	protected Iterator<Entry<Integer, Double>> getTransitionIteratorOfModel(int state, int action)
	{
		if (action != 0) {
			throw new IllegalArgumentException("in an MC, we only have one action");
		}

		return dtmc.getTransitionsIterator(state);
	}

	@Override
	protected int prepareStateForReward(int state)
	{
		return state / dtmc.getNumStrategies();
	}

	/**
	 * Since this uses a DTMC, we return a strategy which chooses randomly one out of
	 * one action. At a first glance this may sound awkward, however returning such a strategy
	 * does not harm anybody in this case, and other classes need not care, which subclass of
	 * MultiLongRun they use.
	 */
	@Override
	public Strategy getStrategy()
	{
		int[] choices = new int[dtmc.getNumStates()];
		for (int i = 0; i < choices.length; choices[i++] = -2)
			;
		return new MDStrategyArray(new ArtificialMdpFromDtmc(dtmc), choices);
	}

}
