//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Hongyang Qu <hongyang.qu@cs.ox.ac.uk> (University of Oxford)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* David Mueller <david.mueller@tcs.inf.tu-dresden.de> (TU Dresden)
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

package automata;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import acceptance.AcceptanceGenRabinTransition;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceOmegaTransition;
import acceptance.AcceptanceRabin;
import jltl2ba.APElement;
import jltl2ba.APElementIterator;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;
import prism.PrismPrintStreamLog;

/**
 * Class to store a deterministic automata of some acceptance type Acceptance.
 * States are 0-indexed integers; class is parameterised by edge labels (Symbol).
 */
public class DA<Symbol, Acceptance extends AcceptanceOmega>
{
	/** AP list */
	private List<String> apList;
	/** Size, i.e. number of states */
	private int size;
	/** Start state (index) */
	private int start;
	/** Edges of DA */
	private List<List<Edge>> edges;
	/** The acceptance condition (as BitSets) */
	private Acceptance acceptance;

	/** Public class to represent DA edge */
	public class Edge
	{
		public Symbol label;
		public int dest;

		public Edge(Symbol label, int dest)
		{
			this.label = label;
			this.dest = dest;
		}

		@Override
		public boolean equals(Object other)
		{
			if (other == null) {
				return false;
			} else if (!(other instanceof DA.Edge)) {
				return false;
			} else {
				Edge that = (DA<Symbol, Acceptance>.Edge) other;
				return this.label.equals(that.label) && this.dest == that.dest;
			}
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(this.label, this.dest);
		}
	}

