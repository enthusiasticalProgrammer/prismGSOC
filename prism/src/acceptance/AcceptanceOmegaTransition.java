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

package acceptance;

import java.util.BitSet;
import java.util.Map;

/**
 * This interface defines all methods for transition-based omega-acceptance-conditions
 */
public interface AcceptanceOmegaTransition extends AcceptanceOmega
{
	/** 
	 * Get the acceptance signature for state {@code stateIndex} (HOA format).
	 * 
	 * @param startState the state-number for which  the outgoing edge is to be going
	 * @param label the edge-label
	 * @return an acceptance-signature which has to be in HOA-format
	 */
	public String getSignatureForEdgeHOA(int startState, BitSet label);

	@Override
	public default String getSignatureForState(int stateIndex)
	{
		return "";
	}

	@Override
	public default String getSignatureForStateHOA(int stateIndex)
	{
		return "";
	}

	/**
	 * This method is used for removing any edges which are never traversed in the explicit DA-Model-Product. The parameter
	 * is a map storing the only usable edges (making the acceptance de facto state-based, but de typo it stays transition-based,
	 * because transforming the BitSets storing edges to BitSets storing states is neither tidy nor maintainable)
	 * 
	 * @param usedEdges the edges, which are used (e.g. 0->x in usedEdges means that from state-number 0 the label x is used)
	 */
	public void removeUnneccessaryProductEdges(Map<Integer, BitSet> usedEdges);
}
