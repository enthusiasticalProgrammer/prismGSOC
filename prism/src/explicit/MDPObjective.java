package explicit;

import org.eclipse.jdt.annotation.NonNull;

import explicit.rewards.MDPReward;
import prism.Operator;

class MDPObjective extends MDPItem
{

	MDPObjective(@NonNull MDPReward reward, Operator operator)
	{
		super(reward, operator);
		assert(operator.equals(Operator.R_MAX) || operator.equals(Operator.R_MIN));
	}

}
