//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package acceptance;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import automata.DA;
import jdd.JDDVars;

/**
 * A Generalized Rabin acceptance condition (based on BitSet state sets)
 * This is a list of GenRabinPairs, which can be manipulated with the usual List interface.
 * <br>
 * Semantics: Each Generalized Rabin pair has state sets L and K_1,...,K_n and is accepting iff
 * L is not visited infinitely often and all K_j are visited infinitely often:
 *   (F G !"L") & (G F "K_1") & ... & (G F "K_n").
 *
 * The Generalized Rabin condition is accepting if at least one of the pairs is accepting.
 */
public class AcceptanceGenRabinTransition<Symbol> extends ArrayList<AcceptanceGenRabinTransition<Symbol>.GenRabinPair>
		implements AcceptanceOmegaTransition<Symbol>
{

	/**
	 * A pair in a Generalized Rabin acceptance condition, i.e., with
	 *  (F G !"L") & (G F "K_1") & ... & (G F "K_n").
	 **/
	public class GenRabinPair
	{
		/** Edge set Finite (should be visited only finitely often) 
		 * The offset of the list is equal to the number of the source state 
		 */
		private final List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> Finite;

		/** Edge sets Infinite (should all be visited infinitely often) */
		private final List<List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>>> Infinite;

		/** Constructor with L and K_j state sets */
		public GenRabinPair(List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> Finite,
				List<List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>>> Infinite)
		{
			this.Finite = Finite;
			this.Infinite = Infinite;
		}

		//TODO this can give sometimes false negatives (e.g. if a Fin-set is present, but the inf-sets can still be used,
		// without the Fin-edges. I am not sure whether this is desired or not, therefore I leave it.
		/** Returns true if the bottom strongly connected component
		 * given by bscc_states is accepting for this pair.
		 */
		public boolean isBSCCAccepting(BitSet bscc_states)
		{

			for (Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge> edgesFromState : Finite) {
				for (DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge edge : edgesFromState) {
					if (bscc_states.get(edge.dest)) {
						// there is some state in bscc_states that is
						// forbidden by L
						return false;
					}
				}
			}

			for (List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> inf : Infinite) {
				for (Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge> edgesFromState : inf) {
					if (edgesFromState.stream().allMatch(edge -> !bscc_states.get(edge.dest))) {
						//there is no edge for an infinite set in the bscc 
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public GenRabinPair clone()
		{
			List<List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>>> newInfList = new ArrayList<>();
			for (List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> inf : Infinite) {
				newInfList.add(new ArrayList<>(inf));

				for (int i = 0; i < inf.size(); i++) {
					newInfList.get(newInfList.size() - 1).add(new HashSet<>(inf.get(i)));
				}
			}
			return new GenRabinPair(new ArrayList<>(Finite), newInfList);
		}

		/** Returns a textual representation of this Generalized Rabin pair. */
		@Override
		public String toString()
		{
			String s = "(" + Finite;
			for (List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> inf : Infinite)
				s += "," + inf;
			s += ")";
			return s;
		}

		private void lift(Map<Integer, Collection<Integer>> lifter)
		{
			//first, adjust the target edges
			Finite.replaceAll(set -> mapEdgeSetFromSingleState(lifter, set));

			for (List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> inf : Infinite) {
				inf.replaceAll(set -> mapEdgeSetFromSingleState(lifter, set));
			}

			//second, adjust the source edges (being represented by the indices of the lists)
			int maxOffset = lifter.values().stream().map(set -> set.stream().reduce(0, (a, b) -> a > b ? a : b)).reduce(0, (a, b) -> a > b ? a : b);
			transformSingleList(Finite, lifter, maxOffset);

			for (List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> inf : Infinite) {
				transformSingleList(inf, lifter, maxOffset);
			}
		}

		private void transformSingleList(List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> oldList,
				Map<Integer, Collection<Integer>> lifter, int maxOffset)
		{
			Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>[] newAsArray = new Set[maxOffset + 1];
			for (int i = 0; i < oldList.size(); i++) {
				Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge> toCopy = oldList.get(i);
				lifter.get(i).stream().forEach(j -> newAsArray[j] = new HashSet<>(toCopy));
			}
			oldList.clear();
			for (int i = 0; i < newAsArray.length; i++) {
				if (newAsArray[i] == null) {
					oldList.add(new HashSet<>());
				} else {
					oldList.add(newAsArray[i]);
				}
			}
		}
	}

	/** Make a copy of the acceptance condition. */
	@Override
	public AcceptanceGenRabinTransition<Symbol> clone()
	{
		AcceptanceGenRabinTransition<Symbol> result = new AcceptanceGenRabinTransition<>();
		for (GenRabinPair pair : this) {
			result.add(pair.clone());
		}

		return result;
	}

	/** Returns true if the bottom strongly connected component
	 * given by bscc_states is accepting for this Rabin condition,
	 * i.e., there is a pair that accepts for bscc_states.
	 */
	@Override
	public boolean isBSCCAccepting(BitSet bscc_states)
	{
		return this.stream().anyMatch(pair -> pair.isBSCCAccepting(bscc_states));
	}

	/**
	 * Returns a new Generalized Rabin acceptance condition that corresponds to the disjunction
	 * of this and the other Generalized Rabin acceptance condition. The GenRabinPairs are cloned, i.e.,
	 * not shared with the argument acceptance condition.
	 * @param other the other GeneralizedRabin acceptance condition
	 * @return new AcceptanceGenRabin, disjunction of this and other
	 */
	public AcceptanceGenRabinTransition<Symbol> or(AcceptanceGenRabinTransition<Symbol> other)
	{
		AcceptanceGenRabinTransition<Symbol> result = new AcceptanceGenRabinTransition<>();
		for (GenRabinPair pair : this) {
			result.add(pair.clone());
		}
		for (GenRabinPair pair : other) {
			result.add(pair.clone());
		}
		return result;
	}

	@Override
	public String getSignatureForEdgeHOA(int startState, Symbol sym)
	{
		String result = "";

		int set_index = 0;
		for (GenRabinPair pair : this) {
			Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge> finEdgeFromState = pair.Finite.get(startState);
			if (finEdgeFromState.stream().anyMatch(edge -> edge.label.equals(sym))) {
				result += (result.isEmpty() ? "" : " ") + set_index;
			}

			set_index++;
			for (List<Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge>> inf : pair.Infinite) {
				Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge> infEdgeFromState = inf.get(startState);
				if (infEdgeFromState.stream().anyMatch(edge -> edge.label.equals(sym))) {
					result += (result.isEmpty() ? "" : " ") + set_index;
				}
				set_index++;
			}
		}

		if (!result.isEmpty())
			result = "{" + result + "}";

		return result;
	}

	/** Returns a textual representation of this acceptance condition. */
	@Override
	public String toString()
	{
		String result = "";
		for (GenRabinPair pair : this) {
			result += pair.toString();
		}
		return result;
	}

	@Override
	public String getSizeStatistics()
	{
		return size() + " Generalized Rabin pairs";
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.GENERALIZED_RABIN;
	}

	@Override
	public void outputHOAHeader(PrintStream out)
	{
		int sets = 0;
		out.print("acc-name: generalized-Rabin " + size());
		for (GenRabinPair pair : this) {
			sets++; // the Fin
			out.print(" " + pair.Infinite.size());
			sets += pair.Infinite.size();
		}
		out.println();
		out.print("Acceptance: " + sets);
		if (sets == 0) {
			out.println("f");
			return;
		}

		int set_index = 0;
		for (GenRabinPair pair : this) {
			if (set_index > 0)
				out.print(" | ");
			out.print("( Fin(" + set_index + ")");
			set_index++;
			for (int i = 0; i < pair.Infinite.size(); i++) {
				out.print(" & Inf(" + set_index + ")");
				set_index++;
			}
			out.print(")");
		}
		out.println();
	}

	@Override
	public void lift(Map<Integer, Collection<Integer>> lifter)
	{
		for (AcceptanceGenRabinTransition<Symbol>.GenRabinPair pair : this) {
			pair.lift(lifter);
		}
	}

	private Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge> mapEdgeSetFromSingleState(Map<Integer, Collection<Integer>> lifter,
			Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge> edgeSet)
	{
		Set<DA<Symbol, ? extends AcceptanceOmegaTransition<Symbol>>.Edge> newEdgeSet = new HashSet<>();
		edgeSet.stream().forEach(edge -> lifter.get(edge.dest).stream()
				.forEach(integer -> newEdgeSet.add(new DA<Symbol, AcceptanceGenRabinTransition<Symbol>>(0).new Edge(edge.label, edge.dest))));
		return newEdgeSet;
	}

	@Override
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars)
	{
		return new AcceptanceGenRabinTransitionDD(this, ddRowVars);
	}
}
