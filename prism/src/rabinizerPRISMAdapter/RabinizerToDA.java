package rabinizerPRISMAdapter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import acceptance.AcceptanceGenRabinTransition;
import acceptance.AcceptanceOmegaTransition;
import automata.DA;
import omega_automaton.Edge;
import omega_automaton.collections.TranSet;
import omega_automaton.collections.Tuple;
import omega_automaton.collections.valuationset.ValuationSet;
import rabinizer.automata.ProductRabinizer;

public class RabinizerToDA
{
	static DA<BitSet, AcceptanceGenRabinTransition<BitSet>> getDAFromRabinizer(ProductRabinizer automaton, BiMap<String, Integer> aliases)
	{
		DA<BitSet, AcceptanceGenRabinTransition<BitSet>> result = new DA<>(automaton.getStates().size());

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

	private static @NonNull AcceptanceGenRabinTransition<BitSet> transformRabinizerToPrismAcceptance(ProductRabinizer automaton,
			BiMap<ProductRabinizer.ProductState, Integer> stateIntMap, DA<BitSet, AcceptanceGenRabinTransition<BitSet>> da)
	{
		List<Tuple<TranSet<ProductRabinizer.ProductState>, List<TranSet<ProductRabinizer.ProductState>>>> acceptance = automaton
				.getAcceptance().acceptanceCondition;

		AcceptanceGenRabinTransition<BitSet> result = new AcceptanceGenRabinTransition<>();

		for (Tuple<TranSet<ProductRabinizer.ProductState>, List<TranSet<ProductRabinizer.ProductState>>> genRabinPair : acceptance) {
			List<Set<DA<BitSet, ? extends AcceptanceOmegaTransition<BitSet>>.Edge>> Finite = transformSingleAcceptingSetFromRabinizerToPrism(genRabinPair.left,
					stateIntMap, da);
			List<List<Set<DA<BitSet, ? extends AcceptanceOmegaTransition<BitSet>>.Edge>>> Infinite = genRabinPair.right.stream()
					.map(set -> transformSingleAcceptingSetFromRabinizerToPrism(set, stateIntMap, da)).collect(Collectors.toList());
			result.add(result.new GenRabinPair(Finite, Infinite));
		}
		return result;
	}

	private static @NonNull List<Set<DA<BitSet, ? extends AcceptanceOmegaTransition<BitSet>>.Edge>> transformSingleAcceptingSetFromRabinizerToPrism(
			TranSet<ProductRabinizer.ProductState> accSet, BiMap<ProductRabinizer.ProductState, Integer> stateIntMap,
			DA<BitSet, AcceptanceGenRabinTransition<BitSet>> automaton)
	{
		List<Set<DA<BitSet, ? extends AcceptanceOmegaTransition<BitSet>>.Edge>> result = new ArrayList<>();
		for (int i = 0; i < stateIntMap.entrySet().size(); i++) {
			Set<DA<BitSet, ? extends AcceptanceOmegaTransition<BitSet>>.Edge> edgesFromCurrentState = new HashSet<>();
			ProductRabinizer.ProductState state = stateIntMap.inverse().get(i);
			ValuationSet valu = accSet.asMap().get(state);
			if (valu != null) {
				Iterator<BitSet> iter = valu.iterator();
				while (iter.hasNext()) {
					BitSet bs = iter.next();
					if (bs != null)
						try {
							edgesFromCurrentState.add(automaton.new Edge(bs, stateIntMap.get(state.getSuccessor(bs))));
						} catch (NullPointerException e) {
							//nothing to do. This occurs sometimes in Rabinizer, when an edge is suppressed.
						}
				}
			}
			result.add(edgesFromCurrentState);
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
