package acceptance;

public interface AcceptanceOmegaTransition<Symbol> extends AcceptanceOmega
{
	//TODO if necessary: also write something like a getSignatureForEdge, which outputs the edge-signature in Dot
	/** Get the acceptance signature for state {@code stateIndex} (HOA format).
	 */
	public String getSignatureForEdgeHOA(int startState, Symbol sym);

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
}
