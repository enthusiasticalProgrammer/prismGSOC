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
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;

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
public class AcceptanceGenRabinTransition implements AcceptanceOmegaTransition
{
	/**
	 * These two attributes are important for mapping the edges to numbers, such that we can store them in BitSets.
	 * The number for a BitSet of an edge from x with BitSet bs is computed as follows: x*(2^{amountOfAPs})+sum_{bs}(2^i * bs.get(i)) 
	 */
	private int amountOfStates;
	private final int amountOfAPs;

	public @NonNull final List<@NonNull AcceptanceGenRabinTransition.GenRabinPair> accList;

	public AcceptanceGenRabinTransition(DA<BitSet, ?> da)
	{
		amountOfStates = da.size();
		amountOfAPs = da.getAPList().size();
		accList = new ArrayList<>();
	}

	public AcceptanceGenRabinTransition(int amountOfStates, int amountOfAPs)
	{
		this.amountOfStates = amountOfStates;
		this.amountOfAPs = amountOfAPs;
		accList = new ArrayList<>();
	}

	/**
	 * A pair in a Generalized Rabin acceptance condition, i.e., with
	 *  (F G !"L") & (G F "K_1") & ... & (G F "K_n").
	 **/
	public class GenRabinPair
	{
		/** Edge set Finite (should be visited only finitely often) 
		 * The offset of the list is equal to the number of the source state 
		 */
		@NonNull
		BitSet Finite;

		/** Edge sets Infinite (should all be visited infinitely often) */
		final @NonNull List<@NonNull BitSet> Infinite;

		/** Constructor with L and K_j state sets */
		public GenRabinPair(@NonNull BitSet Finite, @NonNull List<@NonNull BitSet> Infinite)
		{
			this.Finite = Finite;
			this.Infinite = Infinite;
		}

		/** Returns true if the bottom strongly connected component
		 * given by bsccEdges is accepting for this pair.
		 */
		public boolean isBSCCAccepting(BitSet bsccEdges)
		{
			if (bsccEdges.intersects(Finite)) {
				return false;
			}

			for (BitSet inf : Infinite) {
				if (!bsccEdges.intersects(inf)) {
					return false;
				}
			}
			return true;
		}

		@Override
		public GenRabinPair clone()
		{
			@NonNull
			List<@NonNull BitSet> newInfList = new ArrayList<>();
			for (BitSet inf : Infinite) {
				BitSet newInf = (BitSet) inf.clone();
				if (newInf != null) {
					newInfList.add(newInf);
				} else {
					throw new NullPointerException("Cloning a BitSet returned null");
				}
			}
			BitSet newFinite = (BitSet) Finite.clone();
			if (newFinite != null)
				return new GenRabinPair(newFinite, newInfList);
			throw new NullPointerException("Cloning a BitSet returned null");
		}

		/** Returns a textual representation of this Generalized Rabin pair. */
		@Override
		public String toString()
		{
			String s = "(" + Finite;
			for (BitSet inf : Infinite)
				s += "," + inf;
			s += ")";
			return s;
		}

		private void lift(Map<Integer, Collection<Integer>> lifter)
		{
			Finite = transformSingleBitSet(Finite, lifter);
			Infinite.replaceAll(inf -> transformSingleBitSet(inf, lifter));
		}

		private @NonNull BitSet transformSingleBitSet(BitSet oldBitSet, Map<Integer, Collection<Integer>> lifter)
		{
			BitSet newBs = new BitSet(amountOfStates * (1 << amountOfAPs));
			IntStream.range(0, oldBitSet.size()).filter(i -> oldBitSet.get(i)).mapToObj(i -> lifter.get(i)).reduce(new HashSet<>(), (a, b) -> {
				a.addAll(b);
				return a;
			}).forEach(i -> newBs.set(i));
			return newBs;
		}
	}

