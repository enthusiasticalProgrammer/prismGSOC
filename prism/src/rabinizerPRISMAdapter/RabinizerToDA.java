package rabinizerPRISMAdapter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import acceptance.AcceptanceGenRabinTransition;
import automata.DA;
import omega_automaton.Edge;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;
import rabinizer.automata.ProductRabinizer;

public class RabinizerToDA
{
	static DA<BitSet, AcceptanceGenRabinTransition> getDAFromRabinizer(ProductRabinizer automaton, BiMap<String, Integer> aliases)
	{
		DA<BitSet, AcceptanceGenRabinTransition> result = new DA<>(automaton.getStates().size());

		BiMap<ProductRabinizer.ProductState, Integer> stateIntMap = StateIntegerBiMap(automaton);

		List<String> APList = getAPList(aliases);
		result.setAPList(APList);

		result.setStartState(stateIntMap.get(automaton.getInitialState()));

		for (ProductRabinizer.ProductState state : stateIntMap.keySet()) {
			Map<Edge<ProductRabinizer.ProductState>, ValuationSet> successors = automaton.getSuccessors(state);
			for (Entry<Edge<ProductRabinizer.ProductState>, ValuationSet> entry : successors.entrySet()) {
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

	private static AcceptanceGenRabinTransition transformRabinizerToPrismAcceptance(ProductRabinizer automaton,
			BiMap<ProductRabinizer.ProductState, Integer> stateIntMap, DA<BitSet, AcceptanceGenRabinTransition> da)
	{
		List<Tuple<TranSet<ProductRabinizer.ProductState>, List<TranSet<ProductRabinizer.ProductState>>>> acceptance = automaton
				.getAcceptance().acceptanceCondition;

		AcceptanceGenRabinTransition result = new AcceptanceGenRabinTransition(da);

		for (Tuple<TranSet<ProductRabinizer.ProductState>, List<TranSet<ProductRabinizer.ProductState>>> genRabinPair : acceptance) {
			BitSet Finite = transformSingleAcceptingSetFromRabinizerToPrism(genRabinPair.left, stateIntMap, result);
			List<BitSet> Infinite = genRabinPair.right.stream().map(set -> transformSingleAcceptingSetFromRabinizerToPrism(set, stateIntMap, result))
					.collect(Collectors.toList());
			if (Infinite != null) {
				result.accList.add(result.new GenRabinPair(Finite, Infinite));
			} else {
				throw new NullPointerException("A lambda expression returned null.");
			}
		}
		return result;
	}

	private static BitSet transformSingleAcceptingSetFromRabinizerToPrism(TranSet<ProductRabinizer.ProductState> accSet,
			BiMap<ProductRabinizer.ProductState, Integer> stateIntMap, AcceptanceGenRabinTransition acc)
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

	private static BiMap<ProductRabinizer.ProductState, Integer> StateIntegerBiMap(ProductRabinizer automaton)
	{
		BiMap<ProductRabinizer.ProductState, Integer> stateInt = HashBiMap.create();
		int current = 0;
		for (ProductRabinizer.ProductState state : automaton.getStates()) {
			stateInt.put(state, current++);
		}
		return stateInt;
	}
}
