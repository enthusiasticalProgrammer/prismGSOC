package acceptance;

import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import automata.DA;
import jdd.JDDNode;
import jdd.JDDVars;
import ltl.parser.Comparison;
import prism.ProbModel;

public class AcceptanceControllerSynthesis extends AcceptanceGenRabinTransition
{

	public AcceptanceControllerSynthesis(DA<BitSet, ?> da)
	{
		super(da);
		// TODO Auto-generated constructor stub
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.CONTROLLER_SYNTHESIS_ACCEPTANCE;
	}

	@Override
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars, JDDVars daColVars, JDDVars allddRowVars, JDDVars allddColVars, DA<BitSet, ?> da,
			Vector<JDDNode> labelAPs, ProbModel product)
	{
		throw new UnsupportedOperationException("This is not yet implemented (and the super-class-method would compute an uncorrect result");
	}

	@Override
	public AcceptanceControllerSynthesis or(AcceptanceGenRabinTransition other)
	{
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public class AccControllerPair extends GenRabinPair
	{

		final List<MDPCondition> mdpCondition;

		public AccControllerPair(BitSet Finite, List<BitSet> Infinite, List<MDPCondition> mdpCondition)
		{
			super(Finite, Infinite);
			this.mdpCondition = mdpCondition;
		}

		/**
		 * checks if the finite-infinite conditions holds as well as the mdpConditions 
		 */
		@Override
		public boolean isBSCCAccepting(BitSet bscc_states)
		{
			throw new UnsupportedOperationException("Not yet implemented");
		}

		@Override
		public AccControllerPair clone()
		{
			throw new UnsupportedOperationException("Not yet implemented");
		}

		@Override
		public String toString()
		{
			String s = super.toString();
			s += "mdp:";
			for (MDPCondition mdp : mdpCondition) {
				s += " " + mdp.toString();
			}
			return s;
		}

		@Override
		protected void lift(Map<Integer, Collection<Integer>> lifter)
		{
			super.lift(lifter);
			mdpCondition.stream().forEach(mdp -> {
				BiMap<BitSet, Integer> newAccSet = HashBiMap.create();
				for (Entry<BitSet, Integer> acc : mdp.acceptanceSet.entrySet()) {
					newAccSet.put(transformSingleBitSet(acc.getKey(), lifter), acc.getValue());
				}
				mdp.acceptanceSet = newAccSet;
			});
		}
	}

	/**
	 * This class stores an acceptance set (as BitSet) and some info regarding the condition, i.e. lim inf/sup (in the long run)
	 * of visiting these states has to be >/</<=/>= a bound (usually the bound is between 0 and 1. 
	 */
	public class MDPCondition
	{
		public BiMap<BitSet, Integer> acceptanceSet; //means each edge gets a certain Integer-reward (or zero)
		public final double bound;
		public final Comparison cmpOperator;
		public final boolean isLimInf;

		public MDPCondition(BiMap<BitSet, Integer> acceptanceSet, double bound, Comparison cmpOperator, boolean isLimInf)
		{
			this.acceptanceSet = acceptanceSet;
			this.bound = bound;
			this.cmpOperator = cmpOperator;
			this.isLimInf = isLimInf;
		}

		@Override
		public String toString()
		{
			return "bound: " + bound + cmpOperator + isLimInf + " rewards: " + acceptanceSet;
		}
	}
}
