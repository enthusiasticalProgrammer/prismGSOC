//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package simulator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import explicit.Distribution;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.ast.Expression;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.type.Type;
import prism.ModelType;
import prism.Prism;
import prism.PrismComponent;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLangException;
import prism.PrismLog;
import prism.PrismUtils;
import prism.ResultsCollection;
import prism.UndefinedConstants;
import simulator.method.SimulationMethod;
import simulator.sampler.Sampler;
import strat.InvalidStrategyStateException;
import strat.Strategy;
import userinterface.graph.Graph;

/**
 * A discrete event simulation engine for PRISM models.
 * 
 * The SimulatorEngine class provides support for:
 * <UL>
 * <LI> State/model exploration
 * <LI> Manual/random path generation
 * <LI> Monte-Carlo sampling techniques for approximate property verification
 * </UL>
 * 
 * After creating a SimulatorEngine object, you can build paths or explore models using:
 * <UL>
 * <LI> {@link #createNewPath} if you want to create a path that will be stored in full
 * <LI> {@link #createNewOnTheFlyPath} if just want to do e.g. model exploration
 * </UL>
 * The input to these methods is a model (ModulesFile) in which all constants have already been defined.
 * 
 * At this point, you can also load labels and properties into the simulator, whose values
 * will be available during path generation. Use:
 * <UL>
 * <LI> {@link #addLabel}
 * <LI> {@link #addProperty}
 * </UL>
 * 
 * To actually initialise the path with an initial state (or to reinitialise later) use:
 * <UL>
 * <LI> {@link #initialisePath}
 * </UL>
 * 
 * For sampling-based approximate model checking, use:
 * <UL>
 * <LI> {@link #isPropertyOKForSimulation}
 * <LI> {@link #checkPropertyForSimulation}
 * <LI> {@link #modelCheckSingleProperty}
 * <LI> {@link #modelCheckMultipleProperties}
 * <LI> {@link #modelCheckExperiment}
 * </UL>
 */
public class SimulatorEngine extends PrismComponent
{
	// The current parsed model + info
	private ModulesFile modulesFile;
	private ModelType modelType;
	// Variable info
	private VarList varList;
	private int numVars;
	// Constant definitions from model file
	private Values mfConstants;

	// Objects from model checking
	// Strategy
	private Strategy strategy;

	// Labels + properties info
	protected List<Expression> labels;
	private List<Expression> properties;
	private List<Sampler> propertySamplers;

	// Current path info
	protected Path path;
	protected boolean onTheFly;
	// Current state (note: this info is duplicated in the path - it is always the same
	// as path.getCurrentState(); we maintain it separately for efficiency,
	// i.e. to avoid creating new State objects at every step)
	protected State currentState;
	// List of currently available transitions
	protected TransitionList transitionList;
	// Has the transition list been built? 
	protected boolean transitionListBuilt;
	// State for which transition list applies
	// (if null, just the default - i.e. the last state in the current path)
	protected State transitionListState;
	// Temporary storage for manipulating states/rewards
	protected double tmpStateRewards[];
	protected double tmpTransitionRewards[];

	// Updater object for model
	protected Updater updater;
	// Random number generator
	private RandomNumberGenerator rng;

	// strategy information
	private Map<State, Integer> stateIds;

	// I know it is removed from the trunk, but it is important to keep the strategy up to date
	private Prism prism;

	// ------------------------------------------------------------------------------
	// Basic setup
	// ------------------------------------------------------------------------------

	/**
	 * Constructor for the simulator engine.
	 */
	public SimulatorEngine(PrismComponent parent, Prism prism)
	{
		super(parent);
		this.prism = prism;
		modulesFile = null;
		modelType = null;
		varList = null;
		numVars = 0;
		mfConstants = null;
		labels = null;
		properties = null;
		propertySamplers = null;
		path = null;
		onTheFly = true;
		currentState = null;
		transitionList = null;
		transitionListBuilt = false;
		transitionListState = null;
		tmpStateRewards = null;
		tmpTransitionRewards = null;
		updater = null;
		rng = new RandomNumberGenerator();
		strategy = null;
	}

	// ------------------------------------------------------------------------------
	// Path creation and modification
	// ------------------------------------------------------------------------------

	/**
	 * Check whether a model is suitable for exploration/analysis using the simulator.
	 * If not, an explanatory error message is thrown as an exception.
	 */
	public void checkModelForSimulation(ModulesFile modulesFile) throws PrismException
	{
		// No support for PTAs yet
		if (modulesFile.getModelType() == ModelType.PTA) {
			throw new PrismException("Sorry - the simulator does not currently support PTAs");
		}
		// No support for system...endsystem yet
		if (modulesFile.getSystemDefn() != null) {
			throw new PrismException("Sorry - the simulator does not currently handle the system...endsystem construct");
		}
	}

	/**
	 * Create a new path for a model.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile Model for simulation
	 */
	public void createNewPath(ModulesFile modulesFile) throws PrismException
	{
		// Store model
		loadModulesFile(modulesFile);
		// Create empty (full) path object associated with this model
		path = new PathFull(modulesFile);
		onTheFly = false;
	}

	/**
	 * Create a new on-the-fly path for a model.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile Model for simulation
	 */
	public void createNewOnTheFlyPath(ModulesFile modulesFile) throws PrismException
	{
		// Store model
		loadModulesFile(modulesFile);
		// Create empty (on-the-fly_ path object associated with this model
		path = new PathOnTheFly(modulesFile);
		onTheFly = true;
	}

	/**
	 * Initialise (or re-initialise) the simulation path, starting with a specific (or random) initial state.
	 * @param initialState Initial state (if null, use default, selecting randomly if needed)
	 */
	public void initialisePath(State initialState) throws PrismException
	{
		// Store passed in state as current state
		if (initialState != null) {
			currentState.copy(initialState);
		}
		// Or pick default/random one
		else {
			if (modulesFile.getInitialStates() == null) {
				currentState.copy(modulesFile.getDefaultInitialState());
			} else {
				throw new PrismException("Random choice of multiple initial states not yet supported");
			}
		}
		updater.calculateStateRewards(currentState, tmpStateRewards);
		// Initialise stored path
		path.initialise(currentState, tmpStateRewards);
		this.setStrategy(prism.getStrategy());

		// Reset transition list
		transitionListBuilt = false;
		transitionListState = null;
		// Reset and then update samplers for any loaded properties
		resetSamplers();
		updateSamplers();
		// Initialise the strategy (if loaded)
		initialiseStrategy();
	}

