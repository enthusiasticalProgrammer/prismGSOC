//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Christopher Ziegler <ga25suc@mytum.de>
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import explicit.rewards.MDPRewards;
import prism.Operator;

/**
 * This class corresponds to a multi-long-run property of the form P >= probabilty [reward > bound [S] ].
 */
class MDPConstraint extends MDPExpectationConstraint
{
	final double probability;

	MDPConstraint(MDPRewards reward, prism.Operator operator, double bound, double probability)
	{
		super(reward, operator, bound);
		checkForIllegalArguments(operator, probability);
		this.probability = probability;
	}

	MDPConstraint(MDPRewards reward, prism.Operator operator, double bound)
	{
		this(reward, operator, bound, 5.0);
	}

	private void checkForIllegalArguments(Operator operator2, double probability2)
	{
		if (!(operator2.equals(Operator.R_MIN) || operator2.equals(Operator.R_MAX) || operator2.equals(Operator.R_GE) || operator2.equals(Operator.R_LE))) {
			throw new IllegalArgumentException("wrong operator in MDPConstraint");
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
