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
 * Interface for classes to store strategies (for MDPs, games, etc.)
 */
public interface Strategy
{
	// Types of info stored for each choice
	public enum Choice {
		INDEX, ACTION, UNKNOWN, ARBITRARY, UNREACHABLE;
	}

	/**
	 * Export the strategy to a PrismLog, displaying strategy choices as action names.
	 */
	public void exportActions(PrismLog out);

	/**
	 * Export the strategy to a PrismLog, displaying strategy choices as indices.
	 */
	public void exportIndices(PrismLog out);

	/**
	 * Export the model induced by this strategy to a PrismLog.
	 */
	public void exportInducedModel(PrismLog out);

	/**
	 * Export the strategy to a dot file (of the model showing the strategy).
	 */
	public void exportDotFile(PrismLog out);

	/**
	 * Initialises memory based on a state
	 * 
	 * @param state
	 *            initial state
	 */
	public void initialise(int s);

	/**
	 * Updates memory
	 * 
	 * @param action
	 *            action taken in the previous states
	 * @param state
	 *            the current state
	 * @throws InvalidStrategyStateException
	 *             if memory update function is not defined for the given
	 *             action, state and the current strategy's memory state.
	 */
	public void updateMemory(int action, int state) throws InvalidStrategyStateException;

	/**
	 * Next move function
	 * 
	 * @param state
	 *            current state
	 * @return the distribution on actions prescribed by the strategy in a
	 *         state.
	 * @throws InvalidStrategyStateException
	 *             if next move function is undefined for the given state in
	 *             current strategy's memory state.
	 */
	public Distribution getNextMove(int state) throws InvalidStrategyStateException;

	/**
	 * Clear storage of the strategy.
	 */
	public void clear();

	/**
	 * Resets the strategy to uninitialised state
	 */
	public void reset();

	/**
	 * Exports adversary to a given file
	 * 
	 * @param file
	 *            file name to which adversary will be exported
	 */
	public void exportToFile(String file);

	/**
	 * Builds the product of the model and the strategy. The product is built by
	 * adding extra integer variable to the state to represent the memory state
	 * of the strategy. The initial states are the first N states of the product
	 * where N is the size of the original model.
	 * 
	 * @param model
	 *            The model for which the strategy is defined.
	 * @return the model, which is the product of the input model and the strategy
	 * 
	 */
	public Model buildProduct(Model model);

	/**
	 * Get textual description of the strategy
	 * 
	 * @return the textual description of the strategy
	 */
	public String getInfo();

	/**
	 * Set textual description of the strategy
	 * 
	 * @param info
	 *            strategy information
	 */
	public void setInfo(String info);

	/**
	 * Returns the size of memory of the strategy.
	 * 
	 * @return size of memory
	 */
	public int getMemorySize();

	/**
	 * Returns strategy type
	 * 
	 * @return type of the strategy
	 */
	public String getType();

	/**
	 * Returns the current memory element that fully describes state of the
	 * strategy
	 * 
	 * @return the memory element (or null if it is a memoryless strategy)
	 */
	public Object getCurrentMemoryElement();

	/**
	 * Updates the strategy's state to the one provided
	 * 
	 * @param memory
	 *            memory element representing the state of the strategy
	 * @throws InvalidStrategyStateException
	 *             if the memory element is not recognised by the strategy
	 */
	public void setMemory(Object memory) throws InvalidStrategyStateException;

	/**
	 * Returns the textual description of the current state of the strategy
	 * (ideally, human readable)
	 * 
	 * @return textual description of the current state of the strategy
	 */
	public String getStateDescription();
}
