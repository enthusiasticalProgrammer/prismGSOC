package acceptance;

import java.util.BitSet;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

public class AcceptanceGenRabinTransitionDD<Symbol> implements AcceptanceOmegaDD
{
	private final AcceptanceGenRabinTransition<Symbol> acceptanceGenRabinTransition;
	private final JDDVars ddRowVars;

	public AcceptanceGenRabinTransitionDD(AcceptanceGenRabinTransition<Symbol> acceptanceGenRabinTransition, JDDVars ddRowVars)
	{
		this.acceptanceGenRabinTransition = acceptanceGenRabinTransition;
		this.ddRowVars = ddRowVars;
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		return this.acceptanceGenRabinTransition.isBSCCAccepting(transformJDDToBitSet(bscc_states));
	}

	private BitSet transformJDDToBitSet(JDDNode bscc){
			BitSet result = new BitSet(ddRowVars.getNumVars());
			for(int i=0; i< ddRowVars.getNumVars();i++){
				if(JDD.GetVectorElement(bscc, ddRowVars, i)>0.5){
					result.set(i);
				}
			}
		return result;
	}

	@Override
	public String getSizeStatistics()
	{
		return this.acceptanceGenRabinTransition.getSizeStatistics();
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.GENERALIZED_RABIN;
	}

	@Override
	public void clear()
	{
		//Nothing to do
	}
}
