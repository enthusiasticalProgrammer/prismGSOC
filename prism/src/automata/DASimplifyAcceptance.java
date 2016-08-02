package automata;

import java.util.BitSet;
import java.util.Set;

import prism.PrismComponent;
import explicit.SCCComputer;
import acceptance.AcceptanceGenRabinTransition;
import acceptance.AcceptanceGenRabinTransition.GenRabinPair;
import acceptance.AcceptanceOmega;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceReach;
import acceptance.AcceptanceType;
import acceptance.AcceptanceRabin.RabinPair;
import common.IterableBitSet;

public class DASimplifyAcceptance
{

	/**
	 * Tries to simplify the acceptance condition of the deterministic automaton.
	 * Note that the passed parameter {@code da} may be destroyed by this function.
	 * @param parent the calling PrismComponent (for SCC computer)
	 * @param da the DA to be simplified (may be destroyed)
	 * @param allowedAcceptance the allowed acceptance types
	 */
	public static DA<BitSet, ? extends AcceptanceOmega> simplifyAcceptance(PrismComponent parent, DA<BitSet, ? extends AcceptanceOmega> da,
			AcceptanceType... allowedAcceptance)
	{
		// Simplifications for DRAs
		if (da.getAcceptance() instanceof AcceptanceRabin) {
			DA<BitSet, AcceptanceRabin> dra = (DA<BitSet, AcceptanceRabin>) da;
			// K_i states that do not occur in a (non-trivial) SCC of the DRA may as well be removed
			SCCComputer sccComp = explicit.SCCComputer.createSCCComputer(parent, new LTSFromDA(da));
			sccComp.computeBSCCs();
			BitSet trivial = sccComp.getNotInSCCs();
			for (RabinPair pair : dra.getAcceptance()) {
				if (pair.getK().intersects(trivial)) {
					pair.getK().andNot(trivial);
				}
			}
			// See if the DRA is actually a DFA
			if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.REACH) && isDFA(dra)) {
				// we can switch to AcceptanceReach
				AcceptanceReach reachAcceptance = new AcceptanceReach(getDFAGoalStatesForRabin(dra.getAcceptance()));
				DA.switchAcceptance(dra, reachAcceptance);
				da = dra;
			}
		} else if (da.getAcceptance() instanceof AcceptanceGenRabinTransition) {
			DA<BitSet, AcceptanceGenRabinTransition> dtgra = (DA<BitSet, AcceptanceGenRabinTransition>) da;
			// See if the DTGRA is actually a DFA
			if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.REACH) && isDfaDtgra(dtgra)) {
				// we can switch to AcceptanceReach
				AcceptanceReach reachAcceptance = new AcceptanceReach(getDFAGoalStatesForGenRabinTransition(dtgra));
				DA.switchAcceptance(da, reachAcceptance);
				da = dtgra;
			}
		}
		return da;
	}

	/**
	 * Is this Rabin automaton actually a finite automaton? This check is done syntactically:
	 * it returns true if every transition from a K_i state goes to another K_i state.
	 * We also require that there are no L_i states overlapping with any K_j states.
	 */
	public static boolean isDFA(DA<BitSet, AcceptanceRabin> dra)
	{
		AcceptanceRabin acceptance = dra.getAcceptance();
		// Compute potential set of goal states as the union of all K_i sets
		BitSet goalStates = getDFAGoalStatesForRabin(acceptance);

		// Make sure there are no L_i states in the goal states for any i
		for (int i = 0; i < acceptance.size(); i++) {
			if (goalStates.intersects(acceptance.get(i).getL()))
				return false;
		}
		// Check if every transition from a goal state goes to another goal state
		for (int i = goalStates.nextSetBit(0); i >= 0; i = goalStates.nextSetBit(i + 1)) {
			int m = dra.getNumEdges(i);
			for (int j = 0; j < m; j++) {
				if (!goalStates.get(dra.getEdgeDest(i, j)))
					return false;
			}
		}
		return true;
	}

	/**
	 * Analogue to method above 
	 */
	private static boolean isDfaDtgra(DA<BitSet, AcceptanceGenRabinTransition> dtgra)
	{
		AcceptanceGenRabinTransition acceptance = dtgra.getAcceptance();
		// Compute potential set of goal states as the union of all K_i sets
		BitSet goalStates = getDFAGoalStatesForGenRabinTransition(dtgra);
		if (goalStates.isEmpty()) {
			return false;
		}
		// Make sure there are no Finite transitions in the goal states for any pair
		if (!acceptance.accList.stream()
				.allMatch(pair -> pair.Finite.stream().map(finEdge -> acceptance.computeStartStateOfEdge(finEdge)).allMatch(i -> !goalStates.get(i)))) {
			return false;
		}
		// Check if every transition from a goal state goes to another goal state, and whether it intersects with all Inf-transitions
		for (int goalState = goalStates.nextSetBit(0); goalState >= 0; goalState = goalStates.nextSetBit(goalState + 1)) {
			int m = dtgra.getNumEdges(goalState);
			for (int edgeNum = 0; edgeNum < m; edgeNum++) {
				if (!goalStates.get(dtgra.getEdgeDest(goalState, edgeNum))) {
					return false;
				}
				// check Inf-transition intersection
				for (GenRabinPair pair : acceptance.accList) {
					for (BitSet inf : pair.Infinite) {
						if (!inf.get(acceptance.computeOffsetForEdge(goalState, dtgra.getEdgeLabel(goalState, edgeNum)))) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	/**
	 * Get the union of the K_i states of a Rabin acceptance condition.
	 */
	public static BitSet getDFAGoalStatesForRabin(AcceptanceRabin acceptance)
	{
		// Compute set of goal states as the union of all K_i sets
		BitSet goalStates = new BitSet();
		int n = acceptance.size();
		for (int i = 0; i < n; i++) {
			goalStates.or(acceptance.get(i).getK());
		}
		return goalStates;
	}

	/**
	 * analogue to method above 
	 */
	private static BitSet getDFAGoalStatesForGenRabinTransition(DA<BitSet, AcceptanceGenRabinTransition> da)
	{
		// Compute set of goal states as the union of all K_i sets
		BitSet goalStates = new BitSet();
		for (GenRabinPair pair : da.getAcceptance().accList) {
			BitSet currentStates = new BitSet();
			currentStates.set(0, da.size());
			pair.Infinite.stream().forEach(inf -> {
				BitSet currInf = new BitSet();
				inf.stream().forEach(i -> currInf.set(da.getAcceptance().computeStartStateOfEdge(i)));
				currentStates.and(currInf);
			});
			BitSet FiniteState = new BitSet();
			pair.Finite.stream().forEach(i -> FiniteState.set(da.getAcceptance().computeStartStateOfEdge(i)));
			currentStates.andNot(FiniteState);

			goalStates.or(currentStates);
		}

		boolean goalsChanged = true;
		while (goalsChanged) { // fixPoint iteration
			goalsChanged = false;
			for (int goal : IterableBitSet.getSetBits(goalStates)) {
				Set<DA<BitSet, AcceptanceGenRabinTransition>.Edge> successors = da.getAllEdgesFrom(goal);
				if (successors.stream().anyMatch(successor -> !goalStates.get(successor.dest))) {
					goalStates.clear(goal);
					goalsChanged = true;
					break;
				}
			}
		}
		return goalStates;
	}
}
