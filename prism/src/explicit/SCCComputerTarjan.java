//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import prism.PrismComponent;

/**
 * Tarjan's SCC algorithm operating on a Model object.
 */
public class SCCComputerTarjan extends SCCComputer
{
	/* The model to compute (B)SCCs for */
	private Model model;
	/* Number of nodes (model states) */
	private int numNodes;
	/* Computed list of SCCs */
	private List<BitSet> sccs = new ArrayList<>();
	/* States not in non-trivial SCCs */
	private BitSet notInSCCs;
	/* Computed list of BSCCs */
	private List<BitSet> bsccs = new ArrayList<>();
	/* States not in any BSCC */
	private BitSet notInBSCCs;

	/* Next index to give to a node */
	private int index = 0;
	/* Stack of nodes */
	private List<Integer> stack = new LinkedList<>();
	/* List of nodes in the graph. Invariant: {@code nodeList.get(i).id == i} */
	private ArrayList<Node> nodeList;
	/* Nodes currently on the stack. */
	private BitSet onStack;

	/**
	 * Build (B)SCC computer for a given model.
	 */
	public SCCComputerTarjan(PrismComponent parent, Model model)
	{
		super(parent);
		this.model = model;
		this.numNodes = model.getNumStates();
		this.nodeList = new ArrayList<>(numNodes);
		for (int i = 0; i < numNodes; i++) {
			nodeList.add(new Node(i));
		}
		onStack = new BitSet();
	}

	// Methods for SCCComputer interface

	@Override
	public void computeSCCs()
	{
		tarjan();
		// Now remove trivial SCCs
		notInSCCs = new BitSet();
		for (Iterator<BitSet> it = sccs.iterator(); it.hasNext();) {
			BitSet scc = it.next();
			if (scc.cardinality() == 1) {
				int s = scc.nextSetBit(0);
				if (!model.someSuccessorsInSet(s, scc)) {
					it.remove(); // remove this SCC from sccs list
					notInSCCs.set(s);
				}
			}
		}
	}

	@Override
	public void computeBSCCs()
	{
		computeSCCs();
		notInBSCCs = (BitSet) getNotInSCCs().clone();
		int n = sccs.size();
		for (int i = 0; i < n; i++) {
			BitSet scc = sccs.get(i);
			boolean bottom = true;
			for (int s = scc.nextSetBit(0); s >= 0; s = scc.nextSetBit(s + 1)) {
				if (!model.allSuccessorsInSet(s, scc)) {
					bottom = false;
					break;
				}
			}
			if (bottom)
				bsccs.add(scc);
			else
				notInBSCCs.or(scc);
		}
	}

	@Override
	public List<BitSet> getSCCs()
	{
		return sccs;
	}

	@Override
	public BitSet getNotInSCCs()
	{
		return notInSCCs;
	}

	@Override
	public List<BitSet> getBSCCs()
	{
		return bsccs;
	}

	@Override
	public BitSet getNotInBSCCs()
	{
		return notInBSCCs;
	}

	// SCC Computation

	/**
	 * Execute Tarjan's algorithm. Determine maximal strongly connected components
	 * (SCCS) for the graph of the model and stored in {@code sccs}.
	 */
	public void tarjan()
	{
		for (int i = 0; i < numNodes; i++) {
			if (nodeList.get(i).lowlink == -1)
				tarjan(i);
		}

	}

	private void tarjan(int i)
	{
		final Node v = nodeList.get(i);
		v.index = index;
		v.lowlink = index;
		index++;
		stack.add(0, i);
		onStack.set(i);
		Iterator<Integer> it = model.getSuccessorsIterator(i);
		while (it.hasNext()) {
			int e = it.next();
			Node n = nodeList.get(e);
			if (n.index == -1) {
				tarjan(e);
				v.lowlink = Math.min(v.lowlink, n.lowlink);
			} else if (onStack.get(e)) {
				v.lowlink = Math.min(v.lowlink, n.index);
			}
		}
		if (v.lowlink == v.index) {
			int n;
			BitSet component = new BitSet();
			do {
				n = stack.remove(0);
				onStack.set(n, false);
				component.set(n);
			} while (n != i);
			sccs.add(component);
		}
	}

	/**
	 * A small class wrapping a node.
	 * It carries extra information necessary for Tarjan's algorithm.
	 */
	protected static class Node
	{
		public int lowlink = -1;
		public int index = -1;
		public int id;

		public Node(int id)
		{
			this.id = id;
		}
	}
}
