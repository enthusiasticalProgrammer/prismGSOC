//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Aistis Simaitis <aistis.aimaitis@cs.ox.ac.uk> (University of Oxford)
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

import explicit.Distribution;
import explicit.Model;
import prism.PrismException;
import prism.PrismLog;

/**
 * Classes to store memoryless deterministic (MD) strategies.
 */
public abstract class MDStrategy implements Strategy
{
	/**
	 * Current state of model
	 */
	protected int currentState = -1;

	/**
	 * Get the number of states of the model associated with this strategy. 
	 */
	public abstract int getNumStates();

	/**
	 * Is choice information stored for state s?
	 */
	public abstract boolean isChoiceDefined(int s);

	/**
	 * Get the type of choice information stored for state s.
	 */
	public abstract Strategy.Choice getChoice(int s);

	/**
	 * Get the index of the choice taken in state s.
	 * The index is defined with respect to a particular model, stored locally.
	 * Other possible values: -1 (unknown), -2 (arbitrary), -3 (unreachable)
	 */
	public abstract int getChoiceIndex(int s);

	/**
	 * Get the action taken in state s.
	 */
	public abstract Object getChoiceAction(int s);

	// Methods for Strategy

	@Override
	public void exportActions(PrismLog out)
	{
		int n = getNumStates();
		for (int s = 0; s < n; s++) {
			if (isChoiceDefined(s))
				out.println(s + ":" + getChoiceAction(s));
		}
	}

	@Override
	public void exportIndices(PrismLog out)
	{
		int n = getNumStates();
		for (int s = 0; s < n; s++) {
			if (isChoiceDefined(s))
				out.println(s + ":" + getChoiceIndex(s));
		}
	}
	
	// Temp stubs

	@Override
	public void init(int state)
	{
		currentState=state;
	}

	@Override
	public void updateMemory(int action, int state) throws InvalidStrategyStateException
	{
		//Do nothing, because the strategy is memoryless
	}

	@Override
	public Distribution getNextMove(int state) throws InvalidStrategyStateException
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void reset()
	{
		//nothing to do, because we are memoryless
	}

	@Override
	public void exportToFile(String file)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Model buildProduct(Model model) throws PrismException
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public String getInfo()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public void setInfo(String info)
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public int getMemorySize()
	{
		return 0;
	}

	@Override
	public String getType()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Object getCurrentMemoryElement()
	{
		return null;
	}

	@Override
	public void setMemory(Object memory) throws InvalidStrategyStateException
	{
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public String getStateDescription()
	{
		throw new UnsupportedOperationException("not yet implemented");
	}
}
