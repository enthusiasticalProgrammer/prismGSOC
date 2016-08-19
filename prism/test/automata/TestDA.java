//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors: 
// * Christopher Ziegler <ga25suc@mytum.de>
//	
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

package automata;

import static org.junit.Assert.*;

import java.util.BitSet;

import org.junit.Test;

import jltl2ba.SimpleLTL;

public class TestDA
{

	@Test
	public void testMakeComplete_GaRabinizer()
	{
		SimpleLTL formulaGa = new SimpleLTL("a");
		formulaGa = new SimpleLTL(SimpleLTL.LTLType.GLOBALLY, formulaGa);
		DA<BitSet, ?> da = rabinizerPRISMAdapter.LTL2DA.getDA(formulaGa);
		da.complete();
		assertEquals(2, da.size());
		for (int state = 0; state < 2; state++) {
			assertEquals(2, da.getNumEdges(state));
		}
	}
}