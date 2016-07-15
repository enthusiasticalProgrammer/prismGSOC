package acceptance;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import acceptance.AcceptanceGenRabinTransition.GenRabinPair;
import acceptance.AcceptanceRabinDD.RabinPairDD;
import common.IterableBitSet;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

public class AcceptanceGenRabinTransitionDD extends AcceptanceGenRabinDD implements AcceptanceOmegaDD
{
	private final JDDVars ddRowVars;
	private final AcceptanceGenRabinTransition accRabinTransition;

	public AcceptanceGenRabinTransitionDD(AcceptanceGenRabinTransition acceptanceGenRabinTransition, JDDVars ddRowVars, Vector<JDDNode> labelForAPs)
	{
		super();
		this.ddRowVars = ddRowVars;
		this.accRabinTransition = acceptanceGenRabinTransition;
		for (AcceptanceGenRabinTransition.GenRabinPair pair : acceptanceGenRabinTransition.accList) {
			super.add(makeGenRabinPairTransitionDD(pair, labelForAPs));
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
		return this.stream().anyMatch(pair -> pair.isBSCCAccepting(bscc_states));
	}

	@Override
	public String getSizeStatistics()
	{
		return this.size() + " Generalized Rabin pairs";
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
			JDDNode transition = JDD.SetVectorElement(JDD.Constant(0), ddRowVars, state, 1.0);

			JDD.Ref(ddRowVars.getVar(state));
			transition = JDD.And(transition, ddRowVars.getVar(state));
			for (int i = 0; i < labelAPs.size(); i++) {
				JDD.Ref(labelAPs.get(i));
				JDDNode ap = labelAPs.get(i);
				if (!edgeLabel.get(i)) {
					ap = JDD.Not(ap);
				}
				transition = JDD.And(transition, ap);
			}
			result = JDD.Or(result, transition);
		}
		return result;
	}

	public class GenRabinPairTransitionDD extends GenRabinPairDD
	{
		/**finite and infinite are states (respectively state-sets) of the product automaton.
		 * Therefore this class could be treated analogously to AcceptanceGenRabinDD after creation.*/

		public GenRabinPairTransitionDD(JDDNode L, List<JDDNode> K_list)
		{
			super(L,K_list);
		}
	}
}
