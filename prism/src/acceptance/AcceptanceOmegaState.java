package acceptance;

import java.util.BitSet;
import java.util.Vector;

import automata.DA;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.PrismException;
import prism.ProbModel;

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

	/**
	 * It is not recommended to override this method, due to the fact that the other parameters are
	 * not necessary for state-based acceptance. It is better to override the other toAcceptanceDD method. 
	 */
	@Override
	default AcceptanceOmegaDD toAcceptanceDD(JDDVars ddRowVars, JDDVars daColVars, JDDVars allddRowVars, JDDVars allddColVars, DA<BitSet, ?> da,
			Vector<JDDNode> labelAPs, ProbModel modelProduct)
	{
		return toAcceptanceDD(ddRowVars);
	}
}
