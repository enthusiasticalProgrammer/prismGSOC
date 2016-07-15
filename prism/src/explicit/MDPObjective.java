package explicit;

import explicit.rewards.MDPReward;
import prism.Operator;

/**
 * This class correpsonds to a multi-long-run objective of the form R{"reward"}max=? 
 */
class MDPObjective extends MDPItem
{

	MDPObjective(MDPReward reward, Operator operator)
	{
		super(reward, operator);
		assert (operator.equals(Operator.R_MAX) || operator.equals(Operator.R_MIN));
	}

}