	/** Make a copy of the acceptance condition. */
	@Override
	public AcceptanceGenRabinTransition clone()
	{
		AcceptanceGenRabinTransition result = new AcceptanceGenRabinTransition(amountOfStates, amountOfAPs);
		for (GenRabinPair pair : this.accList) {
			result.accList.add(pair.clone());
		}

		return result;
	}

	/** Returns true if the bottom strongly connected component
	 * given by bscc_edges is accepting for this Rabin condition,
	 * i.e., there is a pair that accepts for bsccEdges.
	 * Nota bene: the BitSet corresponds here to edges and not states
	 */
	@Override
	public boolean isBSCCAccepting(BitSet bsccEdges)
	{
		return this.accList.stream().anyMatch(pair -> pair.isBSCCAccepting(bsccEdges));
	}

	/**
	 * Returns a new Generalized Rabin acceptance condition that corresponds to the disjunction
	 * of this and the other Generalized Rabin acceptance condition. The GenRabinPairs are cloned, i.e.,
	 * not shared with the argument acceptance condition.
	 * @param other the other GeneralizedRabin acceptance condition
	 * @return new AcceptanceGenRabin, disjunction of this and other
	 */
	public AcceptanceGenRabinTransition or(AcceptanceGenRabinTransition other)
	{
		if (other.amountOfAPs != this.amountOfAPs || other.amountOfStates != this.amountOfStates) {
			throw new IllegalArgumentException("It was tried to connect two acceptance condition, which correspond to different automata.");
		}
		AcceptanceGenRabinTransition result = new AcceptanceGenRabinTransition(amountOfStates, amountOfAPs);
		for (GenRabinPair pair : this.accList) {
			result.accList.add(pair.clone());
		}
		for (GenRabinPair pair : other.accList) {
			result.accList.add(pair.clone());
		}
		return result;
	}

	@Override
	public String getSignatureForEdgeHOA(int startState, BitSet label)
	{
		String result = "";
		int edgeOffset = computeOffsetForEdge(startState, label);

		int set_index = 0;
		for (GenRabinPair pair : this.accList) {
			if (pair.Finite.get(edgeOffset)) {
				result += (result.isEmpty() ? "" : " ") + set_index;
			}

			set_index++;
			for (BitSet inf : pair.Infinite) {
				if (inf.get(edgeOffset)) {
					result += (result.isEmpty() ? "" : " ") + set_index;
				}
				set_index++;
			}
		}

		if (!result.isEmpty())
			result = "{" + result + "}";

		return result;
	}

	public int computeOffsetForEdge(int startState, BitSet label)
	{
		return startState * (1 << amountOfAPs) + computeOffsetForEdgeLabel(label);
	}

	private int computeOffsetForEdgeLabel(BitSet label)
	{
		if (label.size() > this.amountOfAPs) {
			throw new IllegalArgumentException("The BitSet cannot correspond to the same automaton as the acceptance set. It is too large.");
		}
		return IntStream.range(0, label.size()).map(i -> label.get(i) ? 1 << i : 0).sum();
	}

	/** Returns a textual representation of this acceptance condition. */
	@Override
	public String toString()
	{
		String result = "";
		for (GenRabinPair pair : accList) {
			result += pair.toString();
		}
		return result;
	}

	@Override
	public String getSizeStatistics()
	{
		return accList.size() + " Generalized Rabin pairs";
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
		out.print("acc-name: generalized-Rabin " + accList.size());
		for (GenRabinPair pair : accList) {
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
		for (GenRabinPair pair : accList) {
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
		amountOfStates = lifter.values().stream().reduce(new HashSet<>(), (a, b) -> {
			a.addAll(b);
			return a;
		}).stream().reduce(0, Integer::max) + 1;
		for (GenRabinPair pair : accList) {
			pair.lift(lifter);
		}
	}

	@Override
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars)
	{
		return new AcceptanceGenRabinTransitionDD(this, ddRowVars);
	}
}
