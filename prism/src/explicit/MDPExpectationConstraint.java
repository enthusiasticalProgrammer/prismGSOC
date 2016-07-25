package explicit;

import explicit.rewards.MDPReward;
import prism.Operator;

/**
 * This class corresponds to a multi-long-run property of the form R{"reward"} >= bound 
 */
public class MDPExpectationConstraint extends MDPItem
{

	final double bound;

	MDPExpectationConstraint(MDPReward reward, Operator operator, double bound)
	{
		super(reward, operator);
		this.bound = bound;
	}

	@Override
	public boolean equals(Object object)
	{
		if (!super.equals(object)) {
			return false;
		}
		MDPConstraint that = (MDPConstraint) object;
		if (!prism.PrismUtils.doublesAreEqual(this.bound, that.bound)) {
			return false;
		}
		return true;
	}

	double getBound()
	{
		return bound;
	}
}
