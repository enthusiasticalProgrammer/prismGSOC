//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Mateusz Ujma <mateusz.ujma@cs.ox.ac.uk> (University of Oxford)
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

import prism.PrismComponent;
import prism.PrismException;

/**
 * Explicit maximal end component computer for a nondeterministic model such as an MDP.
 * Implements the algorithm from p.48 of:
 * Luca de Alfaro. Formal Verification of Probabilistic Systems. Ph.D. thesis, Stanford University (1997)
 */
public class ECComputerDefault extends ECComputer
{
	/** The model to compute (M)ECs for **/
	private NondetModel model;

	/** Computed list of MECs **/
	private @NonNull List<@NonNull BitSet> mecs = new ArrayList<@NonNull BitSet>();

	/**
	 * Build (M)EC computer for a given model.
	 */
	public ECComputerDefault(PrismComponent parent, NondetModel model)
	{
		super(parent);
		this.model = model;
	}

	// Methods for ECComputer interface

	@Override
	public void computeMECStates()
	{
		mecs = findEndComponents(null, null);
	}

	@Override
	public void computeMECStates(BitSet restrict)
	{
		mecs = findEndComponents(restrict, null);
	}

	@Override
	public void computeMECStates(BitSet restrict, BitSet accept)
	{
		mecs = findEndComponents(restrict, accept);
	}

	@Override
	public @NonNull List<@NonNull BitSet> getMECStates()
	{
		return mecs;
	}

	// Computation

	/**
	 * Find all accepting maximal end components (MECs) in the submodel obtained
	 * by restricting this one to the set of states {@code restrict},
	 * where acceptance is defined as those which intersect with {@code accept}.
	 * If {@code restrict} is null, we look at the whole model, not a submodel.
	 * If {@code accept} is null, the acceptance condition is trivially satisfied.
	 * @param restrict BitSet for the set of states to restrict to
	 * @param accept BitSet for the set of accepting states
	 * @return a list of BitSets representing the MECs
	 */
	private @NonNull List<@NonNull BitSet> findEndComponents(BitSet restrict, BitSet accept)
	{
		// If restrict is null, look within set of all reachable states
		if (restrict == null) {
			restrict = new BitSet();
			restrict.set(0, model.getNumStates());
		}
		// Initialise L with set of all states to look in (if non-empty)
		@NonNull
		List<@NonNull BitSet> L = new ArrayList<@NonNull BitSet>();
		if (restrict.isEmpty())
			return L;
		L.add(restrict);
		// Find MECs
		boolean changed = true;
		while (changed) {
			changed = false;
			BitSet E = L.remove(0);
			SubNondetModel submodel = restrict(E);
			@NonNull
			List<@NonNull BitSet> sccs = translateStates(submodel, computeSCCs(submodel));
			L = replaceEWithSCCs(L, E, sccs);
			changed = canLBeChanged(L, E);
		}
		// Filter and return those that contain a state in accept
		if (accept != null) {
			int i = 0;
			while (i < L.size()) {
				if (!L.get(i).intersects(accept)) {
					L.remove(i);
				} else {
					i++;
				}
			}
		}
		return L;
	}

	private Set<BitSet> processedSCCs = new HashSet<BitSet>();

	private boolean canLBeChanged(List<BitSet> L, BitSet E)
	{
		processedSCCs.add(E);
		for (int i = 0; i < L.size(); i++) {
			if (!processedSCCs.contains(L.get(i))) {
				return true;
			}
		}
		return false;
	}

	private @NonNull List<@NonNull BitSet> replaceEWithSCCs(@NonNull List<@NonNull BitSet> L, BitSet E, @NonNull List<@NonNull BitSet> sccs)
	{
		if (sccs.size() > 0) {
			@NonNull
			List<@NonNull BitSet> toAdd = new ArrayList<@NonNull BitSet>();
			for (int i = 0; i < sccs.size(); i++) {
				if (!L.contains(sccs.get(i))) {
					toAdd.add(sccs.get(i));
				}
			}
			if (toAdd.size() > 0) {
				L.addAll(toAdd);
			}
		}
		return L;
	}

	private SubNondetModel restrict(BitSet states)
	{
		Map<Integer, BitSet> actions = new HashMap<Integer, BitSet>();
		BitSet initialStates = new BitSet();
		initialStates.set(states.nextSetBit(0));

		boolean changed = true;
		while (changed) {
			changed = false;
			actions.clear();
			for (int i = 0; i < model.getNumStates(); i++) {
				BitSet act = new BitSet();
				if (states.get(i)) {
					for (int j = 0; j < model.getNumChoices(i); j++) {
						if (model.allSuccessorsInSet(i, j, states)) {
							act.set(j);
						}
					}
					if (act.isEmpty()) {
						states.clear(i);
						changed = true;
					}
					actions.put(i, act);
				}
			}
		}

		return new SubNondetModel(model, states, actions, initialStates);
	}

	private List<BitSet> computeSCCs(NondetModel model)
	{
		SCCComputer sccc = SCCComputer.createSCCComputer(this, model);
		sccc.computeSCCs();
		return sccc.getSCCs();
	}

	private @NonNull List<@NonNull BitSet> translateStates(SubNondetModel model, List<BitSet> sccs)
	{
		@NonNull
		List<@NonNull BitSet> r = new ArrayList<@NonNull BitSet>();
		for (int i = 0; i < sccs.size(); i++) {
			BitSet set = sccs.get(i);
			BitSet set2 = new BitSet();
			r.add(set2);
			for (int j = set.nextSetBit(0); j >= 0; j = set.nextSetBit(j + 1)) {
				set2.set(model.translateState(j));

			}
		}
		return r;
	}
}
