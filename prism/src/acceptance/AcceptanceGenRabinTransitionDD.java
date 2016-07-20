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

public class AcceptanceGenRabinTransitionDD implements AcceptanceOmegaDD
{
	private final JDDVars daRowVars;
	private final JDDVars daColVars;
	private final JDDVars allDDRowVars;
	private final JDDVars allDDColVars;
	private final DA<BitSet, ?> da;
	private final AcceptanceGenRabinTransition accRabinTransition;
	public final List<GenRabinPairTransitionDD> accList;
	private final ProbModel product;

	public AcceptanceGenRabinTransitionDD(AcceptanceGenRabinTransition acceptanceGenRabinTransition, JDDVars daRowVars, JDDVars daColVars, JDDVars allDDRowVars,
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

	public class GenRabinPairTransitionDD
	{
		public final JDDNode finTransitions;
		public final List<JDDNode> infTransitions;

		private GenRabinPairTransitionDD(JDDNode finTransitions, List<JDDNode> infTransitions)
		{
			this.finTransitions = finTransitions;
			this.infTransitions = infTransitions;
		}

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
