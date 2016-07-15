package acceptance;

import java.util.BitSet;
import java.util.Map;

public interface AcceptanceOmegaTransition extends AcceptanceOmega
{
	//TODO if necessary: also write something like a getSignatureForEdge, which outputs the edge-signature in Dot
	/** Get the acceptance signature for state {@code stateIndex} (HOA format).
	 */
	public String getSignatureForEdgeHOA(int startState, BitSet label);

	@Override
	public default String getSignatureForState(int stateIndex)
	{
		return "";
	}

	@Override
	public default String getSignatureForStateHOA(int stateIndex)
	{
		return "";
	}

	/**
	 * This method is used for removing any edges which are never traversed in the explicit DA-Model-Product. The parameter
	 * is a map storing the only usable edges (making the acceptance de facto state-based, but de typo it stays transition-based,
	 * because transforming the BitSets storing edges to BitSets storing states is neither tidy nor maintainable)
	 */
	public void removeUnneccessaryProductEdges(Map<Integer, BitSet> usedEdges);
}
