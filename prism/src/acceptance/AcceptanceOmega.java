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

import java.io.PrintStream;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import jdd.JDDNode;
import jdd.JDDVars;

/**
 * Generic interface for an omega-regular acceptance condition (BitSet-based).
 */
public interface AcceptanceOmega extends Cloneable
{
	/** Returns true if the bottom strongly connected component (BSSC)
	 *  given by bscc_states is accepting for this acceptance condition.
	 **/
	public boolean isBSCCAccepting(BitSet bscc_states);

	/**
	 * Get a string describing the acceptance condition's size,
	 * i.e. "x Rabin pairs", etc.
	 */
	public String getSizeStatistics();

	/** Returns the AcceptanceType of this acceptance condition */
	public AcceptanceType getType();

	/** Print the appropriate Acceptance (and potentially acc-name) header */
	public void outputHOAHeader(PrintStream out);

	/** Make a copy of the acceptance condition. */
	public AcceptanceOmega clone();

	/** 
	 * Get the acceptance signature for state {@code stateIndex}
	 */
	public String getSignatureForState(int stateIndex);

	/** 
	 * Get the acceptance signature for state {@code stateIndex} in HOA format.
	 */
	public String getSignatureForStateHOA(int stateIndex);

	/**
	 * The lifter basically maps an automaton-state to its corresponding states in the Product construction.
	 * This function should lift the numbers of the BitSets according to the lifter.
	 **/
	public void lift(Map<Integer, Collection<Integer>> lifter);

	default BitSet liftBitSet(Map<Integer, Collection<Integer>> lifter, BitSet bs)
	{
		BitSet result = new BitSet();
		bs.stream().forEach(bit -> {
			lifter.get(bit).stream().forEach(result::set);
		});
		return result;
	}

	/**
	 * Convert this BitSet based acceptance condition to the corresponding BDD based acceptance condition.
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the bitset
	 * @param labelAPs: the labels of the DA, only used for transition-based acceptance (but to avoid unneccessary castings in caller-methods, we use it here)
	 */
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars, Vector<JDDNode> labelAPs);
}
