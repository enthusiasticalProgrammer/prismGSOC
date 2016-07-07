package acceptance;

public interface AcceptanceOmegaTransition<Symbol> extends AcceptanceOmega
{
	/** Get the acceptance signature for edge {@code stateIndex}.
	 **/
	public String getSignatureForEdge(int startState,Symbol sym); //TODO write

	/** Get the acceptance signature for state {@code stateIndex} (HOA format).
	 */
	public String getSignatureForEdgeHOA(int startState, Symbol sym); //TODO write
}
