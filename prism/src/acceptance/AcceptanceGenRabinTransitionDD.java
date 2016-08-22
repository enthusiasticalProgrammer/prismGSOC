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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import acceptance.AcceptanceGenRabinTransition.GenRabinPair;
import automata.DA;
import common.IterableBitSet;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.ProbModel;

/**
 * This class models a generalised transition-based Rabin-acceptance with BDDs instead of BitSets 
 */
public class AcceptanceGenRabinTransitionDD implements AcceptanceOmegaDD
{
	private final JDDVars daRowVars;
	private final JDDVars daColVars;
	private final JDDVars allDDRowVars;
	private final JDDVars allDDColVars;
	private final DA<BitSet, ?> da;
	private final AcceptanceGenRabinTransition accRabinTransition;

	/**
	 * list of transition-based generalised Rabin pairs 
	 */
	public final List<GenRabinPairTransitionDD> accList;
	private final ProbModel product;

	AcceptanceGenRabinTransitionDD(AcceptanceGenRabinTransition acceptanceGenRabinTransition, JDDVars daRowVars, JDDVars daColVars, JDDVars allDDRowVars,
			JDDVars allDDColVars, DA<BitSet, ?> da, Vector<JDDNode> labelForAPs, ProbModel product)
	{
		super();
		this.daRowVars = daRowVars;
		this.daColVars = daColVars;
		this.allDDRowVars = allDDRowVars;
		this.allDDColVars = allDDColVars;
		this.da = da;
		this.accRabinTransition = acceptanceGenRabinTransition;
		this.accList = new ArrayList<>(acceptanceGenRabinTransition.accList.size());
		this.product = product;
		for (AcceptanceGenRabinTransition.GenRabinPair pair : acceptanceGenRabinTransition.accList) {
			accList.add(makeGenRabinPairTransitionDD(pair, labelForAPs));
		}
	}

	private GenRabinPairTransitionDD makeGenRabinPairTransitionDD(GenRabinPair pair, Vector<JDDNode> labelForAPs)
	{
		JDDNode fin = convertAccSetToJDDNode(pair.Finite, labelForAPs);
		List<JDDNode> inf = pair.Infinite.stream().map(set -> convertAccSetToJDDNode(set, labelForAPs)).collect(Collectors.toList());
		return new GenRabinPairTransitionDD(fin, inf);
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		return accList.stream().anyMatch(pair -> pair.isBSCCAccepting(bscc_states));
	}

	@Override
	public String getSizeStatistics()
	{
		return this.accList.size() + " Generalized Rabin pairs";
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.GENERALIZED_RABIN_TRANSITION_BASED;
	}

	/**
	 * This method converts an edge-set represented by a BitSet into a JDDNode.
	 * The JDD.Ref and JDD.Deref calls should cancel out in a way such that only
	 * the final result is Ref'd and everything else stays as before. 
	 */
	private JDDNode convertAccSetToJDDNode(BitSet accSet, Vector<JDDNode> labelAPs)
	{
		JDDNode result = JDD.Constant(0);
		for (int edge : IterableBitSet.getSetBits(accSet)) {
			int state = this.accRabinTransition.computeStartStateOfEdge(edge);
			BitSet edgeLabel = accRabinTransition.computeBitSetOfEdge(edge);
			JDDNode label = JDD.Constant(1);
			for (int i = 0; i < labelAPs.size(); i++) {
				JDD.Ref(labelAPs.get(i));
				JDDNode ap = labelAPs.get(i);
				if (!edgeLabel.get(i)) {
					ap = JDD.Not(ap);
				}
				label = JDD.And(label, ap);
			}
			label = JDD.PermuteVariables(label, allDDRowVars, allDDColVars);
			JDDNode transition = JDD.SetMatrixElement(JDD.Constant(0), daRowVars, daColVars, state, da.getEdgeDestByLabel(state, edgeLabel), 1);
			transition = JDD.And(transition, label);
			result = JDD.Or(result, transition);
		}
		return result;
	}

	/**
	 * a generalised transition-based Rabin-pair modeled with BDD instead of BitSets 
	 */
	public class GenRabinPairTransitionDD
	{
		/**
		 * The transition set, which is to be traversed at most finitely often 
		 */
		public final JDDNode finTransitions;

		/**
		 * The transition sets, which are to be traversed at least infinitely often  
		 */
		public final List<JDDNode> infTransitions;

		private GenRabinPairTransitionDD(JDDNode finTransitions, List<JDDNode> infTransitions)
		{
			this.finTransitions = finTransitions;
			this.infTransitions = infTransitions;
		}

		/**
		 * This method checks if a BSCC is accepting regarding this particular acceptance pair
		 * 
		 * @param bsccStates the BSCC
		 * @return true if the BSCC is accepting regarding this pair and false otherwise
		 * 
		 */
		public boolean isBSCCAccepting(JDDNode bsccStates)
		{
			JDDNode bsccEdges = JDD.Apply(JDD.TIMES, product.getTrans(), bsccStates);
			return (!JDD.AreIntersecting(bsccEdges, finTransitions)) && infTransitions.stream().allMatch(inf -> JDD.AreIntersecting(inf, bsccEdges));
		}

		private void clear()
		{
			JDD.Deref(finTransitions);
			infTransitions.forEach(JDD::Deref);
		}
	}

	@Override
	public void clear()
	{
		this.accList.forEach(a -> a.clear());
	}
}
