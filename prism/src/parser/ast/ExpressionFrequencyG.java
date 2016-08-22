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

package parser.ast;

import ltl.parser.Comparison;

/**
 * This class corresponds to a frequency-G operator 
 */
public class ExpressionFrequencyG extends ExpressionTemporal
{
	/**
	 * The maximal (or minimal bound), for example in G {inf >= 0.2} phi, it is 0.2
	 * <p>
	 * It should reasonably be between strictly 0 and 1 (if it is equal to 0 or 1, it can be expressed by ordinary LTL,
	 * which can usually be model-checked faster). However the algorithms should also work, if the bound is not between
	 * 0 and 1 (although something like G {inf >= 999} phi does not make much sense, since it is always false)
	 */
	public final double bound;

	/**
	 * Any comparison operator besides == is possible. 
	 */
	public final Comparison cmpOperator; // the cmpOperator

	/**
	 * if this is true, we consider lim inf, else we consider lim sup. 
	 */
	public final boolean isLimInf;

	/**
	 * @param bound the value for the bound
	 * @param cmpOperator the comparison operator
	 * @param isLimInf either true for lim inf or false for lim sup
	 */
	public ExpressionFrequencyG(double bound, Comparison cmpOperator, boolean isLimInf)
	{
		this.bound = bound;
		this.cmpOperator = cmpOperator;
		this.isLimInf = isLimInf;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null) {
			return false;
		}
		if (o instanceof ExpressionFrequencyG) {
			ExpressionFrequencyG freq = (ExpressionFrequencyG) o;
			return super.equals(o) && this.bound == freq.bound && this.cmpOperator == freq.cmpOperator && this.isLimInf == freq.isLimInf;
		}
		return false;
	}

	@Override
	public String toString()
	{
		String s = "G {";
		s = s + (isLimInf ? "inf" : "sup");
		s = s + " ";
		s = s + cmpOperator.toString();
		s = s + " ";
		s = s + bound;
		s = s + "} ";
		return s + super.toString().substring(1);
	}

	@Override
	public void setOperator(int op)
	{
		if (op == ExpressionTemporal.P_FREQ) {
			super.setOperator(op);
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public int getOperator()
	{
		return ExpressionTemporal.P_FREQ;
	}

	@Override
	public ExpressionFrequencyG deepCopy()
	{
		ExpressionFrequencyG expr = new ExpressionFrequencyG(this.bound, this.cmpOperator, this.isLimInf);
		expr.setOperator(op);
		if (operand1 != null)
			expr.setOperand1(operand1.deepCopy());
		if (operand2 != null)
			expr.setOperand2(operand2.deepCopy());
		expr.setLowerBound(lBound == null ? null : lBound.deepCopy(), lBoundStrict);
		expr.setUpperBound(uBound == null ? null : uBound.deepCopy(), uBoundStrict);
		expr.equals = equals;
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
}
