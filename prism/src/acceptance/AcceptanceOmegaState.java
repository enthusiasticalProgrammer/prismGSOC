package acceptance;

import java.util.BitSet;

import jdd.JDDVars;
import prism.PrismException;

public interface AcceptanceOmegaState extends AcceptanceOmega
{
	/** Abstract functor for use with the lift function. */
	public static abstract class LiftBitSet
	{
		public abstract BitSet lift(BitSet states);
	}

	/**
	 * Lift the state sets in the acceptance condition.
	 * For each state set {@code states} in the condition,
	 * {@code lifter.lift(states)} is called and the state set is
	 * replaced by the result.
	 **/
	public void lift(LiftBitSet lifter);

	/**
	 * Complement the acceptance condition if possible.
	 * @param numStates the number of states in the underlying model / automaton (needed for complementing BitSets)
	 * @param allowedAcceptance the allowed acceptance types that may be used for complementing
	 */
	public AcceptanceOmegaState complement(int numStates, AcceptanceType... allowedAcceptance) throws PrismException;

	/** 
	 * Get the acceptance signature for state {@code stateIndex}
	 */
	public String getSignatureForState(int stateIndex);

	/** 
	 * Get the acceptance signature for state {@code stateIndex} in HOA format.
	 */
	public String getSignatureForStateHOA(int stateIndex);

	/**
	 * Convert this BitSet based acceptance condition to the corresponding BDD based acceptance condition.
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the bitset
	 */
	public AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars);

	/**
	 * Convert this acceptance condition to an AcceptanceGeneric condition.
	 */
	public AcceptanceGeneric toAcceptanceGeneric();
}