	/**
	 * Construct a DA of fixed size (i.e. fixed number of states).
	 */
	public DA(int size)
	{
		apList = null;
		this.size = size;
		this.start = -1;
		edges = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			edges.add(new ArrayList<Edge>());
		}
	}

	public void setAcceptance(Acceptance acceptance)
	{
		this.acceptance = acceptance;
	}

	public Acceptance getAcceptance()
	{
		return acceptance;
	}

	// TODO: finish/tidy this
	public void setAPList(List<String> apList)
	{
		this.apList = apList;
	}

	public List<String> getAPList()
	{
		return apList;
	}

	// Mutators

	/**
	 * Set the start state (index)
	 */
	public void setStartState(int start)
	{
		this.start = start;
	}

	/**
	 * Returns true if the automaton has an edge for {@code src} and {@label}.
	 */
	public boolean hasEdge(int src, Symbol label)
	{
		for (Edge edge : edges.get(src)) {
			if (edge.label.equals(label)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Add an edge
	 * @param src the source state
	 * @param label the label of the edge
	 * @param dest the state-number of the destination
	 */
	public void addEdge(int src, Symbol label, int dest)
	{
		edges.get(src).add(new Edge(label, dest));
	}

	// Accessors

	/**
	 * @return the size (number of states).
	 */
	public int size()
	{
		return size;
	}

	/**
	 * @return the start state (index)
	 */
	public int getStartState()
	{
		return start;
	}

	/**
	 * @param i state-number of the source state
	 * @return the number of edges from state i
	 */
	public int getNumEdges(int i)
	{
		return edges.get(i).size();
	}

	/**
	 * Get the destination of edge j from state i
	 */
	public int getEdgeDest(int i, int j)
	{
		return edges.get(i).get(j).dest;
	}

	/**
	 * Get the label of edge j from state i.
	 */
	public Symbol getEdgeLabel(int i, int j)
	{
		return edges.get(i).get(j).label;
	}

	/**
	 * Get the destination of the edge from state i with label lab.
	 * Returns -1 if no such edge is found.
	 */
	public int getEdgeDestByLabel(int i, Symbol lab)
	{
		for (Edge e : edges.get(i))
			if (e.label.equals(lab))
				return e.dest;
		return -1;
	}

	/**
	 * Print the automaton in Dot format to an output stream.
	 */
	public void printDot(PrintStream out)
	{
		printDot(new PrismPrintStreamLog(out));
	}

	/**
	 * Prints the automaton in Dot format to a PrismLog
	 * @param out the prismLog where it ought to be printed into
	 */
	public void printDot(PrismLog out)
	{
		int i;
		out.println("digraph model {");
		for (i = 0; i < size; i++) {
			out.print("	" + i + " [label=\"" + i + " [");
			out.print(acceptance.getSignatureForState(i));
			out.print("]\", shape=");
			if (i == start)
				out.println("doublecircle]");
			else
				out.println("ellipse]");
		}
		for (i = 0; i < size; i++) {
			for (Edge e : edges.get(i)) {
				out.println("	" + i + " -> " + e.dest + " [label=\"" + e.label + "\"]");
			}
		}
		out.println("}");
	}

	/**
	 * Print the DA in ltl2dstar v2 format to the output stream.
	 * @param out the output stream 
	 */
	public static void printLtl2dstar(DA<BitSet, AcceptanceRabin> dra, PrintStream out) throws PrismException
	{
		AcceptanceRabin acceptance = dra.getAcceptance();

		if (dra.getStartState() < 0) {
			// No start state! 
			throw new PrismException("No start state in DA!");
		}

		out.println("DRA v2 explicit");
		out.println("States: " + dra.size());
		out.println("Acceptance-Pairs: " + acceptance.size());
		out.println("Start: " + dra.getStartState());

		// Enumerate APSet
		out.print("AP: " + dra.getAPList().size());
		for (String ap : dra.getAPList()) {
			out.print(" \"" + ap + "\"");
		}
		out.println();

		out.println("---");

		for (int i_state = 0; i_state < dra.size(); i_state++) {
			out.println("State: " + i_state);

			out.print("Acc-Sig:");
			for (int pair = 0; pair < acceptance.size(); pair++) {
				if (acceptance.get(pair).getL().get(i_state)) {
					out.print(" -" + pair);
				} else if (acceptance.get(pair).getK().get(i_state)) {
					out.print(" +" + pair);
				}
			}
			out.println();

			APElementIterator it = new APElementIterator(dra.apList.size());
			while (it.hasNext()) {
				APElement edge = it.next();
				out.println(dra.getEdgeDestByLabel(i_state, edge));
			}
		}
	}

	/**
	 * Print the DA in HOA format to the output stream.
	 * @param out the output stream
	 * @throws PrismNotSupportedException if the Symbol-parameter is not a BitSet
	 */
	public void printHOA(PrintStream out) throws PrismNotSupportedException
	{
		out.println("HOA: v1");
		out.println("States: " + size());

		// AP
		out.print("AP: " + apList.size());
		for (String ap : apList) {
			// TODO(JK): Proper quoting
			out.print(" \"" + ap + "\"");
		}
		out.println();

		out.println("Start: " + start);
		acceptance.outputHOAHeader(out);
		out.println("properties: trans-labels explicit-labels state-acc no-univ-branch deterministic");
		out.println("--BODY--");
		for (int i = 0; i < size(); i++) {
			out.print("State: " + i + " "); // id
			out.println(acceptance.getSignatureForStateHOA(i));

			for (Edge edge : edges.get(i)) {
				Symbol label = edge.label;
				if (!(label instanceof BitSet))
					throw new PrismNotSupportedException("Can not print automaton with " + label.getClass() + " labels");
				String labelString = "[" + APElement.toStringHOA((BitSet) label, apList.size()) + "]";
				out.print(labelString);
				out.print(" ");
				out.print(edge.dest);
				if (acceptance instanceof AcceptanceOmegaTransition) {
					out.println(((AcceptanceOmegaTransition) acceptance).getSignatureForEdgeHOA(i, (BitSet) edge.label));
				} else {
					out.println("");
				}
			}
		}
		out.println("--END--");
	}

	/**
	 * Print automaton to a PrismLog in a specified format ("dot" or "txt").
	 */
	public void print(PrismLog out, String type)
	{
		switch (type) {
		case "txt":
			out.println(toString());
			break;
		case "dot":
			printDot(out);
			break;
		// Default to txt
		default:
			out.println(toString());
			break;
		}
	}

	/**
	 * Print automaton to a PrintStream in a specified format ("dot", "txt" or "hoa").
	 */
	public void print(PrintStream out, String type) throws PrismNotSupportedException
	{
		switch (type) {
		case "txt":
			out.println(toString());
			break;
		case "dot":
			printDot(out);
			break;
		case "hoa":
			printHOA(out);
			break;
		// Default to txt
		default:
			out.println(toString());
			break;
		}
	}

	// Standard methods

	@Override
	public String toString()
	{
		String s = "";
		int i;
		s += size + " states (start " + start + ")";
		if (apList != null)
			s += ", " + apList.size() + " labels (" + apList + ")";
		s += ":";
		for (i = 0; i < size; i++) {
			for (Edge e : edges.get(i)) {
				s += " " + i + "-" + e.label + "->" + e.dest;
			}
		}
		s += "; " + acceptance.getType() + " acceptance: ";
		s += acceptance;
		return s;
	}

	public String getAutomataType()
	{
		return "D" + acceptance.getType().getNameAbbreviated() + "A";
	}

	/**
	 * Switch the acceptance condition. This may change the acceptance type,
	 * i.e., a DA&lt;BitSet, AcceptanceRabin&gt; may become a DA&lt;BitSet, AcceptanceStreett&gt;
	 * @param da the automaton
	 * @param newAcceptance the new acceptance condition
	 */
	public static void switchAcceptance(DA da, AcceptanceOmega newAcceptance)
	{
		// as Java generics are only compile time, we can change the AcceptanceType
		da.acceptance = newAcceptance;
	}

	/**
	 * Validates that the atomic propositions of this automaton
	 * conform to the standard values that PRISM expects:
	 *   L0, ..., Ln-1 (in arbitrary order)
	 * if there are {@code n} expected atomic propositions.
	 * <br/>
	 * The automaton may actually have less atomic propositions than expected,
	 * e.g., if the given atomic proposition does not influence the acceptance
	 * of a run in the automaton.
	 * <br/>
	 * If there is an error, throws a {@code PrismException} detailing the problem.
	 * @param expectedNumberOfAPs the expected number of atomic propositions
	 */
	public void checkForCanonicalAPs(int expectedNumberOfAPs) throws PrismException
	{
		BitSet seen = new BitSet();
		for (String ap : apList) {
			if (!ap.substring(0, 1).equals("L")) {
				throw new PrismException("In deterministic automaton, unexpected atomic proposition " + ap + ", expected L0, L1, ...");
			}
			try {
				int index = Integer.parseInt(ap.substring(1));
				if (seen.get(index)) {
					throw new PrismException("In deterministic automaton, duplicate atomic proposition " + ap);
				}
				if (index < 0) {
					throw new PrismException("In deterministic automaton, unexpected atomic proposition " + ap + ", expected L0, L1, ...");
				}
				if (index >= expectedNumberOfAPs) {
					throw new PrismException(
							"In deterministic automaton, unexpected atomic proposition " + ap + ", expected highest index to be " + (expectedNumberOfAPs - 1));
				}
				seen.set(index);
			} catch (NumberFormatException e) {
				throw new PrismException("In deterministic automaton, unexpected atomic proposition " + ap + ", expected L0, L1, ...");
			}
		}
		// We are fine with an empty apList or an apList that lacks some of the expected Li.
	}

	public Set<Edge> getAllEdgesFrom(int i)
	{
		return new HashSet<>(edges.get(i));
	}

	/**
	 * This method completes the DA by adding a trapState and adjusting the acceptance.
	 * This method is currently only used for an automaton obtained by Rabinizer; therefore,
	 *  it currently supports only AcceptanceGenRabinTransition as acceptance (and Bitset as Symbol).
	 */
	public void complete()
	{
		Collection<Integer> uncompleteStates = statesWhichAreNotComplete();
		if (uncompleteStates.isEmpty()) {
			return; //We are already complete
		}

		//Add trapstate
		this.size++;
		int trapState = this.size - 1;
		this.edges.add(new ArrayList<>());
		uncompleteStates.add(trapState);

		Collection<BitSet> allSymbols = getAllPossibleSymbols();
		for (int state : uncompleteStates) {
			for (BitSet bs : allSymbols) {
				if (!hasEdge(state, (Symbol) bs)) {
					this.addEdge(state, (Symbol) bs, trapState);
				}
			}
		}

		if (this.acceptance instanceof AcceptanceGenRabinTransition) {
			((AcceptanceGenRabinTransition) this.acceptance).makeTrapState(trapState, this);
		} else {
			throw new UnsupportedOperationException("Making the DA complete is currently" + " only supported for AcceptanceGenRabinTransition as acceptance"
					+ " and not for " + this.acceptance.getClass());
		}
	}

	/**
	 * This method returns a list of states, which are not complete,
	 *         such that the state has not 2^{APList.size()} many outgoing edges. 
	 */
	private Collection<Integer> statesWhichAreNotComplete()
	{
		return IntStream.range(0, size).filter(state -> edges.get(state).size() < 1 << apList.size()).mapToObj(i -> i).collect(Collectors.toSet());
	}

	/**
	 * @return Set of all possible symbols (aka BitSets), marking an edge.
	 *                Beware that this set has 2^{apList.size()} many elments. 
	 */
	public Collection<BitSet> getAllPossibleSymbols()
	{
		Set<BitSet> result = new HashSet<>();
		for (int i = 0; i < 1 << apList.size(); i++) {
			BitSet bs = new BitSet(apList.size());
			for (int offset = 0; offset < apList.size(); offset++) {
				bs.set(offset, (i & (1 << offset)) != 0);
			}
			result.add(bs);
		}
		return result;
	}
}
