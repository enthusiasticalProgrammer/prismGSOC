package acceptance;

import prism.PrismException;

public interface AcceptanceOmegaState extends AcceptanceOmega
{

	/**
	 * Complement the acceptance condition if possible.
	 * @param numStates the number of states in the underlying model / automaton (needed for complementing BitSets)
	 * @param allowedAcceptance the allowed acceptance types that may be used for complementing
	 */
	public AcceptanceOmegaState complement(int numStates, AcceptanceType... allowedAcceptance) throws PrismException;

	/**
	 * Convert this acceptance condition to an AcceptanceGeneric condition.
	 */
	public AcceptanceGeneric toAcceptanceGeneric();
}
