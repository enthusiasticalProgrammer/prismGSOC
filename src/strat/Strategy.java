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

 * 
 * @author aistis
 * 
 */
public interface Strategy
{
	// Types of info stored for each choice
	public enum Choice {
		INDEX, ACTION, UNKNOWN, ARBITRARY, UNREACHABLE;
	};

	/**
	 * Initialises memory based on a state
	 * 
	 * @param state
	 *            initial state
	 * @throws InvalidStrategyStateException
	 *             if the initial distribution function is undefined for the
	 *             given state
	 */
	public void init(int state) throws InvalidStrategyStateException;

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
	 * @throws PrismException
	 * 
	 */
	public Model buildProduct(Model model) throws PrismException;

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
	 * @return
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

	/**
	 * Returns the initial memory state of the strategy for the state. It is
	 * required by the model checker to determine which states can be treated as
	 * initial in the product.
	 * 
	 * @param s
	 *            the state for which to return initial memory element
	 * 
	 * @return non negative integer or -1 if product does not contain extra
	 *         variables
	 */
	public int getInitialStateOfTheProduct(int s);

	//	/**
	//	 * Retrieve the expected value that this strategy will achieve from it's
	//	 * current state
	//	 * @return the expect value of the function, return -1 if exp values are not defined
	//	 */
	//	public double getExpectedValue();
	//	
	//	/**
	//	 * Get expected value if a given action was taken and given state turned out to be a successor
	//	 * @param action action
	//	 * @param state state
	//	 * @return expectation
	//	 */
	//	public double getExpectedValue(int action, int state);

	// NEW METHODS:

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
	 * Initialise the strategy, based on an initial model state.
	 * @param s Initial state of the model
	 */
	public void initialise(int s);

	/**
	 * Update the strategy, based on the next step in a model's history.
	 * @param action The action taken in the previous state of the model
	 * @param s The new state of the model
	 */
	public void update(Object action, int s);

	/**
	 * Get the action chosen by the strategy in the current state (assuming it is deterministic). 
	 */
	public Object getChoiceAction();

	/**
	 * Clear storage of the strategy.
	 */
	public void clear();
}
