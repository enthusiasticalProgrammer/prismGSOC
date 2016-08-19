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

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import prism.PrismException;
import strat.MultiLongRunStrategy;

public class TestDTMCProductMLRStrategyAndMDP
{

	DTMCProductMLRStrategyAndMDP dtmc;

	@Before
	public void init() throws PrismException
	{
		TestMultiLongRun tmlr = new TestMultiLongRun();
		tmlr.setUp();
		MultiLongRun<?> ml11 = tmlr.mdp11.createMultiLongRun(tmlr.m1, tmlr.e1);
		ml11.createMultiLongRunLP();
		ml11.solveDefault();
		MultiLongRunStrategy strat = (MultiLongRunStrategy) ml11.getStrategy();
		dtmc = (DTMCProductMLRStrategyAndMDP) strat.buildProduct(tmlr.m1);
	}

	@Test
	public void testInitialStateHasCorrectSuccessors()
	{
		Iterator<Integer> s = dtmc.getSuccessorsIterator(0);
		Set<Integer> successors = new HashSet<>();
		s.forEachRemaining(i -> successors.add(i));

		Set<Integer> correctSuccessors = new HashSet<>();
		correctSuccessors.add(13);
		correctSuccessors.add(14);
		correctSuccessors.add(7);

		assertEquals(correctSuccessors, successors);
	}

	@Test
	public void testNumberOfTransitions()
	{
		//actually we have only 13 transitions, but the ones from
		//unreachable states are also counted
		assertEquals(23, dtmc.getNumTransitions());
	}
}
