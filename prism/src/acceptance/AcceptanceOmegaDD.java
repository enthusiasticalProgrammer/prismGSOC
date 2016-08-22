//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import jdd.JDDNode;

/**
 * Generic interface for an omega-regular acceptance condition (BDD-based).
 */
public interface AcceptanceOmegaDD
{
	/** Returns true if the bottom strongly connected component (BSCC)
	 *  given by bscc_states is accepting for this acceptance condition.
	 * <br>
	 * [ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 * 
	 * @param bscc_states the states forming a BSCC as JDDNode
	 * @return true if the BSCC is accepting and els false
	 */
	public boolean isBSCCAccepting(JDDNode bscc_states);

	/**
	 * @return  description of the acceptance condition's size,
	 * i.e. "x Rabin pairs", etc.
	 */
	public String getSizeStatistics();

	/** @return the AcceptanceType of this acceptance condition */
	public AcceptanceType getType();

	/**
	 * Clear the resources used by this acceptance condition.
	 * Call to ensure that the JDD based state sets actually get
	 * dereferenced.
	 */
	void clear();

}
