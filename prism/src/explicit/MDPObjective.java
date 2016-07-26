package explicit;

import explicit.rewards.MDPRewards;
import prism.Operator;

/**
 * This class correpsonds to a multi-long-run objective of the form R{"reward"}max=? 
 */
class MDPObjective extends MDPItem
{

	MDPObjective(MDPRewards reward, Operator operator)
	{
		super(reward, operator);
		assert (operator.equals(Operator.R_MAX) || operator.equals(Operator.R_MIN));
	}

}
