//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* @aistis
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

package strat;

/**
 * Exception to be thrown when the strategy is not used in a proper way, i.e.,
 * next move function is undefined for a given pair or memory and state.
 * 
 * @author aistis
 * 
 */
public class InvalidStrategyStateException extends Exception
{

	public InvalidStrategyStateException(String string)
	{
		super(string);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 6297385672024098036L;

}
