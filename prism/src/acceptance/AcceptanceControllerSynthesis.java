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
import explicit.ArtificialMdpFromDtmc;
import explicit.DTMC;
import explicit.Model;
import explicit.MultiLongRunControllerSynthesis;
import jdd.JDDNode;
import jdd.JDDVars;
import ltl.parser.Comparison;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.ProbModel;

public class AcceptanceControllerSynthesis extends AcceptanceGenRabinTransition
{

	public AcceptanceControllerSynthesis(DA<BitSet, ?> da)
	{
		super(da);
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.CONTROLLER_SYNTHESIS_ACCEPTANCE;
	}

	/**
	 * checks if the finite-infinite conditions holds as well as the mdpConditions 
	 */
	@Override
	public boolean isBSCCAccepting(BitSet bscc_states, Model model)
	{
		if (model instanceof DTMC) {
			DTMC dtmc = (DTMC) model;
			MultiLongRunControllerSynthesis mlrcs = new MultiLongRunControllerSynthesis(new ArtificialMdpFromDtmc(dtmc),
					AcceptanceControllerSynthesis.this, "Linear programming");
			try {
				return mlrcs.computeStatesInAcceptingMECs().intersects(bscc_states);
			} catch (PrismException e) {
				throw new RuntimeException(e);
			}
		}
		throw new UnsupportedOperationException("Controller synthesis is not yet implemented for anything besides DTMCs and MDPs");
	}

	@Override
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars, JDDVars daColVars, JDDVars allddRowVars, JDDVars allddColVars, DA<BitSet, ?> da,
			Vector<JDDNode> labelAPs, ProbModel product) throws PrismNotSupportedException
	{
		throw new PrismNotSupportedException("AcceptanceControllerSynthesis is currently only supported for explicit engine");
	}

	@Override
	public AcceptanceControllerSynthesis or(AcceptanceGenRabinTransition other)
	{
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public class AccControllerPair extends GenRabinPair
	{

		public final List<MDPCondition> mdpCondition;

		public AccControllerPair(BitSet Finite, List<BitSet> Infinite, List<MDPCondition> mdpCondition)
		{
			super(Finite, Infinite);
			this.mdpCondition = mdpCondition;
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

		@Override
		protected void removeUnneccessaryProductEdges(Map<Integer, BitSet> usedEdges)
		{
			super.removeUnneccessaryProductEdges(usedEdges);
			for (MDPCondition mdp : mdpCondition) {
				BiMap<BitSet, Integer> newAccSet = HashBiMap.create();
				for (Entry<BitSet, Integer> acc : mdp.acceptanceSet.entrySet()) {
					BitSet newBS = (BitSet) acc.getKey().clone();
					removeUnneccessaryProductEdgesForSet(usedEdges, newBS);
					newAccSet.put(newBS, acc.getValue());
				}
				mdp.acceptanceSet = newAccSet;
			}
		}
	}

	/**
	 * This class stores an acceptance set (as BitSet) and some info regarding the condition, i.e. lim inf/sup (in the long run)
	 * of visiting these states has to be >/</<=/>= a bound (usually the bound is between 0 and 1. 
	 */
	public static class MDPCondition
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

	public BitSet transformToStateSet(BitSet set)
	{
		BitSet stateSet = new BitSet();
		set.stream().forEach(index -> stateSet.set(computeStartStateOfEdge(index)));
		return stateSet;
	}
}
