package explicit;

import org.eclipse.jdt.annotation.NonNull;

import explicit.rewards.MDPReward;
import prism.Operator;


class MDPConstraint extends MDPExpectationConstraint
{
	final double probability;

	MDPConstraint(@NonNull MDPReward reward, prism.Operator operator, double bound, double probability)
	{
		super(reward, operator, bound);
		checkForIllegalArguments(reward, operator, bound, probability);
		this.probability = probability;
	}

	MDPConstraint(@NonNull MDPReward reward, prism.Operator operator, double bound)
	{
		this(reward, operator, bound, 5.0);
	}

	private void checkForIllegalArguments(MDPReward reward2, Operator operator2, double bound2, double probability2)
	{
		if (!(operator2.equals(Operator.R_MIN) || operator2.equals(Operator.R_MAX) || operator2.equals(Operator.R_GE) || operator2.equals(Operator.R_LE))) {
			throw new IllegalArgumentException("wrong operator in MDPSatisfactionConstraint");
		} else if (probability2 < 0.0) {
			throw new IllegalArgumentException("A probability smaller than 0.0 is difficult to handle. Try to increase it.");
		}

	}

	boolean isProbabilistic()
	{
		return probability <= 1.0;
	}

	double getProbability()
	{
		if (!this.isProbabilistic())
			throw new UnsupportedOperationException();
		return probability;
	}

	@Override
	public boolean equals(Object object)
	{
		if (!super.equals(object)) {
			return false;
		}
		MDPConstraint that = (MDPConstraint) object;
		return (this.isProbabilistic() && that.isProbabilistic()) || (prism.PrismUtils.doublesAreEqual(this.probability, that.probability));
	}
}