	/**
	 * Execute a transition from the current transition list, specified by its index
	 * within the (whole) list. If this is a continuous-time model, the time to be spent
	 * in the state before leaving is picked randomly.
	 */
	public void manualTransition(int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		int i = transitions.getChoiceIndexOfTransition(index);
		int offset = transitions.getChoiceOffsetOfTransition(index);
		if (modelType.continuousTime()) {
			double r = transitions.getProbabilitySum();
			executeTimedTransition(i, offset, rng.randomExpDouble(r), index);
		} else {
			executeTransition(i, offset, index);
		}
	}

	/**
	 * Execute a transition from the current transition list, specified by its index
	 * within the (whole) list. In addition, specify the amount of time to be spent in
	 * the current state before this transition occurs.
	 * [continuous-time models only]
	 */
	public void manualTransition(int index, double time) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		int i = transitions.getChoiceIndexOfTransition(index);
		int offset = transitions.getChoiceOffsetOfTransition(index);
		executeTimedTransition(i, offset, time, index);
	}

	/**
	 * Select, at random, a transition from the current transition list and execute it.
	 * For continuous-time models, the time to be spent in the state before leaving is also picked randomly.
	 * If there is currently a deadlock, no transition is taken and the function returns false.
	 * Otherwise, the function returns true indicating that a transition was successfully taken. 
	 */
	public boolean automaticTransition() throws PrismException
	{
		Choice choice;
		int numChoices, i = -1, j;
		double d, r;

		TransitionList transitions = getTransitionList();
		// Check for deadlock; if so, stop and return false
		numChoices = transitions.getNumChoices();
		if (numChoices == 0)
			return false;
		//throw new PrismException("Deadlock found at state " + path.getCurrentState().toString(modulesFile));

		switch (modelType) {
		case DTMC:
			i = (int) (Math.random() * numChoices);
			choice = transitions.getChoice(i);
			// Pick a random transition from this choice
			d = Math.random();
			j = choice.getIndexByProbabilitySum(d);
			// Execute
			executeTransition(i, j, -1);
			break;
		case MDP:
			// Pick a random choice
			//Note that we also check this in the DTMC-case, because maybe someone wants to simulate
			// the product of an MDP and a strategy (which is a DTMC)
			if (this.strategy == null) {
				i = (int) (Math.random() * numChoices);
			} else {
				Distribution strategyChoice;
				try {
					strategyChoice = this.strategy.getNextMove(stateIds.get(path.getCurrentState()));
				} catch (InvalidStrategyStateException e) {
					throw new RuntimeException(e);
				}
				double random = Math.random();
				for (int k : strategyChoice.getSupport()) {
					if (random < strategyChoice.get(k)) {
						i = k;
						break;
					}
					random = random - strategyChoice.get(k);
				}
			}
			choice = transitions.getChoice(i);
			// Pick a random transition from this choice
			d = Math.random();
			j = choice.getIndexByProbabilitySum(d);
			// Execute
			executeTransition(i, j, -1);
			break;
		case CTMC:
			// Get sum of all rates
			r = transitions.getProbabilitySum();
			// Pick a random number to determine choice/transition
			d = Math.random() * r;
			TransitionList.Ref ref = transitions.new Ref();
			transitions.getChoiceIndexByProbabilitySum(d, ref);
			// Execute
			executeTimedTransition(ref.i, ref.offset, (-Math.log(Math.random())) / r, -1);
			break;
		default:
			throw new AssertionError();
		}

		return true;
	}

	/**
	 * Select, at random, n successive transitions and execute them.
	 * For continuous-time models, the time to be spent in each state before leaving is also picked randomly.
	 * If a deadlock is found, the process stops.
	 * Optionally, if a deterministic loop is detected, the process stops.
	 * The function returns the number of transitions successfully taken. 
	 */
	public int automaticTransitions(int n, boolean stopOnLoops) throws PrismException
	{
		int i = 0;
		while (i < n && !(stopOnLoops && path.isLooping())) {
			if (!automaticTransition())
				break;
			i++;
		}
		return i;
	}

	/**
	 * Randomly select and execute transitions until the specified time delay is first exceeded.
	 * (Time is measured from the initial execution of this method, not total time.)
	 * (For discrete-time models, this just results in ceil(time) steps being executed.)
	 * If a deadlock is found, the process stops.
	 * The function returns the number of transitions successfully taken. 
	 * @throws PrismException
	 */
	public int automaticTransitions(double time, boolean stopOnLoops) throws PrismException
	{
		if (modelType.continuousTime()) {
			int i = 0;
			double targetTime = path.getTotalTime() + time;
			while (path.getTotalTime() < targetTime) {
				if (automaticTransition())
					i++;
				else
					break;
			}
			return i;
		} else {
			return automaticTransitions((int) Math.ceil(time), false);
		}
	}

	/**
	 * Backtrack to a particular step within the current path.
	 * Time point should be >=0 and <= the total path size. 
	 * (Not applicable for on-the-fly paths)
	 * @param step The step to backtrack to.
	 */
	public void backtrackTo(int step) throws PrismException
	{
		// Check step is valid
		if (step < 0) {
			throw new PrismException("Cannot backtrack to a negative step index");
		}
		if (step > path.size()) {
			throw new PrismException("There is no step " + step + " to backtrack to");
		}
		// Back track in path
		((PathFull) path).backtrack(step);
		// Update current state
		currentState.copy(path.getCurrentState());
		// Reset transition list 
		transitionListBuilt = false;
		transitionListState = null;
		// Recompute samplers for any loaded properties
		recomputeSamplers();
	}

	/**
	 * Backtrack to a particular (continuous) time point within the current path.
	 * Time point should be >=0 and <= the total path time. 
	 * (Not applicable for on-the-fly paths)
	 * @param time The amount of time to backtrack.
	 */
	public void backtrackTo(double time) throws PrismException
	{
		// Check step is valid
		if (time < 0) {
			throw new PrismException("Cannot backtrack to a negative time point");
		}
		if (time > path.getTotalTime()) {
			throw new PrismException("There is no time point " + time + " to backtrack to");
		}
		PathFull pathFull = (PathFull) path;
		// Get length (non-on-the-fly paths will never exceed length Integer.MAX_VALUE) 
		long nLong = path.size();
		if (nLong > Integer.MAX_VALUE)
			throw new PrismException("PathFull cannot deal with paths over length " + Integer.MAX_VALUE);
		int n = (int) nLong;
		// Find the index of the step we are in at that point
		// i.e. the first state whose cumulative time on entering exceeds 'time'
		int step;
		for (step = 0; step <= n && pathFull.getCumulativeTime(step) < time; step++)
			;
		// Then backtrack to this step
		backtrackTo(step);
	}

	/**
	 * Remove the prefix of the current path up to the given path step.
	 * Index step should be >=0 and <= the total path size. 
	 * (Not applicable for on-the-fly paths)
	 * @param step The step before which state will be removed.
	 */
	public void removePrecedingStates(int step) throws PrismException
	{
		// Check step is valid
		if (step < 0) {
			throw new PrismException("Cannot remove states before a negative step index");
		}
		if (step > path.size()) {
			throw new PrismException("There is no step " + step + " in the path");
		}
		// Modify path
		((PathFull) path).removePrecedingStates(step);
		// (No need to update currentState or re-generate transitions) 
		// Recompute samplers for any loaded properties
		recomputeSamplers();
	}

	/**
	 * Compute the transition table for an earlier step in the path.
	 * (Not applicable for on-the-fly paths)
	 * If you do this mid-path, don't forget to reset the transition table
	 * with <code>computeTransitionsForCurrentState</code> because this
	 * is not kept track of by the simulator.
	 */
	public void computeTransitionsForStep(int step) throws PrismException
	{
		updater.calculateTransitions(((PathFull) path).getState(step), transitionList);
		transitionListBuilt = true;
		transitionListState = new State(((PathFull) path).getState(step));
	}

	/**
	 * Re-compute the transition table for the current state.
	 */
	public void computeTransitionsForCurrentState() throws PrismException
	{
		updater.calculateTransitions(path.getCurrentState(), transitionList);
		transitionListBuilt = true;
		transitionListState = null;
	}

	/**
	 * Construct a path through a model to match a supplied path,
	 * specified as a PathFullInfo object.
	 * Note: All constants in the model must have already been defined.
	 * @param modulesFile Model for simulation
	 * @param newPath Path to match
	 */
	public void loadPath(ModulesFile modulesFile, PathFullInfo newPath) throws PrismException
	{
		int i, j, numTrans;
		long numSteps;
		boolean found;
		State state, nextState;
		createNewPath(modulesFile);
		numSteps = newPath.size();
		state = newPath.getState(0);
		initialisePath(state);
		for (i = 0; i < numSteps; i++) {
			nextState = newPath.getState(i + 1);
			// Find matching transition
			// (just look at states for now)
			TransitionList transitions = getTransitionList();
			numTrans = transitions.getNumTransitions();
			found = false;
			for (j = 0; j < numTrans; j++) {
				if (transitions.computeTransitionTarget(j, state).equals(nextState)) {
					found = true;
					if (modelType.continuousTime() && newPath.hasTimeInfo())
						manualTransition(j, newPath.getTime(i));
					else
						manualTransition(j);
					break;
				}
			}
			if (!found)
				throw new PrismException("Path loading failed at step " + (i + 1));
			state = nextState;
		}
	}

	// ------------------------------------------------------------------------------
	// Methods for adding/querying labels and properties
	// ------------------------------------------------------------------------------

	/**
	 * Add a label to the simulator, whose value will be computed during path generation.
	 * The resulting index of the label is returned: this is used for later queries about the property.
	 * Any constants/formulas etc. appearing in the label must have been defined in the current model.
	 * If there are additional constants (e.g. from a properties file),
	 * then use the {@link #addLabel(Expression, PropertiesFile)} method. 
	 */
	public int addLabel(Expression label) throws PrismLangException
	{
		return addLabel(label, null);
	}

	/**
	 * Add a label to the simulator, whose value will be computed during path generation.
	 * The resulting index of the label is returned: this is used for later queries about the property.
	 * Any constants/formulas etc. appearing in the label must have been defined in the current model
	 * or be supplied in the (optional) passed in PropertiesFile.
	 */
	public int addLabel(Expression label, PropertiesFile pf) throws PrismLangException
	{
		// Take a copy, get rid of any constants and simplify
		Expression labelNew = label.deepCopy();
		labelNew = (Expression) labelNew.replaceConstants(mfConstants);
		if (pf != null) {
			labelNew = (Expression) labelNew.replaceConstants(pf.getConstantValues());
		}
		labelNew = (Expression) labelNew.simplify();
		// Add to list and return index
		labels.add(labelNew);
		return labels.size() - 1;
	}

	/**
	 * Add a (path) property to the simulator, whose value will be computed during path generation.
	 * The resulting index of the property is returned: this is used for later queries about the property.
	 * Any constants/formulas etc. appearing in the label must have been defined in the current model.
	 * If there are additional constants (e.g. from a properties file),
	 * then use the {@link #addProperty(Expression, PropertiesFile)} method. 
	 */
	public int addProperty(Expression prop) throws PrismException
	{
		return addProperty(prop, null);
	}

	/**
	 * Add a (path) property to the simulator, whose value will be computed during path generation.
	 * The resulting index of the property is returned: this is used for later queries about the property.
	 * Any constants/formulas etc. appearing in the property must have been defined in the current model
	 * or be supplied in the (optional) passed in PropertiesFile.
	 * In case of error, the property is not added an exception is thrown.
	 */
	public int addProperty(Expression prop, PropertiesFile pf) throws PrismException
	{
		// Take a copy
		Expression propNew = prop.deepCopy();
		// Combine label lists from model/property file, then expand property refs/labels in property 
		LabelList combinedLabelList = (pf == null) ? modulesFile.getLabelList() : pf.getCombinedLabelList();
		propNew = (Expression) propNew.expandPropRefsAndLabels(pf, combinedLabelList);
		// Then get rid of any constants and simplify
		propNew = (Expression) propNew.replaceConstants(mfConstants);
		if (pf != null) {
			propNew = (Expression) propNew.replaceConstants(pf.getConstantValues());
		}
		propNew = (Expression) propNew.simplify();
		// Create sampler
		Sampler sampler = Sampler.createSampler(propNew, modulesFile);
		// Update lists and return index
		// (do this right at the end so that lists only get updated if there are no errors)
		properties.add(propNew);
		propertySamplers.add(sampler);
		return properties.size() - 1;
	}

	/**
	 * Get the current value of a previously added label (specified by its index).
	 */
	public boolean queryLabel(int index) throws PrismLangException
	{
		return labels.get(index).evaluateBoolean(path.getCurrentState());
	}

	/**
	 * Get the value, at the specified path step, of a previously added label (specified by its index).
	 * (Not applicable for on-the-fly paths)
	 * @param step The index of the step to check for
	 */
	public boolean queryLabel(int index, int step) throws PrismLangException
	{
		return labels.get(index).evaluateBoolean(((PathFull) path).getState(step));
	}

	/**
	 * Check whether the current state is an initial state.
	 */
	public boolean queryIsInitial() throws PrismLangException
	{
		// Currently init...endinit is not supported so this is easy to check
		return path.getCurrentState().equals(modulesFile.getDefaultInitialState());
	}

	/**
	 * Check whether a particular step in the current path is an initial state.
	 * @param step The index of the step to check for
	 * (Not applicable for on-the-fly paths)
	 */
	public boolean queryIsInitial(int step) throws PrismLangException
	{
		// Currently init...endinit is not supported so this is easy to check
		return ((PathFull) path).getState(step).equals(modulesFile.getDefaultInitialState());
	}

	/**
	 * Check whether the current state is a deadlock.
	 */
	public boolean queryIsDeadlock() throws PrismException
	{
		return getTransitionList().isDeadlock();
	}

	/**
	 * Check whether a particular step in the current path is a deadlock.
	 * (Not applicable for on-the-fly paths)
	 * @param step The index of the step to check for
	 */
	public boolean queryIsDeadlock(int step) throws PrismException
	{
		// By definition, earlier states in the path cannot be deadlocks
		return step == path.size() ? getTransitionList().isDeadlock() : false;
	}

	/**
	 * Check the value for a particular property in the current path.
	 * If the value is not known yet, the result will be null.
	 * @param index The index of the property to check
	 */
	public Object queryProperty(int index)
	{
		if (index < 0 || index >= propertySamplers.size()) {
			mainLog.printWarning("Can't query property " + index + ".");
			return null;
		}
		Sampler sampler = propertySamplers.get(index);
		return sampler.isCurrentValueKnown() ? sampler.getCurrentValue() : null;
	}

	// ------------------------------------------------------------------------------
	// Private methods for path creation and modification
	// ------------------------------------------------------------------------------

	/**
	 * Loads a new PRISM model into the simulator.
	 * @param modulesFile The parsed PRISM model
	 */
	private void loadModulesFile(ModulesFile modulesFile) throws PrismException
	{
		// Store model, some info and constants
		this.modulesFile = modulesFile;
		modelType = modulesFile.getModelType();
		this.mfConstants = modulesFile.getConstantValues();

		// Check model is simulate-able
		checkModelForSimulation(modulesFile);

		// Get variable list (symbol table) for model 
		varList = modulesFile.createVarList();
		numVars = varList.getNumVars();

		// Evaluate constants and optimise (a copy of) modules file for simulation
		modulesFile = (ModulesFile) modulesFile.deepCopy().replaceConstants(mfConstants).simplify();

		// Create state/transition/rewards storage
		currentState = new State(numVars);
		tmpStateRewards = new double[modulesFile.getNumRewardStructs()];
		tmpTransitionRewards = new double[modulesFile.getNumRewardStructs()];
		transitionList = new TransitionList();

		// Create updater for model
		updater = new Updater(modulesFile, varList, this);

		// Clear storage for strategy
		strategy = null;
		stateIds = null;

		// Create storage for labels/properties
		labels = new ArrayList<>();
		properties = new ArrayList<>();
		propertySamplers = new ArrayList<>();
	}

	/**
	 * Execute a transition from the current transition list and update path (if being stored).
	 * Transition is specified by index of its choice and offset within it. If known, its index
	 * (within the whole list) can optionally be specified (since this may be needed for storage
	 * in the path). If this is -1, it will be computed automatically if needed.
	 * @param i Index of choice containing transition to execute
	 * @param offset Index within choice of transition to execute
	 * @param index (Optionally) index of transition within whole list (-1 if unknown)
	 */
	private void executeTransition(int i, int offset, int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		// Get corresponding choice and, if required (for full paths), calculate transition index
		Choice choice = transitions.getChoice(i);
		if (!onTheFly && index == -1)
			index = transitions.getTotalIndexOfTransition(i, offset);
		// Compute probability for transition (is normalised for a DTMC)
		double p = (choice.getProbability(offset));

		if (modelType == ModelType.DTMC) {
			p = p / transitions.getNumChoices();
		}
		// Compute its transition rewards
		updater.calculateTransitionRewards(path.getCurrentState(), choice, tmpTransitionRewards);
		// Compute next state. Note use of path.getCurrentState() because currentState
		// will be overwritten during the call to computeTarget().
		choice.computeTarget(offset, path.getCurrentState(), currentState);
		// Compute state rewards for new state 
		updater.calculateStateRewards(currentState, tmpStateRewards);
		// Update path
		if (strategy != null && path instanceof PathFull) {
			// update strategy
			try {
				strategy.updateMemory(i, stateIds.get(currentState));
			} catch (InvalidStrategyStateException error) {
				//error.printStackTrace();
				throw new PrismException("Strategy update failed");
			}
			((PathFull) path).addStep(index, choice.getModuleOrActionIndex(), p, tmpTransitionRewards, currentState, tmpStateRewards, transitions,
					strategy.getCurrentMemoryElement());
		} else
			path.addStep(index, choice.getModuleOrActionIndex(), p, tmpTransitionRewards, currentState, tmpStateRewards, transitions);
		// Reset transition list 
		transitionListBuilt = false;
		transitionListState = null;
		// Update samplers for any loaded properties
		updateSamplers();
	}

	/**
	 * Execute a (timed) transition from the current transition list and update path (if being stored).
	 * Transition is specified by index of its choice and offset within it. If known, its index
	 * (within the whole list) can optionally be specified (since this may be needed for storage
	 * in the path). If this is -1, it will be computed automatically if needed.
	 * In addition, the amount of time to be spent in the current state before this transition occurs should be specified.
	 * [continuous-time models only]
	 * @param i Index of choice containing transition to execute
	 * @param offset Index within choice of transition to execute
	 * @param time Time for transition
	 * @param index (Optionally) index of transition within whole list (-1 if unknown)
	 */
	private void executeTimedTransition(int i, int offset, double time, int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		// Get corresponding choice and, if required (for full paths), calculate transition index
		Choice choice = transitions.getChoice(i);
		if (!onTheFly && index == -1)
			index = transitions.getTotalIndexOfTransition(i, offset);
		// Get probability for transition (no need to normalise because DTMC transitions are never timed)
		double p = choice.getProbability(offset);
		// Compute its transition rewards
		updater.calculateTransitionRewards(path.getCurrentState(), choice, tmpTransitionRewards);
		// Compute next state. Note use of path.getCurrentState() because currentState
		// will be overwritten during the call to computeTarget().
		choice.computeTarget(offset, path.getCurrentState(), currentState);
		// Compute state rewards for new state 
		updater.calculateStateRewards(currentState, tmpStateRewards);
		// Update path
		if (strategy != null && path instanceof PathFull) {
			// update strategy
			try {
				strategy.updateMemory(i, stateIds.get(currentState));
			} catch (InvalidStrategyStateException error) {
				//error.printStackTrace();
				throw new PrismException("Strategy update failed");
			}
			((PathFull) path).addStep(time, index, choice.getModuleOrActionIndex(), p, tmpTransitionRewards, currentState, tmpStateRewards, transitions,
					strategy.getCurrentMemoryElement());
		} else
			path.addStep(time, index, choice.getModuleOrActionIndex(), p, tmpTransitionRewards, currentState, tmpStateRewards, transitions);
		// Reset transition list 
		transitionListBuilt = false;
		transitionListState = null;
		// Update samplers for any loaded properties
		updateSamplers();
	}

	/**
	 * Reset samplers for any loaded properties.
	 */
	private void resetSamplers()
	{
		for (Sampler sampler : propertySamplers) {
			sampler.reset();
		}
	}

	/**
	 * Notify samplers for any loaded properties that a new step has occurred.
	 */
	private void updateSamplers() throws PrismException
	{
		for (Sampler sampler : propertySamplers) {
			sampler.update(path, getTransitionList());
		}
	}

	/**
	 * Recompute the state of samplers for any loaded properties based on the whole current path.
	 * (Not applicable for on-the-fly paths)
	 */
	private void recomputeSamplers() throws PrismLangException
	{
		resetSamplers();
		// Get length (non-on-the-fly paths will never exceed length Integer.MAX_VALUE) 
		long nLong = path.size();
		if (nLong > Integer.MAX_VALUE)
			throw new PrismLangException("PathFull cannot deal with paths over length " + Integer.MAX_VALUE);
		int n = (int) nLong;
		// Loop
		PathFullPrefix prefix = new PathFullPrefix((PathFull) path, 0);
		for (int i = 0; i <= n; i++) {
			prefix.setPrefixLength(i);
			for (Sampler sampler : propertySamplers) {
				sampler.update(prefix, null);
				// TODO: fix this optimisation 
			}
		}
	}

	/**
	 * Initialise the state of the loaded strategy, if present, based on the current state.
	 */
	private void initialiseStrategy()
	{
		if (strategy != null) {
			State state = getCurrentState();
			strategy.initialise(stateIds.get(state));
		}
	}

	// ------------------------------------------------------------------------------
	// Queries regarding model
	// ------------------------------------------------------------------------------

	/**
	 * Returns the number of variables in the current model.
	 */
	public int getNumVariables()
	{
		return numVars;
	}

	/**
	 * Returns the name of the ith variable in the current model.
	 * (Returns null if index i is out of range.)
	 */
	public String getVariableName(int i)
	{
		return (i < numVars && i >= 0) ? varList.getName(i) : null;
	}

	/**
	 * Returns the type of the ith variable in the current model.
	 * (Returns null if index i is out of range.)
	 */
	public Type getVariableType(int i)
	{
		return (i < numVars && i >= 0) ? varList.getType(i) : null;
	}

	/**
	 * Returns the index of a variable name, as stored by the simulator for the current model.
	 * Returns -1 if the action name does not exist. 
	 */
	public int getIndexOfVar(String name)
	{
		return varList.getIndex(name);
	}

	// ------------------------------------------------------------------------------
	// Querying of current state and its available choices/transitions
	// ------------------------------------------------------------------------------

	/**
	 * Returns the current list of available transitions, generating it first if this has not yet been done.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public TransitionList getTransitionList() throws PrismException
	{
		// Compute the current transition list, if required
		if (!transitionListBuilt) {
			computeTransitionsForCurrentState();
		}
		return transitionList;
	}

	/**
	 * Get the state for which the simulator is currently supplying information about its transitions. 
	 * Usually, this is the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be this state instead.
	 */
	public State getTransitionListState()
	{
		return (transitionListState == null) ? path.getCurrentState() : transitionListState;

	}

	/**
	 * Returns the current number of available choices.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 * Returns the current number of available choices.
	 */
	public int getNumChoices() throws PrismException
	{
		return getTransitionList().getNumChoices();
	}

	/**
	 * Returns the current (total) number of available transitions.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public int getNumTransitions() throws PrismException
	{
		return getTransitionList().getNumTransitions();
	}

	/**
	 * Returns the current number of available transitions in choice i.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public int getNumTransitions(int i) throws PrismException
	{
		return getTransitionList().getChoice(i).size();
	}

	/**
	 * Get the index of the choice containing a transition of a given index.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public int getChoiceIndexOfTransition(int index) throws PrismException
	{
		return getTransitionList().getChoiceIndexOfTransition(index);
	}

	/**
	 * Get a string describing the action/module of a transition, specified by its index/offset.
	 * (form is "module" or "[action]")
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public String getTransitionModuleOrAction(int i, int offset) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		return transitions.getTransitionModuleOrAction(transitions.getTotalIndexOfTransition(i, offset));
	}

	/**
	 * Get a string describing the action/module of a transition, specified by its index.
	 * (form is "module" or "[action]")
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public String getTransitionModuleOrAction(int index) throws PrismException
	{
		return getTransitionList().getTransitionModuleOrAction(index);
	}

	/**
	 * Get the probability/rate of a transition, specified by its index.
	 * Usually, this is for the current (final) state of the path but, if you called {@link #computeTransitionsForStep(int step)}, it will be for this state instead.
	 */
	public double getTransitionProbability(int index) throws PrismException
	{
		TransitionList transitions = getTransitionList();
		double p = transitions.getTransitionProbability(index);
		// For DTMCs, we need to normalise (over choices)
		return (modelType == ModelType.DTMC ? p / transitions.getNumChoices() : p);
	}

	/**
	 * Get a string describing the updates making up a transition, specified by its index.
	 * This is in abbreviated form, i.e. x'=1, rather than x'=x+1.
	 * Format is: x'=1, y'=0, with empty string for empty update.
	 * Only variables updated are included in list (even if unchanged).
	 */
	public String getTransitionUpdateString(int index) throws PrismException
	{
		return getTransitionList().getTransitionUpdateString(index, getTransitionListState());
	}

	/**
	 * Get the size of the current path (number of steps; or number of states - 1).
	 */
	public long getPathSize()
	{
		return path.size();
	}

	/**
	 * Returns the current state being explored by the simulator.
	 */
	public State getCurrentState()
	{
		return path.getCurrentState();
	}

	/**
	 * Get the total time elapsed so far (where zero time has been spent in the current (final) state).
	 * For discrete-time models, this is just the number of steps (but returned as a double).
	 */
	public double getTotalTimeForPath()
	{
		return path.getTotalTime();
	}

	// ------------------------------------------------------------------------------
	// Querying of current path (full or on-the-fly)
	// ------------------------------------------------------------------------------

	/**
	 * Get access to the {@code Path} object storing the current path.
	 * This object is only valid until the next time {@link #createNewPath} is called. 
	 */
	public Path getPath()
	{
		return path;
	}

	/**
	 * Get the total reward accumulated so far
	 * (includes reward for previous transition but no state reward for current (final) state).
	 * @param rsi Reward structure index
	 */
	public double getTotalCumulativeRewardForPath(int rsi)
	{
		return path.getTotalCumulativeReward(rsi);
	}

	// ------------------------------------------------------------------------------
	// Querying of current path (full paths only)
	// ------------------------------------------------------------------------------

	/**
	 * Get access to the {@code PathFull} object storing the current path.
	 * (Not applicable for on-the-fly paths)
	 * This object is only valid until the next time {@link #createNewPath} is called. 
	 */
	public PathFull getPathFull()
	{
		return (PathFull) path;
	}

	/**
	 * Get the state at a given step of the path.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public State getStateOfPathStep(int step)
	{
		return ((PathFull) path).getState(step);
	}

	/**
	 * Get the index of the choice taken for a given step.
	 * (Not applicable for on-the-fly paths)
	 * @param step Step index (0 = initial state/step of path)
	 */
	public int getChoiceOfPathStep(int step)
	{
		return ((PathFull) path).getChoice(step);
	}

	/**
	 * Check whether the current path is in a deterministic loop.
	 */
	public boolean isPathLooping()
	{
		return path.isLooping();
	}

	/**
	 * Export the current path to a file in a simple space separated format.
	 * (Not applicable for on-the-fly paths)
	 * @param file File to which the path should be exported to (mainLog if null).
	 */
	public void exportPath(File file) throws PrismException
	{
		exportPath(file, false, " ", null);
	}

	/**
	 * Export the current path to a file.
	 * (Not applicable for on-the-fly paths)
	 * @param file File to which the path should be exported to (mainLog if null).
	 * @param timeCumul Show time in cumulative form?
	 * @param colSep String used to separate columns in display
	 * @param vars Restrict printing to these variables (indices) and steps which change them (ignore if null)
	 */
	public void exportPath(File file, boolean timeCumul, String colSep, ArrayList<Integer> vars) throws PrismException
	{
		PrismLog log;
		if (path == null)
			throw new PrismException("There is no path to export");
		// create new file log or use main log
		if (file != null) {
			log = new PrismFileLog(file.getPath());
			if (!log.ready()) {
				log.close();
				throw new PrismException("Could not open file \"" + file + "\" for output");
			}
			log.close();
			mainLog.println("\nExporting path to file \"" + file + "\"...");
		} else {
			log = mainLog;
			log.println();
		}
		((PathFull) path).exportToLog(log, timeCumul, colSep, vars);
	}

	/**
	 * Plot the current path on a Graph.
	 * @param graphModel Graph on which to plot path
	 */
	public void plotPath(Graph graphModel)
	{
		((PathFull) path).plotOnGraph(graphModel);
	}

	// ------------------------------------------------------------------------------
	// Model checking (approximate)
	// ------------------------------------------------------------------------------

	/**
	 * Check whether a property is suitable for approximate model checking using the simulator.
	 */
	public boolean isPropertyOKForSimulation(Expression expr)
	{
		return isPropertyOKForSimulationString(expr) == null;
	}

	/**
	 * Check whether a property is suitable for approximate model checking using the simulator.
	 * If not, an explanatory error message is thrown as an exception.
	 */
	public void checkPropertyForSimulation(Expression expr) throws PrismException
	{
		String errMsg = isPropertyOKForSimulationString(expr);
		if (errMsg != null)
			throw new PrismException(errMsg);
	}

	/**
	 * Check whether a property is suitable for approximate model checking using the simulator.
	 * If yes, return null; if not, return an explanatory error message.
	 */
	private String isPropertyOKForSimulationString(Expression expr)
	{
		// Simulator can only be applied to P or R properties (without filters)
		if (!(expr instanceof ExpressionProb || expr instanceof ExpressionReward)) {
			if (expr instanceof ExpressionFilter) {
				if (((ExpressionFilter) expr).getOperand() instanceof ExpressionProb || ((ExpressionFilter) expr).getOperand() instanceof ExpressionReward)
					return "Simulator cannot handle P or R properties with filters";
			}
			return "Simulator can only handle P or R properties";
		}
		// Check that there are no nested probabilistic operators
		try {
			if (expr.computeProbNesting() > 1) {
				return "Simulator cannot handle nested P, R or S operators";
			}
		} catch (PrismException e) {
			return "Simulator cannot handle this property: " + e.getMessage();
		}
		// Simulator cannot handle cumulative reward properties without a time bound
		if (expr instanceof ExpressionReward) {
			Expression exprTemp = ((ExpressionReward) expr).getExpression();
			if (exprTemp instanceof ExpressionTemporal) {
				if (((ExpressionTemporal) exprTemp).getOperator() == ExpressionTemporal.R_C) {
					if (((ExpressionTemporal) exprTemp).getUpperBound() == null) {
						return "Simulator cannot handle cumulative reward properties without time bounds";
					}
				}
			}
		}
		// No errors
		return null;
	}

	/**
	 * Perform approximate model checking of a property on a model, using the simulator.
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Returns a Result object, except in case of error, where an Exception is thrown.
	 * Note: All constants in the model/property files must have already been defined.
	 * @param modulesFile Model for simulation, constants defined
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param expr The property to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 * @throws InvalidStrategyStateException 
	 */
	public Object modelCheckSingleProperty(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression expr, State initialState, long maxPathLength,
			SimulationMethod simMethod) throws PrismException
	{
		ArrayList<Expression> exprs;
		Object res[];

		// Just do this via the 'multiple properties' method
		exprs = new ArrayList<>();
		exprs.add(expr);
		res = modelCheckMultipleProperties(modulesFile, propertiesFile, exprs, initialState, maxPathLength, simMethod);

		if (res[0] instanceof PrismException)
			throw (PrismException) res[0];
		else
			return res[0];
	}

	/**
	 * Perform approximate model checking of properties on a model, using the simulator.
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Returns an array of results, some of which may be Exception objects if there were errors.
	 * In the case of an error which affects all properties, an exception is thrown.
	 * Note: All constants in the model/property files must have already been defined.
	 * @param modulesFile Model for simulation, constants defined
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param exprs The properties to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 * @throws InvalidStrategyStateException 
	 */
	public Object[] modelCheckMultipleProperties(ModulesFile modulesFile, PropertiesFile propertiesFile, List<Expression> exprs, State initialState,
			long maxPathLength, SimulationMethod simMethod) throws PrismException
	{
		// Load model into simulator
		createNewOnTheFlyPath(modulesFile);

		// Make sure any missing parameters that can be computed before simulation
		// are computed now (sometimes this has been done already, e.g. for GUI display).
		simMethod.computeMissingParameterBeforeSim();

		// Print details to log
		mainLog.println("\nSimulation method: " + simMethod.getName() + " (" + simMethod.getFullName() + ")");
		mainLog.println("Simulation method parameters: " + simMethod.getParametersString());
		mainLog.println("Simulation parameters: max path length=" + maxPathLength);

		// Add the properties to the simulator (after a check that they are valid)
		Object[] results = new Object[exprs.size()];
		int[] indices = new int[exprs.size()];
		int validPropsCount = 0;
		for (int i = 0; i < exprs.size(); i++) {
			try {
				checkPropertyForSimulation(exprs.get(i));
				indices[i] = addProperty(exprs.get(i), propertiesFile);
				validPropsCount++;
				// Attach a SimulationMethod object to each property's sampler
				SimulationMethod simMethodNew = simMethod.clone();
				propertySamplers.get(indices[i]).setSimulationMethod(simMethodNew);
				// Pass property details to SimuationMethod
				// (note that we use the copy stored in properties, which has been processed)
				try {
					simMethodNew.setExpression(properties.get(indices[i]));
				} catch (PrismException e) {
					// In case of error, also need to remove property/sampler from list
					properties.remove(indices[i]);
					propertySamplers.remove(indices[i]);
					throw e;
				}
			} catch (PrismException e) {
				results[i] = e;
				indices[i] = -1;
			}
		}

		// As long as there are at least some valid props, do sampling
		if (validPropsCount > 0) {
			doSampling(initialState, maxPathLength);
		}

		// Process the results
		for (int i = 0; i < results.length; i++) {
			// If there was an earlier error, nothing to do
			if (indices[i] != -1) {
				Sampler sampler = propertySamplers.get(indices[i]);
				//mainLog.print("Simulation results: mean: " + sampler.getMeanValue());
				//mainLog.println(", variance: " + sampler.getVariance());
				SimulationMethod sm = sampler.getSimulationMethod();
				// Compute/print any missing parameters that need to be done after simulation
				sm.computeMissingParameterAfterSim();
				// Extract result from SimulationMethod and store
				try {
					results[i] = sm.getResult(sampler);
				} catch (PrismException e) {
					results[i] = e;
				}
			}
		}

		// Display results to log
		if (results.length == 1) {
			mainLog.print("\nSimulation method parameters: ");
			mainLog.println((indices[0] == -1) ? "no simulation" : propertySamplers.get(indices[0]).getSimulationMethod().getParametersString());
			mainLog.print("\nSimulation result details: ");
			mainLog.println((indices[0] == -1) ? "no simulation" : propertySamplers.get(indices[0]).getSimulationMethodResultExplanation());
			if (!(results[0] instanceof PrismException))
				mainLog.println("\nResult: " + results[0]);
		} else {
			mainLog.println("\nSimulation method parameters:");
			for (int i = 0; i < results.length; i++) {
				mainLog.print(exprs.get(i) + " : ");
				mainLog.println((indices[i] == -1) ? "no simulation" : propertySamplers.get(indices[i]).getSimulationMethod().getParametersString());
			}
			mainLog.println("\nSimulation result details:");
			for (int i = 0; i < results.length; i++) {
				mainLog.print(exprs.get(i) + " : ");
				mainLog.println((indices[i] == -1) ? "no simulation" : propertySamplers.get(indices[i]).getSimulationMethodResultExplanation());
			}
			mainLog.println("\nResults:");
			for (int i = 0; i < results.length; i++)
				mainLog.println(exprs.get(i) + " : " + results[i]);
		}

		return results;
	}

	/**
	 * Perform an approximate model checking experiment on a model, using the simulator
	 * (specified by values for undefined constants from the property only).
	 * Sampling starts from the initial state provided or, if null, the default
	 * initial state is used, selecting randomly (each time) if there are more than one.
	 * Results are stored in the ResultsCollection object passed in,
	 * some of which may be Exception objects if there were errors.
	 * In the case of an error which affects all properties, an exception is thrown.
	 * Note: All constants in the model file must have already been defined.
	 * @param modulesFile Model for simulation, constants defined
	 * @param propertiesFile Properties file containing property to check, constants defined
	 * @param undefinedConstants Details of constant ranges defining the experiment
	 * @param resultsCollection Where to store the results
	 * @param expr The property to check
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @param simMethod Object specifying details of method to use for simulation
	 * @throws PrismException if something goes wrong with the sampling algorithm
	 * @throws InterruptedException if the thread is interrupted
	 */
	public void modelCheckExperiment(ModulesFile modulesFile, PropertiesFile propertiesFile, UndefinedConstants undefinedConstants,
			ResultsCollection resultsCollection, Expression expr, State initialState, long maxPathLength, SimulationMethod simMethod)
			throws PrismException, InterruptedException
	{
		// Load model into simulator
		createNewOnTheFlyPath(modulesFile);

		// Make sure any missing parameters that can be computed before simulation
		// are computed now (sometimes this has been done already, e.g. for GUI display).
		simMethod.computeMissingParameterBeforeSim();

		// Print details to log
		mainLog.println("\nSimulation method: " + simMethod.getName() + " (" + simMethod.getFullName() + ")");
		mainLog.println("Simulation method parameters: " + simMethod.getParametersString());
		mainLog.println("Simulation parameters: max path length=" + maxPathLength);

		// Add the properties to the simulator (after a check that they are valid)
		int n = undefinedConstants.getNumPropertyIterations();
		Values definedPFConstants = new Values();
		Object[] results = new Object[n];
		Values[] pfcs = new Values[n];
		int[] indices = new int[n];

		int validPropsCount = 0;
		for (int i = 0; i < n; i++) {
			definedPFConstants = undefinedConstants.getPFConstantValues();
			pfcs[i] = definedPFConstants;
			propertiesFile.setSomeUndefinedConstants(definedPFConstants);
			try {
				checkPropertyForSimulation(expr);
				indices[i] = addProperty(expr, propertiesFile);
				validPropsCount++;
				// Attach a SimulationMethod object to each property's sampler
				SimulationMethod simMethodNew = simMethod.clone();
				propertySamplers.get(indices[i]).setSimulationMethod(simMethodNew);
				// Pass property details to SimuationMethod
				// (note that we use the copy stored in properties, which has been processed)
				try {
					simMethodNew.setExpression(properties.get(indices[i]));
				} catch (PrismException e) {
					// In case of error, also need to remove property/sampler from list
					// (NB: this will be at the end of the list so no re-indexing issues)
					properties.remove(indices[i]);
					propertySamplers.remove(indices[i]);
					throw e;
				}
			} catch (PrismException e) {
				results[i] = e;
				indices[i] = -1;
			}
			undefinedConstants.iterateProperty();
		}

		// As long as there are at least some valid props, do sampling
		if (validPropsCount > 0) {
			doSampling(initialState, maxPathLength);
		}

		// Process the results
		for (int i = 0; i < n; i++) {
			// If there was an earlier error, nothing to do
			if (indices[i] != -1) {
				Sampler sampler = propertySamplers.get(indices[i]);
				//mainLog.print("Simulation results: mean: " + sampler.getMeanValue());
				//mainLog.println(", variance: " + sampler.getVariance());
				SimulationMethod sm = sampler.getSimulationMethod();
				// Compute/print any missing parameters that need to be done after simulation
				sm.computeMissingParameterAfterSim();
				// Extract result from SimulationMethod and store
				try {
					results[i] = sm.getResult(sampler);
				} catch (PrismException e) {
					results[i] = e;
				}
			}
			// Store result in the ResultsCollection
			resultsCollection.setResult(undefinedConstants.getMFConstantValues(), pfcs[i], results[i]);
		}

		// Display results to log
		mainLog.println("\nSimulation method parameters:");
		for (int i = 0; i < results.length; i++) {
			mainLog.print(pfcs[i] + " : ");
			mainLog.println((indices[i] == -1) ? "no simulation" : propertySamplers.get(indices[i]).getSimulationMethod().getParametersString());
		}
		mainLog.println("\nSimulation result details:");
		for (int i = 0; i < results.length; i++) {
			mainLog.print(pfcs[i] + " : ");
			mainLog.println((indices[i] == -1) ? "no simulation" : propertySamplers.get(indices[i]).getSimulationMethodResultExplanation());
		}
		mainLog.println("\nResults:");
		mainLog.print(resultsCollection.toStringPartial(undefinedConstants.getMFConstantValues(), true, " ", " : ", false));
	}

	/**
	 * Execute sampling for the set of currently loaded properties.
	 * Sample paths are from the specified initial state and maximum length.
	 * Termination of the sampling process occurs when the SimulationMethod object
	 * for all properties indicate that it is finished.
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param maxPathLength The maximum path length for sampling
	 * @throws InvalidStrategyStateException 
	 */
	private void doSampling(State initialState, long maxPathLength) throws PrismException
	{
		int iters;
		long i;
		// Flags
		boolean stoppedEarly = false;
		boolean deadlocksFound = false;
		boolean allDone = false;
		boolean allKnown = false;
		boolean someUnknownButBounded = false;
		boolean shouldStopSampling = false;
		// Path stats
		double avgPathLength = 0;
		long minPathFound = 0, maxPathFound = 0;
		// Progress info
		int lastPercentageDone = 0;
		int percentageDone = 0;
		// Timing info
		long start, stop;
		double time_taken;

		// Start
		start = System.currentTimeMillis();
		mainLog.print("\nSampling progress: [");
		mainLog.flush();

		// Main sampling loop
		iters = 0;
		while (!shouldStopSampling) {

			// See if all properties are done; if so, stop sampling
			allDone = true;
			for (Sampler sampler : propertySamplers) {
				if (!sampler.getSimulationMethod().shouldStopNow(iters, sampler))
					allDone = false;
			}
			if (allDone)
				break;

			// Display progress (of slowest property)
			percentageDone = 100;
			for (Sampler sampler : propertySamplers) {
				percentageDone = Math.min(percentageDone, sampler.getSimulationMethod().getProgress(iters, sampler));
			}
			if (percentageDone > lastPercentageDone) {
				lastPercentageDone = percentageDone;
				mainLog.print(" " + lastPercentageDone + "%");
				mainLog.flush();
			}

			iters++;

			// Start the new path for this iteration (sample)
			initialisePath(initialState);

			// Generate a path
			allKnown = false;
			someUnknownButBounded = false;
			i = 0;
			while ((!allKnown && i < maxPathLength) || someUnknownButBounded) {
				// Check status of samplers
				allKnown = true;
				someUnknownButBounded = false;
				for (Sampler sampler : propertySamplers) {
					if (!sampler.isCurrentValueKnown()) {
						allKnown = false;
						if (sampler.needsBoundedNumSteps())
							someUnknownButBounded = true;
					}
				}
				// Stop when all answers are known or we have reached max path length
				// (but don't stop yet if there are "bounded" samplers with unkown values)
				if ((allKnown || i >= maxPathLength) && !someUnknownButBounded)
					break;
				// Make a random transition
				automaticTransition();
				i++;
			}

			// TODO: Detect deadlocks so we can report a warning

			// Update path length statistics
			avgPathLength = (avgPathLength * (iters - 1) + (i)) / iters;
			minPathFound = (iters == 1) ? i : Math.min(minPathFound, i);
			maxPathFound = (iters == 1) ? i : Math.max(maxPathFound, i);

			// If not all samplers could produce values, this an error
			if (!allKnown) {
				stoppedEarly = true;
				break;
			}

			// Update state of samplers based on last path
			for (Sampler sampler : propertySamplers) {
				sampler.updateStats();
			}
		}

		// Print details
		if (!stoppedEarly) {
			if (!shouldStopSampling)
				mainLog.print(" 100% ]");
			mainLog.println();
			stop = System.currentTimeMillis();
			time_taken = (stop - start) / 1000.0;
			mainLog.print("\nSampling complete: ");
			mainLog.print(iters + " iterations in " + time_taken + " seconds (average " + PrismUtils.formatDouble(2, time_taken / iters) + ")\n");
			mainLog.print(
					"Path length statistics: average " + PrismUtils.formatDouble(2, avgPathLength) + ", min " + minPathFound + ", max " + maxPathFound + "\n");
		} else {
			mainLog.print(" ...\n\nSampling terminated early after " + iters + " iterations.\n");
		}

		// Print a warning if deadlocks occurred at any point
		if (deadlocksFound)
			mainLog.printWarning("Deadlocks were found during simulation: self-loops were added.");

		// Print a warning if simulation was stopped by the user
		if (shouldStopSampling)
			mainLog.printWarning("Simulation was terminated before completion.");

		// write to feedback file with true to indicate that we have finished sampling
		// Write_Feedback(iteration_counter, numIters, true);

		if (stoppedEarly) {
			throw new PrismException(
					"One or more of the properties being sampled could not be checked on a sample. Consider increasing the maximum path length");
		}
	}

	/**
	 * Halt the sampling algorithm in its tracks (not implemented).
	 */
	public void stopSampling()
	{
		// TODO
	}

	/**
	 * Update strategy reference
	 *
	 * @param strategy
	 */
	public void setStrategy(Strategy strategy)
	{
		this.strategy = strategy;
		if (strategy != null) {
			this.stateIds = new HashMap<>();
			List<State> stateslist;
			if (prism.getBuiltModelExplicit() != null) {
				stateslist = prism.getBuiltModelExplicit().getStatesList();
			} else {
				throw new UnsupportedOperationException("Cannot generate a strategy if there is no explicit model");
			}
			int i = 0;
			for (State s : stateslist) {
				this.stateIds.put(s, i++);
			}
		}
	}
}
