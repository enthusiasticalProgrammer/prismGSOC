//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
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

import prism.PrismLangException;

/**
 * Class to represent a relational operator (or similar) found in a P/R/S operator.
 */
public enum RelOp {
	GT(">") {
		@Override
		public boolean isLowerBound()
		{
			return true;
		}

		@Override
		public boolean isStrict()
		{
			return true;
		}

		@Override
		public RelOp negate()
		{
			return LEQ;
		}
	},
	GEQ(">=") {
		@Override
		public boolean isLowerBound()
		{
			return true;
		}

		@Override
		public RelOp negate()
		{
			return LT;
		}
	},
	MIN("min=") {
		@Override
		public boolean isMin()
		{
			return true;
		}

		@Override
		public RelOp negate()
		{
			return MAX;
		}
	},
	LT("<") {
		@Override
		public boolean isUpperBound()
		{
			return true;
		}

		@Override
		public boolean isStrict()
		{
			return true;
		}

		@Override
		public RelOp negate()
		{
			return GEQ;
		}
	},
	LEQ("<=") {
		@Override
		public boolean isUpperBound()
		{
			return true;
		}

		@Override
		public RelOp negate()
		{
			return GT;
		}
	},
	MAX("max=") {
		@Override
		public boolean isMax()
		{
			return false;
		}

		@Override
		public RelOp negate()
		{
			return MIN;
		}
	},
	EQ("=") {
		@Override
		public RelOp negate() throws PrismLangException
		{
			throw new PrismLangException("Cannot negate " + this);
		}
	};

	private final String symbol;

	private RelOp(String symbol)
	{
		this.symbol = symbol;
	}

	@Override
	public String toString()
	{
		return symbol;
	}

	/**
	 * Returns true if this corresponds to a lower bound (i.e. &gt;, &gt;=).
	 * NB: "min=?" does not return true for this.
	 */
	public boolean isLowerBound()
	{
		return false;
	}

	/**
	 * Returns true if this corresponds to an upper bound (i.e. &lt;, &lt;=).
	 * NB: "max=?" does not return true for this.
	 */
	public boolean isUpperBound()
	{
		return false;
	}

	/**
	 * Returns true if this is a strict bound (i.e. &lt; or &gt;).
	 */
	public boolean isStrict()
	{
		return false;
	}

	/**
	 * Returns true if this corresponds to minimum (min=?).
	 */
	public boolean isMin()
	{
		return false;
	}

	/**
	 * Returns true if this corresponds to maximum (max=?).
	 */
	public boolean isMax()
	{
		return false;
	}

	/**
	 * Returns the negated form of this operator.
	 */
	public abstract RelOp negate() throws PrismLangException;

	/**
	 * Returns the RelOp object corresponding to a (string) symbol,
	 * e.g. parseSymbol("&lt;=") returns RelOp.LEQ. Returns null if invalid.
	 * @param symbol The symbol to look up
	 * @return
	 */
	public static RelOp parseSymbol(String symbol)
	{
		for (RelOp relop : RelOp.values()) {
			if (relop.toString().equals(symbol)) {
				return relop;
			}
		}
		return null;
	}
}