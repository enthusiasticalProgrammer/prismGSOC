package acceptance;

import java.util.Vector;

import jdd.JDDNode;
import jdd.JDDVars;
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

	/**
	 * Convert this BitSet based acceptance condition to the corresponding BDD based acceptance condition.
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the bitset
	 */
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars);

	@Override
	default AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars, Vector<JDDNode> labelAPs)
	{
		return toAcceptanceDD(ddRowVars);
	}
}
