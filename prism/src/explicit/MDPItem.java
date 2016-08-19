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

/**
 * This abstract class stores a multi-long-run satisfaction constraint for an MDP.
 * The operator can be max/geq/..., and the reward is a certain reward.
 */
abstract class MDPItem
{
	final MDPRewards reward; //use also min/max ....
	final prism.Operator operator;

	MDPItem(MDPRewards reward, prism.Operator operator)
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
