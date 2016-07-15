package acceptance;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import org.eclipse.jdt.annotation.NonNull;

import acceptance.AcceptanceGenRabinTransition.GenRabinPair;
import common.IterableBitSet;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

public class AcceptanceGenRabinTransitionDD implements AcceptanceOmegaDD
{
	private final JDDVars ddRowVars;
	private final @NonNull List<@NonNull GenRabinPairTransitionDD> pairs;
	private final @NonNull AcceptanceGenRabinTransition accRabinTransition;

	public AcceptanceGenRabinTransitionDD(@NonNull AcceptanceGenRabinTransition acceptanceGenRabinTransition, JDDVars ddRowVars, Vector<JDDNode> labelForAPs)
	{
		this.ddRowVars = ddRowVars;
		this.pairs = new ArrayList<>();
		this.accRabinTransition = acceptanceGenRabinTransition;
		for (AcceptanceGenRabinTransition.GenRabinPair pair : acceptanceGenRabinTransition.accList) {
			pairs.add(new GenRabinPairTransitionDD(pair, labelForAPs));
		}
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		return pairs.stream().anyMatch(pair -> pair.isBSCCAccepting(bscc_states));
	}

	@Override
	public String getSizeStatistics()
	{
		return pairs.size() + " Generalized Rabin pairs";
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.GENERALIZED_RABIN;
	}

	@Override
	public void clear()
	{
		pairs.forEach(p -> p.clear());
	}

	/**
	 * This method converts an edge-set represented by a BitSet into a JDDNode.
	 * The JDD.Ref and JDD.Deref calls should cancel out in a way such that only
	 * the final result is Ref'd and everything else stays as before. 
	 */
	private JDDNode convertAccSetToJDDNode(@NonNull BitSet accSet, Vector<JDDNode> labelAPs)
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

	//TODO: this might be thrown out
	private BitSet transformJDDToBitSet(JDDNode bscc)
	{
		BitSet result = new BitSet(ddRowVars.getNumVars());
		for (int i = 0; i < ddRowVars.getNumVars(); i++) {
			if (JDD.GetVectorElement(bscc, ddRowVars, i) > 0.5) {
				result.set(i);
			}
		}
		return result;
	}

	private class GenRabinPairTransitionDD
	{
		/**finite and infinite are states (respectively state-sets) of the product automaton.
		 * Therefore this class could be treated analogously to AcceptanceGenRabinDD after creation.*/
		private final JDDNode finite;
		private final @NonNull List<JDDNode> infinite;

		public GenRabinPairTransitionDD(GenRabinPair pair, Vector<JDDNode> labelForAPs)
		{
			finite = convertAccSetToJDDNode(pair.Finite, labelForAPs);
			infinite = new ArrayList<>();
			for (BitSet inf : pair.Infinite) {
				infinite.add(convertAccSetToJDDNode(inf, labelForAPs));
			}
		}

		public boolean isBSCCAccepting(JDDNode bscc_states)
		{
			return (!JDD.AreIntersecting(bscc_states, finite)) && infinite.stream().allMatch(inf -> JDD.AreIntersecting(inf, bscc_states));
		}

		private void clear()
		{
			if (finite != null) {
				JDD.Deref(finite);
			}
			infinite.stream().filter(inf -> inf != null).forEach(JDD::Deref);
		}
	}
}
