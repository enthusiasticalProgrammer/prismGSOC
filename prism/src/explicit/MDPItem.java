package explicit;

import org.eclipse.jdt.annotation.NonNull;

import explicit.rewards.MDPReward;

/**
 * This class stores a satisfaction constraint for an MDP.
 * It has as attributes the operator (LEQ/GEQ),
 * the bound as double, and the probability, for which it has to hold.
 * A probability >1.0 means that the constraint should hold always.
 */
abstract class MDPItem
{
	final @NonNull MDPReward reward; //use also min/max ....
	final prism.Operator operator;
	
	MDPItem(@NonNull MDPReward reward, prism.Operator operator){
		this.reward=reward;
		this.operator=operator;
	}
	
	@Override
	public boolean equals(Object object){
		if(object==null || object.getClass()!=this.getClass()){
			return false;
		}
		MDPItem that=(MDPConstraint) object;
		if(!this.reward.equals(that.reward))
			return false;
		if(!this.operator.equals(that.operator)){
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode()
	{
		return 17*reward.hashCode()+19*operator.hashCode();
	}
}
