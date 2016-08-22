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
import acceptance.AcceptanceControllerSynthesis.MDPCondition;
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
import rabinizer.automata.Product;
import rabinizer.automata.ProductControllerSynthesis;

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

		for (Entry<Product<T>.ProductState, Integer> stateIntEntry : stateIntMap.entrySet()) {
			Map<Edge<Product<T>.ProductState>, ValuationSet> successors = automaton.getSuccessors(stateIntEntry.getKey());
			for (Entry<Edge<Product<T>.ProductState>, ValuationSet> entry : successors.entrySet()) {
				Iterator<BitSet> iter = entry.getValue().iterator();
				while (iter.hasNext()) {
					BitSet bs = iter.next();
					result.addEdge(stateIntEntry.getValue(), bs, stateIntMap.get(entry.getKey().successor));
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

		for (int i = 0; i < acceptance.unmodifiableCopyOfAcceptanceCondition().size(); i++) {
			Tuple<TranSet<Product<T>.ProductState>, List<TranSet<Product<T>.ProductState>>> genRabinPair = acceptance.unmodifiableCopyOfAcceptanceCondition()
					.get(i);
			BitSet Finite = transformSingleAcceptingSetFromRabinizerToPrism(genRabinPair.left, stateIntMap, result);
			List<BitSet> Infinite = genRabinPair.right.stream().map(set -> transformSingleAcceptingSetFromRabinizerToPrism(set, stateIntMap, result))
					.collect(Collectors.toList());
			if (result instanceof AcceptanceControllerSynthesis) {
				List<AcceptanceControllerSynthesis.MDPCondition> mdp = computeMDPConditions(acceptance, stateIntMap, result, i);
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
		return transformMDPAccSetToPrism(accMDP.getUnmodifiableAcceptanceMDP().get(index), stateIntMap, result);
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
