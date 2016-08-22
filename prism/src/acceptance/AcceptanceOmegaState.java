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
import java.util.Vector;

import automata.DA;
import explicit.Model;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.PrismException;
import prism.ProbModel;

/**
 * This interface defines all methods for state-based omega-conditions 
 */
public interface AcceptanceOmegaState extends AcceptanceOmega
{

	/**
	 * Complement the acceptance condition if possible by adjusting just the condition and not touching the underlying DA.
	 * 
	 * @param numStates the number of states in the underlying model / automaton (needed for complementing BitSets)
	 * @param allowedAcceptance the allowed acceptance types that may be used for complementing
	 * @return the complement of the executing object
	 * @throws PrismException is thrown when creating a complement with the allowed acceptance is impossible
	 */
	public AcceptanceOmegaState complement(int numStates, AcceptanceType... allowedAcceptance) throws PrismException;

	/**
	 * @return a generic acceptance condition which accepts the same as this acceptance condition does
	 */
	public AcceptanceGeneric toAcceptanceGeneric();

	/**
	 * Convert this BitSet based acceptance condition to the corresponding BDD based acceptance condition.
	 * 
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the BitSet
	 * @return the BDD based acceptance condition
	 */
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars);

	/**
	 * It is not recommended to override this method, due to the fact that the other parameters are
	 * not necessary for state-based acceptance. It is better to override the other toAcceptanceDD method. 
	 */
	@Override
	default AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars, JDDVars daColVars, JDDVars allddRowVars, JDDVars allddColVars, DA<BitSet, ?> da,
			Vector<JDDNode> labelAPs, ProbModel modelProduct)
	{
		return toAcceptanceDD(ddRowVars);
	}

	/**
	 * It is not recommended to override this method, due to the fact that the other parameters are
	 * not necessary for state-based acceptance. It is better to override the other isBSCCAccepting method. 
	 */
	@Override
	default boolean isBSCCAccepting(BitSet bscc_states, Model model)
	{
		return isBSCCAccepting(bscc_states);
	}

	/** Returns true if the bottom strongly connected component (BSCC)
	 *  given by bscc_states is accepting for this acceptance condition.
	 *  <p>
	 *  This method does not test, if bscc_states is an SCC or not!
	 *  
	 * @param bscc_states the set of states forming the BSCC
	 * @return true if the input-BSCC is accepting
	 */
	public boolean isBSCCAccepting(BitSet bscc_states);
}
