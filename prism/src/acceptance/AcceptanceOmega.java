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
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import automata.DA;
import explicit.Model;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.PrismNotSupportedException;
import prism.ProbModel;

/**
 * Generic interface for an omega-regular acceptance condition (BitSet-based).
 */
public interface AcceptanceOmega extends Cloneable
{
	/** Returns true if the bottom strongly connected component (BSCC)
	 *  given by bscc_states is accepting for this acceptance condition.
	 *  This method does not test, if bscc_states is an SCC or not!
	 *  @param model is needed for AcceptanceControllerSynthesis
	 **/
	public boolean isBSCCAccepting(BitSet bscc_states, Model model);

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
			lifter.getOrDefault(bit, Collections.emptySet()).stream().forEach(result::set);
		});
		return result;
	}

	/**
	 * Convert this BitSet based acceptance condition to the corresponding BDD based acceptance condition.
	 * @param ddRowVars JDDVars of the row variables corresponding to the model
	 * @param daRowVars JDDVars of the row variables corresponding to the bits in the bitset
	 * @param daColVars JDDVars of the col variables from the model checker
	 * @param allddRowVars JDDVars of the row of the product
	 * @param allddColVars JDDVars of the col of the product
	 * @param da DA to which this acceptance corresponds
	 * @param labelAPs the labels of the DA, only used for transition-based acceptance (but to avoid unneccessary castings in caller-methods, we use it here)
	 * @param product The product of the DA and the Model
	 * @return the corresponding AcceptanceOmegaDD
	 * @throws PrismNotSupportedException is used, because AcceptanceControllerSynthesis is (currently) only possible for the explicit engine
	 */
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars, JDDVars daColVars, JDDVars allddRowVars, JDDVars allddColVars, DA<BitSet, ?> da,
			Vector<JDDNode> labelAPs, ProbModel product) throws PrismNotSupportedException;
}
