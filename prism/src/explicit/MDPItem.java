package explicit;

import org.eclipse.jdt.annotation.NonNull;

import explicit.rewards.MDPReward;

/**
 * This abstract class stores a multi-long-run satisfaction constraint for an MDP.
 * The operator can be max/geq/..., and the reward is a certain reward.
 */
abstract class MDPItem
{
	final @NonNull MDPReward reward; //use also min/max ....
	final prism.Operator operator;

	MDPItem(@NonNull MDPReward reward, prism.Operator operator)
	{
		this.reward = reward;
		this.operator = operator;
	}

	@Override
	public boolean equals(Object object)
	{
		if (object == null || object.getClass() != this.getClass()) {
			return false;
		}
		MDPItem that = (MDPConstraint) object;
		if (!this.reward.equals(that.reward))
			return false;
		if (!this.operator.equals(that.operator)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode()
	{
		return 17 * reward.hashCode() + 19 * operator.hashCode();
	}
}
