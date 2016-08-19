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

package rabinizerPRISMAdapter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.BitSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;

import acceptance.AcceptanceGenRabinTransition;
import automata.DA;
import jltl2ba.SimpleLTL;

public class TestLTL2DA
{
	// X X a
	public static SimpleLTL formula1 = new SimpleLTL(SimpleLTL.LTLType.NEXT, new SimpleLTL(SimpleLTL.LTLType.NEXT, new SimpleLTL("a")));

	@Test
	public void TestNoExceptionOccurringForFormula1()
	{
		assertNotNull(LTL2DA.getDA(formula1));
	}

	@Test
	public void HasEdgesWithFirstPropositionNegated()
	{
		DA<BitSet, ? extends AcceptanceGenRabinTransition> da = LTL2DA.getDA(formula1);
		BitSet negatedA = new BitSet(1);
		negatedA.set(0, false);
		assertTrue(da.hasEdge(da.getStartState(), negatedA));
	}

	@Test
	public void EdgeOffsetIsNeverNegative()
	{
		DA<BitSet, ? extends AcceptanceGenRabinTransition> da = LTL2DA.getDA(formula1);
		for (int i = 0; i < da.size(); i++) {
			Set<DA<BitSet, ? extends AcceptanceGenRabinTransition>.Edge> edgesFromState = da.getAllEdgesFrom(i);
			for (DA<BitSet, ? extends AcceptanceGenRabinTransition>.Edge edge : edgesFromState) {
				assertTrue(edge.dest >= 0);
			}
		}
	}

	@Test
	public void OneStateHasNotOutgoingEdgesForEachBitSet()
	{
		DA<BitSet, ? extends AcceptanceGenRabinTransition> da = LTL2DA.getDA(formula1);
		BitSet bs = new BitSet(1);
		bs.set(0, false);
		assertTrue(IntStream.range(0, da.size()).anyMatch(s -> !da.hasEdge(s, bs)));

	}
}
