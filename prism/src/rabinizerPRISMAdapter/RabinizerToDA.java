package rabinizerPRISMAdapter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import acceptance.AcceptanceControllerSynthesis;
import acceptance.AcceptanceGenRabinTransition;
import automata.DA;
import ltl.parser.Comparison;
import ltl.FrequencyG;
import omega_automaton.Edge;
import omega_automaton.acceptance.GeneralisedRabinAcceptance;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;
import rabinizer.DTGRMAAcceptance.BoundAndReward;
import rabinizer.DTGRMAAcceptance.GeneralisedRabinWithMeanPayoffAcceptance;
import rabinizer.automata.AbstractSelfProductSlave;
import rabinizer.automata.FrequencySelfProductSlave;
import rabinizer.automata.Product;
import rabinizer.automata.ProductControllerSynthesis;
import rabinizer.automata.ProductRabinizer;
import rabinizer.automata.RabinSlave.State;

public class RabinizerToDA
{
	static <T extends AbstractSelfProductSlave<T>.State> DA<BitSet, ? extends AcceptanceGenRabinTransition> getGenericDA(Product<T> automaton,
			BiMap<String, Integer> aliases)
	{
		DA<BitSet, AcceptanceGenRabinTransition> result = new DA<>(automaton.getStates().size());

		BiMap<Product<T>.ProductState, Integer> stateIntMap = StateIntegerBiMap(automaton);

		List<String> APList = getAPList(aliases);
		result.setAPList(APList);

		result.setStartState(stateIntMap.get(automaton.getInitialState()));

		for (Product<T>.ProductState state : stateIntMap.keySet()) {
			Map<Edge<Product<T>.ProductState>, ValuationSet> successors = automaton.getSuccessors(state);
			for (Entry<Edge<Product<T>.ProductState>, ValuationSet> entry : successors.entrySet()) {
				Iterator<BitSet> iter = entry.getValue().iterator();
				while (iter.hasNext()) {
					BitSet bs = iter.next();
					result.addEdge(stateIntMap.get(state), bs, stateIntMap.get(entry.getKey().successor));
				}
			}
		}
		result.setAcceptance(transformRabinizerToPrismAcceptance(automaton, stateIntMap, result));
		return result;
	}

	private static <T extends AbstractSelfProductSlave<T>.State> AcceptanceGenRabinTransition transformRabinizerToPrismAcceptance(Product<T> automaton,
			BiMap<Product<T>.ProductState, Integer> stateIntMap, DA<BitSet, ? extends AcceptanceGenRabinTransition> da)
	{
		GeneralisedRabinAcceptance<Product<T>.ProductState> acceptance = automaton.getAcceptance();
		acceptance.AcceptanceGenRabinTransition result;
		if (automaton instanceof ProductControllerSynthesis) {
			result = new AcceptanceControllerSynthesis(da);
		} else {
			result = new AcceptanceGenRabinTransition(da);
		}

		for (Tuple<TranSet<Product<T>.ProductState>, List<TranSet<Product<T>.ProductState>>> genRabinPair : acceptance.acceptanceCondition) {
			BitSet Finite = transformSingleAcceptingSetFromRabinizerToPrism(genRabinPair.left, stateIntMap, result);
			List<BitSet> Infinite = genRabinPair.right.stream().map(set -> transformSingleAcceptingSetFromRabinizerToPrism(set, stateIntMap, result))
					.collect(Collectors.toList());
			if (result instanceof AcceptanceControllerSynthesis) {
				List<AcceptanceControllerSynthesis.MDPCondition> mdp = computeMDPConditions(acceptance, stateIntMap, result,
						acceptance.acceptanceCondition.indexOf(genRabinPair));
				result.accList.add(((AcceptanceControllerSynthesis) result).new AccControllerPair(Finite, Infinite, mdp));
			} else {
				result.accList.add(result.new GenRabinPair(Finite, Infinite));
			}
		}
		return result;
	}

	private static List<AcceptanceControllerSynthesis.MDPCondition> computeMDPConditions(GeneralisedRabinAcceptance<?> acceptance,
			BiMap<?, Integer> stateIntMap, AcceptanceGenRabinTransition result, int index)
	{
			GeneralisedRabinWithMeanPayoffAcceptance accMDP = (GeneralisedRabinWithMeanPayoffAcceptance) acceptance;
		return transformMDPAccSetToPrism(accMDP.acceptanceMDP.get(index), stateIntMap, result);
	}

	private static <T> List<AcceptanceControllerSynthesis.MDPCondition> transformMDPAccSetToPrism(Collection<BoundAndReward> set, BiMap<T, Integer> stateIntMap,
			AcceptanceGenRabinTransition acceptance)
	{
		List<AcceptanceControllerSynthesis.MDPCondition> result = new ArrayList<>(set.size());
		for (BoundAndReward boundAndReward : set) {
			double bound = boundAndReward.GOp.bound;
			Comparison cmpOperator = (boundAndReward.GOp.cmp == FrequencyG.Comparison.GEQ ? Comparison.GEQ : Comparison.GT);
			boolean isLimInf = boundAndReward.GOp.limes == FrequencyG.Limes.INF;
			BiMap<BitSet, Integer> rewards = HashBiMap.create();
			for (Entry<Integer, TranSet<Product<rabinizer.automata.FrequencySelfProductSlave.State>.ProductState>> entry : boundAndReward.relevantEntries()) {
				rewards.put(transformSingleAcceptingSetFromRabinizerToPrism(entry.getValue(), stateIntMap, acceptance), entry.getKey());
			}
			result.add(new MDPCondition(rewards, bound, cmpOperator, isLimInf));
		}
		return result;
	}

	private static BitSet transformSingleAcceptingSetFromRabinizerToPrism(TranSet<?> accSet, BiMap<?, Integer> stateIntMap, AcceptanceGenRabinTransition acc)
	{
		BitSet result = new BitSet();
		for (int state = 0; state < stateIntMap.entrySet().size(); state++) {
			ValuationSet valu = accSet.asMap().get(stateIntMap.inverse().get(state));
			if (valu != null) {
				Iterator<BitSet> iter = valu.iterator();
				while (iter.hasNext()) {
					BitSet bs = iter.next();
					if (bs != null) {
						result.set(acc.computeOffsetForEdge(state, bs));
					}
				}
			}
		}
		return result;
	}

	private static List<String> getAPList(BiMap<String, Integer> aliases)
	{
		List<String> result = new ArrayList<>();

		for (int i = 0; i < aliases.size(); i++) {
			result.add(aliases.inverse().get(i));
		}
		return result;
	}

	private static <T extends AbstractSelfProductSlave<T>.State> BiMap<Product<T>.ProductState, Integer> StateIntegerBiMap(Product<T> automaton)
	{
		BiMap<Product<T>.ProductState, Integer> stateInt = HashBiMap.create();
		int current = 0;
		for (Product<T>.ProductState state : automaton.getStates()) {
			stateInt.put(state, current++);
		}
		return stateInt;
	}
}
