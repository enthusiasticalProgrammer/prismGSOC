package acceptance;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

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

	public AcceptanceGenRabinTransitionDD(AcceptanceGenRabinTransition acceptanceGenRabinTransition, JDDVars ddRowVars)
	{
		this.ddRowVars = ddRowVars;
		this.pairs = new ArrayList<>();
		for (AcceptanceGenRabinTransition.GenRabinPair pair : acceptanceGenRabinTransition.accList) {
			pairs.add(new GenRabinPairTransitionDD(pair));
		}
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bsccEdges)
	{
		return pairs.stream().anyMatch(pair -> pair.isBSCCAccepting(bsccEdges));
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


	private JDDNode convertBitSetToJDDNode(@NonNull BitSet bitset)
	{
		JDDNode result = JDD.Constant(0);
		for (int i : IterableBitSet.getSetBits(bitset)) {
			result = JDD.SetVectorElement(result, ddRowVars, i, 1.0);
		}
		return result;
	}

	private class GenRabinPairTransitionDD
	{
		private final JDDNode finite;
		private final @NonNull List<JDDNode> infinite;

		public GenRabinPairTransitionDD(GenRabinPair pair)
		{
			finite = convertBitSetToJDDNode(pair.Finite);
			infinite = new ArrayList<>();
			for (BitSet inf : pair.Infinite) {
				infinite.add(convertBitSetToJDDNode(inf));
			}
		}

		private boolean isBSCCAccepting(JDDNode bsccEdges)
		{
			if (JDD.AreIntersecting(bsccEdges, finite)) {
				return false;
			}
			return infinite.stream().allMatch(inf -> JDD.AreIntersecting(inf, bsccEdges));
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
